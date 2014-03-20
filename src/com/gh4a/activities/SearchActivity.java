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

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.SearchUser;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.eclipse.egit.github.core.service.UserService;

import android.content.Context;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.widget.SearchView;
import com.gh4a.Gh4Application;
import com.gh4a.ProgressDialogTask;
import com.gh4a.R;
import com.gh4a.adapter.RepositoryAdapter;
import com.gh4a.adapter.SearchUserAdapter;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.StringUtils;
import com.gh4a.utils.UiUtils;

public class SearchActivity extends BaseSherlockFragmentActivity implements
        SearchView.OnQueryTextListener, SearchView.OnCloseListener,
        AdapterView.OnItemSelectedListener, AdapterView.OnItemClickListener {

    protected SearchUserAdapter mUserAdapter;
    protected RepositoryAdapter mRepoAdapter;
    protected ListView mListViewResults;

    private Spinner mSearchType;
    private SearchView mSearch;
    private String mQuery;
    private boolean mSubmitted;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Gh4Application.THEME);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.search);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.search);

        LayoutInflater inflater = LayoutInflater.from(actionBar.getThemedContext());
        LinearLayout searchLayout = (LinearLayout) inflater.inflate(R.layout.search_action_bar, null);
        actionBar.setCustomView(searchLayout);
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        mSearchType = (Spinner) searchLayout.findViewById(R.id.search_type);
        mSearchType.setAdapter(new SearchTypeAdapter(actionBar.getThemedContext()));
        mSearchType.setOnItemSelectedListener(this);

        mSearch = (SearchView) searchLayout.findViewById(R.id.search_view);
        mSearch.setIconifiedByDefault(true);
        mSearch.requestFocus();
        mSearch.setIconified(false);
        mSearch.setOnQueryTextListener(this);
        mSearch.setOnCloseListener(this);
        mSearch.onActionViewExpanded();

        updateSearchTypeHint();

        mListViewResults = (ListView) findViewById(R.id.list_search);
        mListViewResults.setOnItemClickListener(this);
        registerForContextMenu(mListViewResults);
    }

    protected void searchRepository(final String searchKey) {
        mRepoAdapter = new RepositoryAdapter(this);
        mListViewResults.setAdapter(mRepoAdapter);
        new LoadRepositoryTask(searchKey).execute();
    }

    protected void searchUser(final String searchKey) {
        mUserAdapter = new SearchUserAdapter(this);
        mListViewResults.setAdapter(mUserAdapter);
        new LoadUserTask(searchKey).execute();
    }

    private static class SearchTypeAdapter extends BaseAdapter implements SpinnerAdapter {
        private Context mContext;

        private final int[][] mResources = new int[][] {
            { R.string.search_type_repo, R.attr.searchRepoMenuIcon, 0 },
            { R.string.search_type_user, R.attr.searchUserMenuIcon, 0 }
        };

        SearchTypeAdapter(Context context) {
            mContext = context;
            for (int i = 0; i < mResources.length; i++) {
                mResources[i][2] = UiUtils.resolveDrawable(context, mResources[i][1]);
            }
        }

        @Override
        public int getCount() {
            return mResources.length;
        }

        @Override
        public CharSequence getItem(int position) {
            return mContext.getString(mResources[position][0]);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(mContext);
                convertView = inflater.inflate(R.layout.search_type_small, null);
            }

            ImageView icon = (ImageView) convertView.findViewById(R.id.icon);
            icon.setImageResource(mResources[position][2]);

            return convertView;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(mContext);
                convertView = inflater.inflate(R.layout.search_type_popup, null);
            }

            ImageView icon = (ImageView) convertView.findViewById(R.id.icon);
            icon.setImageResource(mResources[position][2]);

            TextView label = (TextView) convertView.findViewById(R.id.label);
            label.setText(mResources[position][0]);

            return convertView;
        }
    }

    protected void fillRepositoriesData(List<Repository> repos) {
        if (mRepoAdapter != null) {
            mRepoAdapter.clear();
            if (repos != null) {
                mRepoAdapter.addAll(repos);
            }
            mRepoAdapter.notifyDataSetChanged();
        }
    }

    protected void fillUsersData(List<SearchUser> users) {
        if (mUserAdapter != null) {
            mUserAdapter.clear();
            if (users != null) {
                mUserAdapter.addAll(users);
            }
            mUserAdapter.notifyDataSetChanged();
        }
    }

    private class LoadRepositoryTask extends ProgressDialogTask<List<Repository>> {
        private String mQuery;

        public LoadRepositoryTask(String query) {
            super(SearchActivity.this, 0, R.string.loading_msg);
            mQuery = query;
        }

        @Override
        protected List<Repository> run() throws IOException {
            if (StringUtils.isBlank(mQuery)) {
                return null;
            }

            RepositoryService repoService = (RepositoryService)
                    Gh4Application.get(mContext).getService(Gh4Application.REPO_SERVICE);
            HashMap<String, String> params = new HashMap<String, String>();
            params.put("fork", "true");

            return repoService.searchRepositories(mQuery, params);
        }

        @Override
        protected void onSuccess(List<Repository> result) {
            fillRepositoriesData(result);
        }
    }

    private class LoadUserTask extends ProgressDialogTask<List<SearchUser>> {
        private String mQuery;

        public LoadUserTask(String query) {
            super(SearchActivity.this, 0, R.string.loading_msg);
            mQuery = query;
        }

        @Override
        protected List<SearchUser> run() throws IOException {
            if (StringUtils.isBlank(mQuery)) {
                return null;
            }

            UserService userService = (UserService)
                    Gh4Application.get(mContext).getService(Gh4Application.USER_SERVICE);
            return userService.searchUsers(mQuery);
        }

        @Override
        protected void onSuccess(List<SearchUser> result) {
            fillUsersData(result);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        menu.clear();// clear items

        if (v.getId() == R.id.list_search) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            ListAdapter listAdapter = mListViewResults.getAdapter();
            Object object = listAdapter.getItem(info.position);
            menu.setHeaderTitle(R.string.go_to);

            if (object instanceof SearchUser) {
                /** Menu for user */
                SearchUser user = (SearchUser) object;
                menu.add(getString(R.string.menu_user, StringUtils.formatName(user.getLogin(), user.getName())));
            } else {
                /** Menu for repository */
                Repository repository = (Repository) object;
                menu.add(getString(R.string.menu_user, repository.getOwner()));
                menu.add(getString(R.string.menu_repo, repository.getName()));
            }
        }
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item
                .getMenuInfo();

        ListAdapter listAdapter = mListViewResults.getAdapter();
        Object object = listAdapter.getItem(info.position);

        if (object instanceof SearchUser) {
            /** User item */
            SearchUser user = (SearchUser) object;
            IntentUtils.openUserInfoActivity(this, user.getLogin(), user.getName());
        } else {
            /** Repo item */
            Repository repository = (Repository) object;
            IntentUtils.openRepositoryInfoActivity(this, repository.getOwner().getLogin(),
                    repository.getName(), null, 0);
        }
        return true;
    }

    private void updateSearchTypeHint() {
        switch (mSearchType.getSelectedItemPosition()) {
            case 0: mSearch.setQueryHint(getString(R.string.search_hint_repo)); break;
            case 1: mSearch.setQueryHint(getString(R.string.search_hint_user)); break;
            default: mSearch.setQueryHint(null); break;
        }
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        boolean searchUser = mSearchType.getSelectedItemPosition() == 1;
        if (searchUser) {
            searchUser(query);
        } else {
            searchRepository(query);
        }
        mSubmitted = true;
        mSearch.clearFocus();
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        mQuery = newText;
        mSubmitted = false;
        return false;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        updateSearchTypeHint();
        if (mSubmitted) {
            onQueryTextSubmit(mQuery);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        updateSearchTypeHint();
    }

    @Override
    public boolean onClose() {
        if (mUserAdapter != null) {
            mUserAdapter.clear();
            mUserAdapter.notifyDataSetChanged();
        }
        if (mRepoAdapter != null) {
            mRepoAdapter.clear();
            mRepoAdapter.notifyDataSetChanged();
        }
        mQuery = null;
        mSubmitted = false;
        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Object object = parent.getAdapter().getItem(position);

        if (object instanceof SearchUser) {
            SearchUser user = (SearchUser) object;
            IntentUtils.openUserInfoActivity(this, user.getLogin(), user.getName());
        } else if (object instanceof Repository) {
            Repository repository = (Repository) object;
            IntentUtils.openRepositoryInfoActivity(this, repository.getOwner().getLogin(),
                    repository.getName(), null, 0);
        }
    }

    @Override
    protected void navigateUp() {
        finish();
    }
}