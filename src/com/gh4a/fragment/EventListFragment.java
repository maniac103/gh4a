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
package com.gh4a.fragment;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.egit.github.core.Commit;
import org.eclipse.egit.github.core.GollumPage;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.event.CommitCommentPayload;
import org.eclipse.egit.github.core.event.DownloadPayload;
import org.eclipse.egit.github.core.event.Event;
import org.eclipse.egit.github.core.event.EventRepository;
import org.eclipse.egit.github.core.event.FollowPayload;
import org.eclipse.egit.github.core.event.ForkApplyPayload;
import org.eclipse.egit.github.core.event.ForkPayload;
import org.eclipse.egit.github.core.event.GistPayload;
import org.eclipse.egit.github.core.event.GollumPayload;
import org.eclipse.egit.github.core.event.IssueCommentPayload;
import org.eclipse.egit.github.core.event.IssuesPayload;
import org.eclipse.egit.github.core.event.MemberPayload;
import org.eclipse.egit.github.core.event.PullRequestPayload;
import org.eclipse.egit.github.core.event.PullRequestReviewCommentPayload;
import org.eclipse.egit.github.core.event.PushPayload;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockFragment;
import com.gh4a.BaseSherlockFragmentActivity;
import com.gh4a.CompareActivity;
import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.WikiListActivity;
import com.gh4a.adapter.FeedAdapter;
import com.gh4a.loader.EventListLoader;

public class EventListFragment extends SherlockFragment 
    implements LoaderManager.LoaderCallbacks<List<Event>>, OnItemClickListener {

    private String mLogin;
    private boolean mIsPrivate;
    private ListView mListView;
    private FeedAdapter mAdapter;

    public static EventListFragment newInstance(String login, boolean isPrivate) {
        EventListFragment f = new EventListFragment();

        Bundle args = new Bundle();
        args.putString(Constants.User.USER_LOGIN, login);
        args.putBoolean(Constants.Event.IS_PRIVATE, isPrivate);
        f.setArguments(args);
        
        return f;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(Constants.LOG_TAG, ">>>>>>>>>>> onCreate EventListFragment");
        super.onCreate(savedInstanceState);
        mLogin = getArguments().getString(Constants.User.USER_LOGIN);
        mIsPrivate = getArguments().getBoolean(Constants.Event.IS_PRIVATE);
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        Log.i(Constants.LOG_TAG, ">>>>>>>>>>> onCreateView EventListFragment");
        View v = inflater.inflate(R.layout.generic_list, container, false);
        mListView = (ListView) v.findViewById(R.id.list_view);
        return v;
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.i(Constants.LOG_TAG, ">>>>>>>>>>> onActivityCreated EventListFragment");
        super.onActivityCreated(savedInstanceState);
        
        mAdapter = new FeedAdapter(getSherlockActivity(), new ArrayList<Event>());
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(this);
        registerForContextMenu(mListView);
        
        getLoaderManager().initLoader(0, null, this);
        getLoaderManager().getLoader(0).forceLoad();
    }
    
    private void fillData(List<Event> events) {
        if (events != null && events.size() > 0) {
            mAdapter.addAll(events);
        }
        mAdapter.notifyDataSetChanged();
    }
    
    @Override
    public Loader<List<Event>> onCreateLoader(int id, Bundle args) {
        return new EventListLoader(getSherlockActivity(), mLogin, mIsPrivate);
    }

    @Override
    public void onLoadFinished(Loader<List<Event>> loader, List<Event> events) {
        fillData(events);
    }

    @Override
    public void onLoaderReset(Loader<List<Event>> arg0) {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Event event = (Event) adapterView.getAdapter().getItem(position);
        Gh4Application context = ((BaseSherlockFragmentActivity) getActivity()).getApplicationContext();
        
        //if payload is a base class, return void.  Think that it is an old event which not supported
        //by API v3.
        if (event.getPayload().getClass().getSimpleName().equals("EventPayload")) {
            return;
        }
        
        String eventType = event.getType();
        EventRepository eventRepo = event.getRepo();
        String[] repoNamePart = eventRepo.getName().split("/");
        String repoOwner = "";
        String repoName = "";
        if (repoNamePart.length == 2) {
            repoOwner = repoNamePart[0];
            repoName = repoNamePart[1];
        }
        String repoUrl = eventRepo.getUrl();
        
        /** PushEvent */
        if (Event.TYPE_PUSH.equals(eventType)) {
            
            if (eventRepo != null) {
                PushPayload payload = (PushPayload) event.getPayload();

                List<Commit> commits = payload.getCommits();
                // if commit > 1, then show compare activity
                
                if (commits != null && commits.size() > 1) {
                    Intent intent = new Intent().setClass(context, CompareActivity.class);
                    for (Commit commit : commits) {
                        String[] commitInfo = new String[4];
                        commitInfo[0] = commit.getSha();
                        commitInfo[1] = commit.getAuthor().getEmail();
                        commitInfo[2] = commit.getMessage();
                        commitInfo[3] = commit.getAuthor().getName();
                        intent.putExtra("commit" + commit.getSha(), commitInfo);
                    }
                    
                    intent.putExtra(Constants.Repository.REPO_OWNER, repoOwner);
                    intent.putExtra(Constants.Repository.REPO_NAME, repoName);
                    startActivity(intent);
                }
                // only 1 commit, then show the commit details
                else {
                    context.openCommitInfoActivity(getSherlockActivity(), repoOwner, repoName,
                            payload.getCommits().get(0).getSha());
                }
            }
            else {
                context.notFoundMessage(getSherlockActivity(), R.plurals.repository);
            }
        }

        /** IssueEvent */
        else if (Event.TYPE_ISSUES.equals(eventType)) {
            if (eventRepo != null) {
                IssuesPayload payload = (IssuesPayload) event.getPayload();
                context.openIssueActivity(getSherlockActivity(), repoOwner, repoName, payload.getIssue().getNumber());
            }
            else {
                context.notFoundMessage(getSherlockActivity(), R.plurals.repository);
            }
        }

        /** WatchEvent */
        else if (Event.TYPE_WATCH.equals(eventType)) {
            if (eventRepo != null) {
                context.openRepositoryInfoActivity(getSherlockActivity(), repoOwner, repoName);
            }
            else {
                context.notFoundMessage(getSherlockActivity(), R.plurals.repository);
            }
        }

        /** CreateEvent */
        else if (Event.TYPE_CREATE.equals(eventType)) {
            if (eventRepo != null) {
                context.openRepositoryInfoActivity(this.getSherlockActivity(), repoOwner, repoName);
            }
            else {
                context.notFoundMessage(getSherlockActivity(), R.plurals.repository);
            }
        }

        /** PullRequestEvent */
        else if (Event.TYPE_PULL_REQUEST.equals(eventType)) {
            if (eventRepo != null) {
                PullRequestPayload payload = (PullRequestPayload) event.getPayload();
                int pullRequestNumber = payload.getNumber();
                context.openPullRequestActivity(getSherlockActivity(), repoOwner, repoName, pullRequestNumber);
            }
            else {
                context.notFoundMessage(getSherlockActivity(), R.plurals.repository);
            }
        }

        /** FollowEvent */
        else if (Event.TYPE_FOLLOW.equals(eventType)) {
            FollowPayload payload = (FollowPayload) event.getPayload();
            if (payload.getTarget() != null) {
                context.openUserInfoActivity(getSherlockActivity(), payload.getTarget().getLogin(), null);
            }
        }

        /** CommitCommentEvent */
        else if (Event.TYPE_COMMIT_COMMENT.equals(eventType)) {
            if (eventRepo != null) {
                CommitCommentPayload payload = (CommitCommentPayload) event.getPayload();
                context.openCommitInfoActivity(getSherlockActivity(), repoOwner, repoName, 
                        payload.getComment().getCommitId());
            }
            else {
                context.notFoundMessage(getSherlockActivity(), R.plurals.repository);
            }
        }

        /** DeleteEvent */
        else if (Event.TYPE_DELETE.equals(eventType)) {
            if (eventRepo != null) {
                context.openRepositoryInfoActivity(getSherlockActivity(), repoOwner, repoName);
            }
            else {
                context.notFoundMessage(getSherlockActivity(), R.plurals.repository);
            }
        }

        /** DownloadEvent */
        else if (Event.TYPE_DOWNLOAD.equals(eventType)) {
            if (eventRepo != null) {
                DownloadPayload payload = (DownloadPayload) event.getPayload();
                context.openBrowser(getSherlockActivity(), payload.getDownload().getHtmlUrl());
            }
            else {
                context.notFoundMessage(getSherlockActivity(), R.plurals.repository);
            }
        }

        /** ForkEvent */
        else if (Event.TYPE_FORK.equals(eventType)) {
            ForkPayload payload = (ForkPayload) event.getPayload();
            Repository forkee = payload.getForkee();
            if (forkee != null) {
                context.openRepositoryInfoActivity(getSherlockActivity(), forkee);
            }
            else {
                context.notFoundMessage(getSherlockActivity(), R.plurals.repository);
            }
        }

        /** ForkEvent */
        else if (Event.TYPE_FORK_APPLY.equals(eventType)) {
            if (eventRepo != null) {
                ForkApplyPayload payload = (ForkApplyPayload) event.getPayload();
                context.openRepositoryInfoActivity(getSherlockActivity(), repoOwner, repoName);
            }
            else {
                context.notFoundMessage(getSherlockActivity(), R.plurals.repository);
            }
        }

        /** GollumEvent */
        else if (Event.TYPE_GOLLUM.equals(eventType)) {
            Intent intent = new Intent().setClass(getSherlockActivity(), WikiListActivity.class);
            intent.putExtra(Constants.Repository.REPO_OWNER, repoOwner);
            intent.putExtra(Constants.Repository.REPO_NAME, repoName);
            startActivity(intent);
        }

        /** PublicEvent */
        else if (Event.TYPE_PUBLIC.equals(eventType)) {
            if (eventRepo != null) {
                context.openRepositoryInfoActivity(getSherlockActivity(), repoOwner, repoName);
            }
            else {
                context.notFoundMessage(getSherlockActivity(), R.plurals.repository);
            }
        }
        
        /** MemberEvent */
        else if (Event.TYPE_MEMBER.equals(eventType)) {
            if (eventRepo != null) {
                MemberPayload payload = (MemberPayload) event.getPayload();
                context.openRepositoryInfoActivity(getSherlockActivity(), repoOwner, repoName);
            }
            else {
                context.notFoundMessage(getSherlockActivity(), R.plurals.repository);
            }
        }
        
        /** Gist Event **/
        else if (Event.TYPE_GIST.equals(eventType)) {
            GistPayload payload = (GistPayload) event.getPayload();
            context.openGistActivity(getSherlockActivity(), payload.getGist().getId());
        }
        
        /** IssueCommentEvent */
        else if (Event.TYPE_ISSUE_COMMENT.equals(eventType)) {
            if (eventRepo != null) {
                IssueCommentPayload payload = (IssueCommentPayload) event.getPayload();
                String type = payload.getIssue().getPullRequest().getDiffUrl() != null ? "pullrequest" : "issue";
                if ("pullrequest".equals(type)) {
                    context.openPullRequestActivity(getSherlockActivity(), repoOwner, repoName, payload.getIssue().getNumber());   
                }
                else {
                    context.openIssueActivity(getSherlockActivity(), repoOwner, repoName, payload.getIssue().getNumber(),
                            payload.getIssue().getState()); 
                }
            }
            else {
                context.notFoundMessage(getSherlockActivity(), R.plurals.repository);
            }
        }
        
        /** PullRequestReviewComment */
        else if (Event.TYPE_PULL_REQUEST_REVIEW_COMMENT.equals(eventType)) {
            PullRequestReviewCommentPayload payload = (PullRequestReviewCommentPayload) event.getPayload();
            context.openCommitInfoActivity(getSherlockActivity(), repoOwner, repoName, payload.getComment().getCommitId());
        }
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (v.getId() == R.id.list_view) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            Event event = (Event) mAdapter.getItem(info.position);
            
            //if payload is a base class, return void.  Think that it is an old event which not supported
            //by API v3.
            if (event.getPayload().getClass().getSimpleName().equals("EventPayload")) {
                return;
            }
            
            String eventType = event.getType();
            EventRepository eventRepo = event.getRepo();
            String[] repoNamePart = eventRepo.getName().split("/");
            String repoOwner = null;
            String repoName = null;
            if (repoNamePart.length == 2) {
                repoOwner = repoNamePart[0];
                repoName = repoNamePart[1];
            }
            String repoUrl = eventRepo.getUrl();
            
            menu.setHeaderTitle("Go to");

            /** Common menu */
            menu.add("User " + event.getActor().getLogin());
            if (repoOwner != null) {
                menu.add("Repo " + repoOwner + "/" + repoName);
            }

            /** PushEvent extra menu for commits */
            if (Event.TYPE_PUSH.equals(eventType)) {
                if (repoOwner != null) {
                    PushPayload payload = (PushPayload) event.getPayload();
                    menu.add("Compare " + payload.getHead());
                    
                    List<Commit> commits = payload.getCommits();
                    for (Commit commit : commits) {
                        menu.add("Commit " + commit.getSha());
                    }
                }
            }

            /** IssueEvent extra menu for commits */
            else if (Event.TYPE_ISSUES.equals(eventType)) {
                IssuesPayload payload = (IssuesPayload) event.getPayload();
                menu.add("Issue " + payload.getIssue().getNumber());
            }

            /** FollowEvent */
            else if (Event.TYPE_FOLLOW.equals(eventType)) {
                FollowPayload payload = (FollowPayload) event.getPayload();
                if (payload.getTarget() != null) {
                    menu.add("User " + payload.getTarget().getLogin());
                }
            }

            /** CommitCommentEvent */
            else if (Event.TYPE_COMMIT_COMMENT.equals(eventType)) {
                if (repoOwner != null) {
                    CommitCommentPayload payload = (CommitCommentPayload) event.getPayload();
                    menu.add("Commit " + payload.getComment().getCommitId().substring(0, 7));
                    //menu.add("Comment in browser");
                }
            }

            /** GistEvent */
            else if (Event.TYPE_GIST.equals(eventType)) {
                GistPayload payload = (GistPayload) event.getPayload();
                menu.add(payload.getGist().getId() + " in browser");
            }

            /** DownloadEvent */
            else if (Event.TYPE_DOWNLOAD.equals(eventType)) {
                DownloadPayload payload = (DownloadPayload) event.getPayload();
                menu.add("File " + payload.getDownload().getName() + " in browser");
            }

            /** ForkEvent */
            else if (Event.TYPE_FORK.equals(eventType)) {
                ForkPayload payload = (ForkPayload) event.getPayload();
                Repository forkee = payload.getForkee();
                if (forkee != null) {
                    menu.add("Forked repo " + forkee.getOwner().getLogin() + "/" + forkee.getName());
                }
            }

            /** GollumEvent */
            else if (Event.TYPE_GOLLUM.equals(eventType)) {
                menu.add("Wiki in browser");
            }
            
            /** PullRequestEvent */
            else if (Event.TYPE_PULL_REQUEST.equals(eventType)) {
                PullRequestPayload payload = (PullRequestPayload) event.getPayload();
                menu.add("Pull request " + payload.getNumber());
            }
            
            /** IssueCommentEvent */
            else if (Event.TYPE_ISSUE_COMMENT.equals(eventType)) {
                menu.add("Open issues");
            }
        }
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item
                .getMenuInfo();
        Event event = (Event) mAdapter.getItem(info.position);
        String eventType = event.getType();
        EventRepository eventRepo = event.getRepo();
        String[] repoNamePart = eventRepo.getName().split("/");
        String repoOwner = null;
        String repoName = null;
        if (repoNamePart.length == 2) {
            repoOwner = repoNamePart[0];
            repoName = repoNamePart[1];
        }
        String repoUrl = eventRepo.getUrl();
        
        String title = item.getTitle().toString();
        String value = title.split(" ")[1];

        Gh4Application context = ((BaseSherlockFragmentActivity) getActivity()).getApplicationContext();

        /** User item */
        if (title.startsWith("User")) {
            context
                    .openUserInfoActivity(getSherlockActivity(), value, event.getActor().getLogin());
        }
        /** Repo item */
        else if (title.startsWith("Repo")) {
            context.openRepositoryInfoActivity(getSherlockActivity(), repoOwner, repoName);
        }
        /** Commit item */
        else if (title.startsWith("Commit")) {
            if (repoOwner != null) {
                context.openCommitInfoActivity(getSherlockActivity(), repoOwner, repoName, value);
            }
            else {
                context.notFoundMessage(getSherlockActivity(), R.plurals.repository);
            }
        }
        /** Issue comment item */
        else if (title.startsWith("Open issues")) {
            context.openIssueListActivity(getSherlockActivity(), repoOwner, repoName, Constants.Issue.ISSUE_STATE_OPEN);
        }
        /** Issue item */
        else if (title.startsWith("Issue")) {
            IssuesPayload payload = (IssuesPayload) event.getPayload();
            context.openIssueActivity(getSherlockActivity(), repoOwner, repoName, payload.getIssue().getNumber());
        }
        /** Commit comment item */
        else if (title.startsWith("Comment in browser")) {
            CommitCommentPayload payload = (CommitCommentPayload) event.getPayload();
            context.openBrowser(getSherlockActivity(), payload.getComment().getUrl());
        }
        /** Gist item */
        else if (title.startsWith("gist")) {
            GistPayload payload = (GistPayload) event.getPayload();
            context.openBrowser(getSherlockActivity(), payload.getGist().getUrl());
        }
        /** Download item */
        else if (title.startsWith("File")) {
            if (repoOwner != null) {
                DownloadPayload payload = (DownloadPayload) event.getPayload();
                context.openBrowser(getSherlockActivity(), payload.getDownload().getHtmlUrl());
            }
            else {
                context.notFoundMessage(getSherlockActivity(), R.plurals.repository);
            }
        }
        /** Fork item */
        else if (title.startsWith("Forked repo")) {
            ForkPayload payload = (ForkPayload) event.getPayload();
            Repository forkee = payload.getForkee();
            if (forkee != null) {
                context.openRepositoryInfoActivity(getSherlockActivity(), forkee);
            }
            else {
                context.notFoundMessage(getSherlockActivity(), R.plurals.repository);
            }
        }
        /** Wiki item */
        else if (title.startsWith("Wiki in browser")) {
            GollumPayload payload = (GollumPayload) event.getPayload();
            List<GollumPage> pages = payload.getPages();
            if (pages != null && !pages.isEmpty()) {//TODO: now just open the first page
                context.openBrowser(getSherlockActivity(), pages.get(0).getHtmlUrl());                
            }
        }
        /** Pull Request item */
        else if (title.startsWith("Pull request")) {
            PullRequestPayload payload = (PullRequestPayload) event.getPayload();
            context.openPullRequestActivity(getSherlockActivity(), repoOwner, repoName, payload.getNumber());
        }
        
        else if (title.startsWith("Compare")) {
            if (repoOwner != null) {
                PushPayload payload = (PushPayload) event.getPayload();
                
                List<Commit> commits = payload.getCommits();
                Intent intent = new Intent().setClass(context, CompareActivity.class);
                for (Commit commit : commits) {
                    intent.putExtra("sha" + commit.getSha(), commit.getSha());
                }
                
                intent.putExtra(Constants.Repository.REPO_OWNER, repoOwner);
                intent.putExtra(Constants.Repository.REPO_NAME, repoName);
                intent.putExtra(Constants.Repository.REPO_URL, event.getRepo().getUrl());
                startActivity(intent);
            }
        }

        return true;
    }
}