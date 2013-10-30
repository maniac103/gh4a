package com.gh4a.loader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.egit.github.core.Contributor;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.service.RepositoryService;

import android.content.Context;

import com.gh4a.Gh4Application;

public class ContributorListLoader extends BaseLoader<List<User>> {

    private String mRepoOwner;
    private String mRepoName;
    
    public ContributorListLoader(Context context, String repoOwner, String repoName) {
        super(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
    }
    
    @Override
    public List<User> doLoadInBackground() throws IOException {
        RepositoryService repoService = (RepositoryService)
                Gh4Application.get(getContext()).getService(Gh4Application.REPO_SERVICE);
        List<Contributor> contributors = repoService.getContributors(new RepositoryId(mRepoOwner, mRepoName), true);
        
        List<User> users = new ArrayList<User>();
        for (Contributor contributor : contributors) {
            User user = new User();
            user.setName(contributor.getName());
            user.setLogin(contributor.getLogin());
            user.setAvatarUrl(contributor.getAvatarUrl());
            users.add(user);
        }

        return users;
    }
}