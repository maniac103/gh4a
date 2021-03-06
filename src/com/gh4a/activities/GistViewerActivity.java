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
import android.support.v4.content.Loader;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.gh4a.Constants;
import com.gh4a.R;
import com.gh4a.loader.GistLoader;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.StringUtils;

import org.eclipse.egit.github.core.Gist;
import org.eclipse.egit.github.core.GistFile;

public class GistViewerActivity extends WebViewerActivity {
    private String mUserLogin;
    private String mFileName;
    private String mGistId;
    private GistFile mGistFile;

    private LoaderCallbacks<Gist> mGistCallback = new LoaderCallbacks<Gist>() {
        @Override
        public Loader<LoaderResult<Gist>> onCreateLoader(int id, Bundle args) {
            return new GistLoader(GistViewerActivity.this, mGistId);
        }
        @Override
        public void onResultReady(LoaderResult<Gist> result) {
            boolean success = !result.handleError(GistViewerActivity.this);
            if (success) {
                mGistFile = result.getData().getFiles().get(mFileName);
                loadThemedHtml(StringUtils.highlightSyntax(mGistFile.getContent(),
                        true, mFileName, null, null, null));
            } else {
                setContentEmpty(true);
                setContentShown(true);
            }
            invalidateOptionsMenu();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (hasErrorView()) {
            return;
        }

        mUserLogin = getIntent().getExtras().getString(Constants.User.LOGIN);
        mFileName = getIntent().getExtras().getString(Constants.Gist.FILENAME);
        mGistId = getIntent().getExtras().getString(Constants.Gist.ID);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(mFileName);
        actionBar.setDisplayHomeAsUpEnabled(true);

        getSupportLoaderManager().initLoader(0, null, mGistCallback);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.download_menu, menu);

        menu.removeItem(R.id.download);
        menu.removeItem(R.id.share);
        if (mGistFile == null) {
            menu.removeItem(R.id.browser);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void navigateUp() {
        IntentUtils.openGistActivity(this, mUserLogin, mGistId, Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.browser:
                IntentUtils.launchBrowser(this, Uri.parse(mGistFile.getRawUrl()));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}