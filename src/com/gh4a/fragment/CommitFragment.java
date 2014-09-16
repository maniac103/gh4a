package com.gh4a.fragment;

import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.CommitFile;
import org.eclipse.egit.github.core.RepositoryCommit;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.devspark.progressfragment.SherlockProgressFragment;
import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.activities.CommitDiffViewerActivity;
import com.gh4a.loader.CommitCommentListLoader;
import com.gh4a.loader.CommitLoader;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.utils.CommitUtils;
import com.gh4a.utils.GravatarHandler;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.StringUtils;
import com.gh4a.utils.UiUtils;

import java.util.ArrayList;
import java.util.List;

public class CommitFragment extends SherlockProgressFragment implements OnClickListener {
    private String mRepoOwner;
    private String mRepoName;
    private String mObjectSha;
    private RepositoryCommit mCommit;
    private List<CommitComment> mComments;
    protected View mContentView;

    private LoaderCallbacks<RepositoryCommit> mCommitCallback = new LoaderCallbacks<RepositoryCommit>() {
        @Override
        public Loader<LoaderResult<RepositoryCommit>> onCreateLoader(int id, Bundle args) {
            return new CommitLoader(getActivity(), mRepoOwner, mRepoName, mObjectSha);
        }

        @Override
        public void onResultReady(LoaderResult<RepositoryCommit> result) {
            if (result.handleError(getActivity())) {
                setContentEmpty(true);
                setContentShown(true);
                return;
            }
            mCommit = result.getData();
            fillDataIfReady();
        }
    };
    private LoaderCallbacks<List<CommitComment>> mCommentCallback =
            new LoaderCallbacks<List<CommitComment>>() {
        @Override
        public Loader<LoaderResult<List<CommitComment>>> onCreateLoader(int id, Bundle args) {
            return new CommitCommentListLoader(getActivity(), mRepoOwner, mRepoName,
                    mObjectSha, false, true);
        }

        @Override
        public void onResultReady(LoaderResult<List<CommitComment>> result) {
            if (result.handleError(getActivity())) {
                setContentEmpty(true);
                setContentShown(true);
                return;
            }
            mComments = result.getData();
            fillDataIfReady();
        }
    };

    public static CommitFragment newInstance(String repoOwner, String repoName, String objectSha) {
        CommitFragment f = new CommitFragment();

        Bundle args = new Bundle();
        args.putString(Constants.Repository.OWNER, repoOwner);
        args.putString(Constants.Repository.NAME, repoName);
        args.putString(Constants.Object.OBJECT_SHA, objectSha);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRepoOwner = getArguments().getString(Constants.Repository.OWNER);
        mRepoName = getArguments().getString(Constants.Repository.NAME);
        mObjectSha = getArguments().getString(Constants.Object.OBJECT_SHA);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mContentView = inflater.inflate(R.layout.commit, null);

        Gh4Application app = Gh4Application.get(getActivity());
        UiUtils.assignTypeface(mContentView, app.boldCondensed, new int[] {
                R.id.commit_added, R.id.commit_changed,
                R.id.commit_renamed, R.id.commit_deleted
        });

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setContentView(mContentView);
        setContentShown(false);

        initLoader();
    }

    protected void initLoader() {
        getLoaderManager().initLoader(0, null, mCommitCallback);
        getLoaderManager().initLoader(1, null, mCommentCallback);
    }

    private void fillDataIfReady() {
        if (mCommit != null && mComments != null) {
            fillHeader();
            fillStats(mCommit.getFiles(), mComments);
            setContentShown(true);
        }
    }

    private void fillHeader() {
        final Activity activity = getActivity();
        final Gh4Application app = Gh4Application.get(activity);

        ImageView ivGravatar = (ImageView) mContentView.findViewById(R.id.iv_gravatar);
        GravatarHandler.assignGravatar(ivGravatar, mCommit.getAuthor());

        String login = CommitUtils.getAuthorLogin(mCommit);
        if (login != null) {
            ivGravatar.setOnClickListener(this);
            ivGravatar.setTag(login);
        }

        TextView tvMessage = (TextView) mContentView.findViewById(R.id.tv_message);
        TextView tvTitle = (TextView) mContentView.findViewById(R.id.tv_title);

        String message = mCommit.getCommit().getMessage();
        int pos = message.indexOf('\n');
        String title = pos > 0 ? message.substring(0, pos) : message;
        int length = message.length();
        while (pos > 0 && pos < length && Character.isWhitespace(message.charAt(pos))) {
            pos++;
        }
        message = pos > 0 && pos < length ? message.substring(pos) : null;

        tvTitle.setText(title);
        if (message != null) {
            tvMessage.setText(message);
        } else {
            tvMessage.setVisibility(View.GONE);
        }

        TextView tvExtra = (TextView) mContentView.findViewById(R.id.tv_extra);
        tvExtra.setText(CommitUtils.getAuthorName(app, mCommit) + " "
                + StringUtils.formatRelativeTime(activity, mCommit.getCommit().getAuthor().getDate(), true));

        if (!CommitUtils.authorEqualsCommitter(mCommit)) {
            ViewGroup committer = (ViewGroup) mContentView.findViewById(R.id.committer_info);
            ImageView gravatar = (ImageView) committer.findViewById(R.id.iv_commit_gravatar);
            TextView extra = (TextView) committer.findViewById(R.id.tv_commit_extra);

            committer.setVisibility(View.VISIBLE);
            GravatarHandler.assignGravatar(gravatar, mCommit.getCommitter());
            extra.setText(getString(R.string.commit_details, CommitUtils.getCommitterName(app, mCommit),
                    StringUtils.formatRelativeTime(activity, mCommit.getCommit().getCommitter().getDate(), true)));
        }
    }

    protected void fillStats(List<CommitFile> files, List<CommitComment> comments) {
        LinearLayout llChanged = (LinearLayout) mContentView.findViewById(R.id.ll_changed);
        LinearLayout llAdded = (LinearLayout) mContentView.findViewById(R.id.ll_added);
        LinearLayout llRenamed = (LinearLayout) mContentView.findViewById(R.id.ll_renamed);
        LinearLayout llDeleted = (LinearLayout) mContentView.findViewById(R.id.ll_deleted);
        final LayoutInflater inflater = getLayoutInflater(null);
        int added = 0, changed = 0, renamed = 0, deleted = 0;
        int additions = 0, deletions = 0;
        int count = files != null ? files.size() : 0;

        for (int i = 0; i < count; i++) {
            CommitFile file = files.get(i);
            String status = file.getStatus();
            final LinearLayout parent;

            if ("added".equals(status)) {
                parent = llAdded;
                added++;
            } else if ("modified".equals(status)) {
                parent = llChanged;
                changed++;
            } else if ("renamed".equals(status)) {
                parent = llRenamed;
                renamed++;
            } else if ("removed".equals(status)) {
                parent = llDeleted;
                deleted++;
            } else {
                continue;
            }

            additions += file.getAdditions();
            deletions += file.getDeletions();

            int commentCount = 0;
            for (CommitComment comment : comments) {
                if (TextUtils.equals(file.getFilename(), comment.getPath())) {
                    commentCount++;
                }
            }

            ViewGroup fileView = (ViewGroup) inflater.inflate(R.layout.commit_filename, parent, false);
            TextView fileNameView = (TextView) fileView.findViewById(R.id.filename);
            fileNameView.setText(file.getFilename());
            fileNameView.setTag(file);

            if (parent != llDeleted && file.getPatch() != null) {
                fileNameView.setTextColor(getResources().getColor(R.color.highlight));
                fileNameView.setOnClickListener(this);
            }
            if (commentCount > 0) {
                TextView commentView = (TextView) fileView.findViewById(R.id.comments);
                commentView.setText(String.valueOf(commentCount));
                commentView.setVisibility(View.VISIBLE);
            }

            parent.addView(fileView);
        }

        llAdded.setVisibility(added > 0 ? View.VISIBLE : View.GONE);
        llChanged.setVisibility(changed > 0 ? View.VISIBLE : View.GONE);
        llRenamed.setVisibility(renamed > 0 ? View.VISIBLE : View.GONE);
        llDeleted.setVisibility(deleted > 0 ? View.VISIBLE : View.GONE);

        TextView tvSummary = (TextView) mContentView.findViewById(R.id.tv_desc);
        tvSummary.setText(getString(R.string.commit_summary, added + changed + renamed + deleted,
                additions, deletions));
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.iv_gravatar) {
            String login = (String) v.getTag();
            IntentUtils.openUserInfoActivity(getActivity(), login);
        } else {
            CommitFile file = (CommitFile) v.getTag();

            Intent intent = new Intent(getActivity(), CommitDiffViewerActivity.class);
            intent.putExtra(Constants.Repository.OWNER, mRepoOwner);
            intent.putExtra(Constants.Repository.NAME, mRepoName);
            intent.putExtra(Constants.Object.REF, mObjectSha);
            intent.putExtra(Constants.Object.OBJECT_SHA, mObjectSha);
            intent.putExtra(Constants.Commit.DIFF, file.getPatch());
            intent.putExtra(Constants.Commit.COMMENTS, new ArrayList<CommitComment>(mComments));
            intent.putExtra(Constants.Object.PATH, file.getFilename());
            intent.putExtra(Constants.Object.TREE_SHA, mCommit.getCommit().getTree().getSha());
            startActivity(intent);
        }
    }
}