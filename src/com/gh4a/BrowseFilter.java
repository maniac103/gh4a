package com.gh4a;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.egit.github.core.client.IGitHubConstants;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

import com.gh4a.activities.BaseSherlockFragmentActivity;
import com.gh4a.activities.BlogListActivity;
import com.gh4a.activities.ExploreActivity;
import com.gh4a.activities.WikiListActivity;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.StringUtils;
import com.gh4a.utils.ToastUtils;

public class BrowseFilter extends BaseSherlockFragmentActivity {

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Uri uri = getIntent().getData();
        if (uri == null) {
            finish();
            return;
        }

        List<String> parts = uri.getPathSegments();

        String first = parts.isEmpty() ? null : parts.get(0);
        if (IGitHubConstants.HOST_GISTS.equals(uri.getHost())) {
            if (parts.size() >= 2) {
                IntentUtils.openGistActivity(this, parts.get(0), parts.get(1), 0);
            } else {
                launchBrowser(uri);
            }
        } else if (first == null
                || "languages".equals(first)
                || "training".equals(first)
                || "login".equals(first)
                || "contact".equals(first)
                || "features".equals(first)) {
            startActivity(createBrowserIntent(uri));
        } else if ("explore".equals(first)) {
            Intent intent = new Intent(this, ExploreActivity.class);
            startActivity(intent);
        } else if ("blog".equals(first)) {
            Intent intent = new Intent(this, BlogListActivity.class);
            startActivity(intent);
        } else {
            // strip off extra data like line numbers etc.
            String last = parts.get(parts.size() - 1);
            int pos = last.indexOf('#');
            if (pos >= 0) {
                parts.set(parts.size() - 1, last.substring(0, pos));
            }

            String user = first;
            String repo = parts.size() >= 2 ? parts.get(1) : null;
            String action = parts.size() >= 3 ? parts.get(2) : null;
            String id = parts.size() >= 4 ? parts.get(3) : null;

            if (repo == null && action == null) {
                IntentUtils.openUserInfoActivity(this, user);
            } else if (action == null || "tree".equals(action)) {
                String ref = "tree".equals(action) ? id : null;
                IntentUtils.openRepositoryInfoActivity(this, user, repo, ref, 0);
            } else if ("issues".equals(action)) {
                if (!StringUtils.isBlank(id)) {
                    try {
                        IntentUtils.openIssueActivity(this, user, repo, Integer.parseInt(id));
                    } catch (NumberFormatException e) {
                    }
                } else {
                    IntentUtils.openIssueListActivity(this, user, repo, Constants.Issue.STATE_OPEN);
                }
            } else if ("pulls".equals(action)) {
                IntentUtils.openPullRequestListActivity(this, user, repo, Constants.Issue.STATE_OPEN);
            } else if ("wiki".equals(action)) {
                Intent intent = new Intent(this, WikiListActivity.class);
                intent.putExtra(Constants.Repository.OWNER, user);
                intent.putExtra(Constants.Repository.NAME, repo);
                startActivity(intent);
            } else if ("pull".equals(action) && !StringUtils.isBlank(id)) {
                try {
                    IntentUtils.openPullRequestActivity(this, user, repo, Integer.parseInt(id));
                } catch (NumberFormatException e) {
                }
            } else if ("commit".equals(action) && !StringUtils.isBlank(id)) {
                IntentUtils.openCommitInfoActivity(this, user, repo, id, 0);
            } else if ("commits".equals(action) && !StringUtils.isBlank(id)) {
                IntentUtils.openRepositoryInfoActivity(this, user, repo, id, 0);
            } else if ("blob".equals(action) && !StringUtils.isBlank(id) && parts.size() >= 5) {
                String fullPath = TextUtils.join("/", parts.subList(4, parts.size()));
                IntentUtils.openFileViewerActivity(this, user, repo, id, fullPath, uri.getLastPathSegment());
            } else {
                launchBrowser(uri);
            }
        }
        finish();
    }

    private void launchBrowser(Uri uri) {
        Intent intent = createBrowserIntent(uri);
        if (intent != null) {
            startActivity(intent);
        } else {
            ToastUtils.showMessage(this, R.string.no_browser_found);
        }
    }

    // We want to forward the URI to a browser, but our own intent filter matches
    // the browser's intent filters. We therefore resolve the intent by ourselves,
    // strip our own entry from the list and pass the result to the system's
    // activity chooser.
    private Intent createBrowserIntent(Uri uri) {
        final Intent browserIntent = new Intent(Intent.ACTION_VIEW, uri)
                .addCategory(Intent.CATEGORY_BROWSABLE);
        final PackageManager pm = getPackageManager();
        final List<ResolveInfo> activities = pm.queryIntentActivities(browserIntent,
                PackageManager.MATCH_DEFAULT_ONLY);
        final ArrayList<Intent> chooserIntents = new ArrayList<Intent>();

        Collections.sort(activities, new ResolveInfo.DisplayNameComparator(pm));

        for (ResolveInfo resInfo : activities) {
            ActivityInfo info = resInfo.activityInfo;
            if (!info.enabled || !info.exported) {
                continue;
            }
            if (info.packageName.equals(getPackageName())) {
                continue;
            }

            Intent targetIntent = new Intent(browserIntent);
            targetIntent.setPackage(info.packageName);
            chooserIntents.add(targetIntent);
        }

        if (chooserIntents.isEmpty()) {
            return null;
        }

        final Intent lastIntent = chooserIntents.remove(chooserIntents.size() - 1);
        if (chooserIntents.isEmpty()) {
            // there was only one, no need to show the chooser
            return lastIntent;
        }

        Intent chooserIntent = Intent.createChooser(lastIntent, null);
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS,
                chooserIntents.toArray(new Intent[chooserIntents.size()]));
        return chooserIntent;
    }
}
