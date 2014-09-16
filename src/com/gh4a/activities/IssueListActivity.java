/*
 * Copyright 2011 Azwan Adli Abdullah
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gh4a.activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.Loader;
import android.text.TextUtils;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.LoadingFragmentPagerActivity;
import com.gh4a.R;
import com.gh4a.fragment.IssueListFragment;
import com.gh4a.loader.CollaboratorListLoader;
import com.gh4a.loader.IsCollaboratorLoader;
import com.gh4a.loader.LabelListLoader;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.loader.MilestoneListLoader;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.UiUtils;

import org.eclipse.egit.github.core.Label;
import org.eclipse.egit.github.core.Milestone;
import org.eclipse.egit.github.core.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IssueListActivity extends LoadingFragmentPagerActivity {
    private String mRepoOwner;
    private String mRepoName;

    private String mSortMode;
    private boolean mSortAscending;
    private List<String> mSelectedLabels;
    private int mSelectedMilestone;
    private String mSelectedAssignee;

    private int mPendingSelectedItem;

    private IssueListFragment mOpenFragment;
    private IssueListFragment mClosedFragment;
    private ActionBar mActionBar;
    private boolean mIsCollaborator;
    private ProgressDialog mProgressDialog;
    private List<Label> mLabels;
    private List<Milestone> mMilestones;
    private List<User> mAssignees;

    private static final int[] TITLES = new int[] {
        R.string.open, R.string.closed
    };
    // The order of those two must match the menu item order!
    private static final String[] SORTMODES = new String[] {
        "updated", "created", "comments"
    };
    private static final int[] SORTMODE_MENU_IDS = new int[] {
        R.id.sort_by_updated, R.id.sort_by_created, R.id.sort_by_comments
    };

    private LoaderCallbacks<List<Label>> mLabelCallback = new LoaderCallbacks<List<Label>>() {
        @Override
        public Loader<LoaderResult<List<Label>>> onCreateLoader(int id, Bundle args) {
            return new LabelListLoader(IssueListActivity.this, mRepoOwner, mRepoName);
        }

        @Override
        public void onResultReady(LoaderResult<List<Label>> result) {
            if (checkForError(result)) {
                return;
            }
            stopProgressDialog(mProgressDialog);
            mLabels = result.getData();
            showLabelsDialog();
            getSupportLoaderManager().destroyLoader(0);
        }
    };

    private LoaderCallbacks<List<Milestone>> mMilestoneCallback =
            new LoaderCallbacks<List<Milestone>>() {
        @Override
        public Loader<LoaderResult<List<Milestone>>> onCreateLoader(int id, Bundle args) {
            return new MilestoneListLoader(IssueListActivity.this, mRepoOwner, mRepoName,
                    Constants.Issue.STATE_OPEN);
        }

        @Override
        public void onResultReady(LoaderResult<List<Milestone>> result) {
            if (checkForError(result)) {
                return;
            }
            stopProgressDialog(mProgressDialog);
            mMilestones = result.getData();
            showMilestonesDialog();
            getSupportLoaderManager().destroyLoader(1);

        }
    };

    private LoaderCallbacks<List<User>> mCollaboratorListCallback =
            new LoaderCallbacks<List<User>>() {
        @Override
        public Loader<LoaderResult<List<User>>> onCreateLoader(int id, Bundle args) {
            return new CollaboratorListLoader(IssueListActivity.this, mRepoOwner, mRepoName);
        }

        @Override
        public void onResultReady(LoaderResult<List<User>> result) {
            if (checkForError(result)) {
                return;
            }
            stopProgressDialog(mProgressDialog);
            mAssignees = result.getData();
            showAssigneesDialog();
            getSupportLoaderManager().destroyLoader(2);
        }
    };

    private LoaderCallbacks<Boolean> mIsCollaboratorCallback = new LoaderCallbacks<Boolean>() {
        @Override
        public Loader<LoaderResult<Boolean>> onCreateLoader(int id, Bundle args) {
            return new IsCollaboratorLoader(IssueListActivity.this, mRepoOwner, mRepoName);
        }

        @Override
        public void onResultReady(LoaderResult<Boolean> result) {
            if (checkForError(result)) {
                return;
            }
            mIsCollaborator = result.getData();
            invalidateOptionsMenu();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Gh4Application.THEME);
        super.onCreate(savedInstanceState);
        if (hasErrorView()) {
            return;
        }

        Bundle data = getIntent().getExtras();
        mRepoOwner = data.getString(Constants.Repository.OWNER);
        mRepoName = data.getString(Constants.Repository.NAME);
        mSortMode = SORTMODES[0];
        mSortAscending = false;

        if (TextUtils.equals(data.getString(Constants.Issue.STATE), Constants.Issue.STATE_CLOSED)) {
            getPager().setCurrentItem(1);
        }

        getSupportLoaderManager().initLoader(3, null, mIsCollaboratorCallback);

        mActionBar = getSupportActionBar();
        mActionBar.setTitle(R.string.issues);
        mActionBar.setSubtitle(mRepoOwner + "/" + mRepoName);
        mActionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected int[] getTabTitleResIds() {
        return TITLES;
    }

    @Override
    protected Fragment getFragment(int position) {
        Map<String, String> filterData = new HashMap<String, String>();
        filterData.put("sort", mSortMode);
        filterData.put("direction", mSortAscending ? "asc" : "desc");
        if (mSelectedLabels != null) {
            filterData.put("labels", TextUtils.join(",", mSelectedLabels));
        }
        if (mSelectedMilestone > 0) {
            filterData.put("milestone", String.valueOf(mSelectedMilestone));
        }
        if (mSelectedAssignee != null) {
            filterData.put("assignee", mSelectedAssignee);
        }

        if (position == 1) {
            filterData.put(Constants.Issue.STATE, Constants.Issue.STATE_CLOSED);
            mClosedFragment = IssueListFragment.newInstance(mRepoOwner, mRepoName, filterData);
            return mClosedFragment;
        } else {
            filterData.put(Constants.Issue.STATE, Constants.Issue.STATE_OPEN);
            mOpenFragment = IssueListFragment.newInstance(mRepoOwner, mRepoName, filterData);
            return mOpenFragment;
        }
    }

    @Override
    protected boolean fragmentNeedsRefresh(Fragment object) {
        if (object instanceof IssueListFragment) {
            if (object != mOpenFragment && object != mClosedFragment) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.issues_menu, menu);
        if (!mIsCollaborator) {
            menu.removeItem(R.id.view_labels);
            menu.removeItem(R.id.view_milestones);
        }
        for (int i = 0; i < SORTMODES.length; i++) {
            if (SORTMODES[i].equals(mSortMode)) {
                menu.findItem(SORTMODE_MENU_IDS[i]).setChecked(true);
                break;
            }
        }

        int dirItemId = mSortAscending ? R.id.sort_ascending : R.id.sort_descending;
        menu.findItem(dirItemId).setChecked(true);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void navigateUp() {
        IntentUtils.openRepositoryInfoActivity(this, mRepoOwner, mRepoName,
                null, Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.sort_by_comments:
            case R.id.sort_by_created:
            case R.id.sort_by_updated:
                for (int i = 0; i < SORTMODE_MENU_IDS.length; i++) {
                    item.setChecked(SORTMODE_MENU_IDS[i] == id);
                    if (item.isChecked()) {
                        mSortMode = SORTMODES[i];
                    }
                }
                reloadIssueList();
                return true;
            case R.id.create_issue:
                if (Gh4Application.get(this).isAuthorized()) {
                    Intent intent = new Intent(this, IssueCreateActivity.class);
                    intent.putExtra(Constants.Repository.OWNER, mRepoOwner);
                    intent.putExtra(Constants.Repository.NAME, mRepoName);
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(this, Github4AndroidActivity.class);
                    startActivity(intent);
                    finish();
                }
                return true;
            case R.id.view_labels:
                Intent intent = new Intent(this, IssueLabelListActivity.class);
                intent.putExtra(Constants.Repository.OWNER, mRepoOwner);
                intent.putExtra(Constants.Repository.NAME, mRepoName);
                startActivity(intent);
                return true;
            case R.id.view_milestones:
                intent = new Intent(this, IssueMilestoneListActivity.class);
                intent.putExtra(Constants.Repository.OWNER, mRepoOwner);
                intent.putExtra(Constants.Repository.NAME, mRepoName);
                startActivity(intent);
                return true;
            case R.id.sort_ascending:
                mSortAscending = true;
                item.setChecked(true);
                reloadIssueList();
                return true;
            case R.id.sort_descending:
                mSortAscending = false;
                item.setChecked(true);
                reloadIssueList();
                return true;
            case R.id.labels:
                if (mLabels == null) {
                    mProgressDialog = showProgressDialog(getString(R.string.loading_msg), true);
                    getSupportLoaderManager().initLoader(0, null, mLabelCallback);
                } else {
                    showLabelsDialog();
                }
                return true;
            case R.id.milestones:
                if (mMilestones == null) {
                    mProgressDialog = showProgressDialog(getString(R.string.loading_msg), true);
                    getSupportLoaderManager().initLoader(1, null, mMilestoneCallback);
                } else {
                    showMilestonesDialog();
                }
                return true;
            case R.id.assignees:
                if (mAssignees == null) {
                    mProgressDialog = showProgressDialog(getString(R.string.loading_msg), true);
                    getSupportLoaderManager().initLoader(2, null, mCollaboratorListCallback);
                } else {
                    showAssigneesDialog();
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void reloadIssueList() {
        mOpenFragment = null;
        mClosedFragment = null;
        invalidateFragments();
    }

    private void showLabelsDialog() {
        final boolean[] checkedItems = new boolean[mLabels.size()];
        final String[] allLabelArray = new String[mLabels.size()];

        for (int i = 0; i < mLabels.size(); i++) {
            Label l = mLabels.get(i);
            allLabelArray[i] = l.getName();
            checkedItems[i] = mSelectedLabels != null && mSelectedLabels.contains(l.getName());
        }

        DialogInterface.OnMultiChoiceClickListener selectCb =
                new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton, boolean isChecked) {
                checkedItems[whichButton] = isChecked;
            }
        };
        DialogInterface.OnClickListener okCb = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                mSelectedLabels = new ArrayList<String>();
                for (int i = 0; i < allLabelArray.length; i++) {
                    if (checkedItems[i]) {
                        mSelectedLabels.add(allLabelArray[i]);
                    }
                }
                reloadIssueList();
            }
        };

        new AlertDialog.Builder(this)
                .setCancelable(true)
                .setTitle(R.string.issue_filter_by_labels)
                .setMultiChoiceItems(allLabelArray, checkedItems, selectCb)
                .setPositiveButton(R.string.ok, okCb)
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showMilestonesDialog() {
        String[] milestones = new String[mMilestones.size() + 1];
        final int[] milestoneIds = new int[mMilestones.size() + 1];

        milestones[0] = getResources().getString(R.string.issue_filter_by_any_milestone);
        milestoneIds[0] = 0;

        mPendingSelectedItem = 0;

        for (int i = 1; i <= mMilestones.size(); i++) {
            Milestone m = mMilestones.get(i - 1);
            milestones[i] = m.getTitle();
            milestoneIds[i] = m.getNumber();
            if (m.getNumber() == mSelectedMilestone) {
                mPendingSelectedItem = i;
            }
        }

        DialogInterface.OnClickListener selectCb = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mPendingSelectedItem = which;
            }
        };
        DialogInterface.OnClickListener okCb = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mSelectedMilestone = milestoneIds[mPendingSelectedItem];
                reloadIssueList();
            }
        };

        new AlertDialog.Builder(this)
                .setCancelable(true)
                .setTitle(R.string.issue_filter_by_milestone)
                .setSingleChoiceItems(milestones, mPendingSelectedItem, selectCb)
                .setPositiveButton(R.string.ok, okCb)
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showAssigneesDialog() {
        final String[] assignees = new String[mAssignees.size() + 1];

        assignees[0] = getResources().getString(R.string.issue_filter_by_any_assignee);

        mPendingSelectedItem = 0;

        for (int i = 1; i <= mAssignees.size(); i++) {
            User u = mAssignees.get(i - 1);
            assignees[i] = u.getLogin();
            if (u.getLogin().equalsIgnoreCase(mSelectedAssignee)) {
                mPendingSelectedItem = i;
            }
        }

        DialogInterface.OnClickListener selectCb = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mPendingSelectedItem = which;
            }
        };
        DialogInterface.OnClickListener okCb = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mSelectedAssignee = mPendingSelectedItem != 0
                        ? mAssignees.get(mPendingSelectedItem - 1).getLogin() : null;
                reloadIssueList();
            }
        };

        new AlertDialog.Builder(this)
                .setCancelable(true)
                .setTitle(R.string.issue_filter_by_assignee)
                .setSingleChoiceItems(assignees, mPendingSelectedItem, selectCb)
                .setPositiveButton(R.string.ok, okCb)
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private boolean checkForError(LoaderResult<?> result) {
        if (result.handleError(IssueListActivity.this)) {
            stopProgressDialog(mProgressDialog);
            invalidateOptionsMenu();
            return true;
        }
        return false;
    }
}