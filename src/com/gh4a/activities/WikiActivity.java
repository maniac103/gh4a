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

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.actionbarsherlock.app.ActionBar;
import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.LoadingFragmentActivity;
import com.gh4a.R;

public class WikiActivity extends WebViewerActivity {
    private String mUserLogin;
    private String mRepoName;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (hasErrorView()) {
            return;
        }

        mUserLogin = getIntent().getStringExtra(Constants.Repository.OWNER);
        mRepoName = getIntent().getStringExtra(Constants.Repository.NAME);
        String title = getIntent().getStringExtra(Constants.Blog.TITLE);
        String content = getIntent().getStringExtra(Constants.Blog.CONTENT);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(title);
        actionBar.setSubtitle(mUserLogin + "/" + mRepoName);
        actionBar.setDisplayHomeAsUpEnabled(true);

        mWebView.loadDataWithBaseURL("https://github.com", content, "text/html", "utf-8", null);
    }

    @Override
    protected void navigateUp() {
        Intent intent = new Intent(this, WikiListActivity.class);
        intent.putExtra(Constants.Repository.OWNER, mUserLogin);
        intent.putExtra(Constants.Repository.NAME, mRepoName);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }
}
