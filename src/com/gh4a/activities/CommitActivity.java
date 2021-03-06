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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.LoadingFragmentPagerActivity;
import com.gh4a.R;
import com.gh4a.fragment.CommitFragment;
import com.gh4a.fragment.CommitNoteFragment;
import com.gh4a.utils.IntentUtils;

public class CommitActivity extends LoadingFragmentPagerActivity {
    private String mRepoOwner;
    private String mRepoName;
    private String mObjectSha;

    private static final int[] TITLES = new int[] {
        R.string.commits, R.string.issue_comments
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
        mObjectSha = data.getString(Constants.Object.OBJECT_SHA);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(getResources().getQuantityString(R.plurals.commit, 1)
                + " " + mObjectSha.substring(0, 7));
        actionBar.setSubtitle(mRepoOwner + "/" + mRepoName);
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected int[] getTabTitleResIds() {
        return TITLES;
    }

    @Override
    protected Fragment getFragment(int position) {
        if (position == 1) {
            return CommitNoteFragment.newInstance(mRepoOwner, mRepoName, mObjectSha);
        } else {
            return CommitFragment.newInstance(mRepoOwner, mRepoName, mObjectSha);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.download_menu, menu);

        menu.removeItem(R.id.download);
        menu.removeItem(R.id.search);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void navigateUp() {
        IntentUtils.openRepositoryInfoActivity(this,
                mRepoOwner, mRepoName, null, Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        String diffUrl = "https://github.com/" + mRepoOwner + "/" + mRepoName + "/commit/" + mObjectSha;
        switch (item.getItemId()) {
            case R.id.browser:
                IntentUtils.launchBrowser(this, Uri.parse(diffUrl));
                return true;
            case R.id.share:
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_commit_subject,
                        mObjectSha.substring(0, 7), mRepoOwner + "/" + mRepoName));
                shareIntent.putExtra(Intent.EXTRA_TEXT, diffUrl);
                shareIntent = Intent.createChooser(shareIntent, getString(R.string.share_title));
                startActivity(shareIntent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}