package com.gittest.proj.ui.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.View;
import android.widget.ListView;

import com.github.kevinsawicki.wishlist.SingleTypeAdapter;
import com.gittest.proj.R.string;
import com.github.mobile.ThrowableLoader;
import com.github.mobile.core.repo.RefreshRepositoryTask;
import com.github.mobile.ui.ItemListFragment;
import com.github.mobile.ui.repo.SearchRepositoryListAdapter;
import com.gittest.proj.ui.activity.RepositoryViewActivity;
import com.google.inject.Inject;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;

import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.SearchRepository;
import org.eclipse.egit.github.core.service.RepositoryService;

public class SearchRepositoryListFragment extends
        ItemListFragment<SearchRepository> {

    @Inject
    private RepositoryService service;

    private String query;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setEmptyText(string.no_repositories);
    }

    public SearchRepositoryListFragment setQuery(final String query) {
        this.query = query;
        return this;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        final SearchRepository result = (SearchRepository) l
                .getItemAtPosition(position);
        new RefreshRepositoryTask(getActivity(), result) {

            @Override
            public void execute() {
                showIndeterminate(MessageFormat.format(
                        getString(string.opening_repository),
                        result.generateId()));

                super.execute();
            }

            @Override
            protected void onSuccess(Repository repository) throws Exception {
                super.onSuccess(repository);

                startActivity(RepositoryViewActivity.createIntent(repository));
            }
        }.execute();
    }

    /**
     * Check if the search query is an exact repository name/owner match and
     * open the repository activity and finish the current activity when it is
     *
     * @param query
     * @return true if query opened as repository, false otherwise
     */
    private boolean openRepositoryMatch(final String query) {
        if (TextUtils.isEmpty(query))
            return false;

        RepositoryId repoId = RepositoryId.createFromId(query.trim());
        if (repoId == null)
            return false;

        Repository repo;
        try {
            repo = service.getRepository(repoId);
        } catch (IOException e) {
            return false;
        }

        startActivity(RepositoryViewActivity.createIntent(repo));
        final Activity activity = getActivity();
        if (activity != null)
            activity.finish();
        return true;
    }

    @Override
    public Loader<List<SearchRepository>> onCreateLoader(int id, Bundle args) {
        return new ThrowableLoader<List<SearchRepository>>(getActivity(), items) {

            @Override
            public List<SearchRepository> loadData() throws Exception {
                if (openRepositoryMatch(query))
                    return Collections.emptyList();
                else
                    return service.searchRepositories(query);
            }
        };
    }

    @Override
    protected int getErrorMessage(Exception exception) {
        return string.error_repos_load;
    }

    @Override
    protected SingleTypeAdapter<SearchRepository> createAdapter(
            List<SearchRepository> items) {
        return new SearchRepositoryListAdapter(getActivity()
                .getLayoutInflater(), items.toArray(new SearchRepository[items
                .size()]));
    }
}
