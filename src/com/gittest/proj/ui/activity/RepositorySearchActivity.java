package com.gittest.proj.ui.activity;

import static android.app.SearchManager.QUERY;
import static android.content.Intent.ACTION_SEARCH;
import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP;
import static android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP;
import android.content.Intent;
import android.os.Bundle;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.gittest.proj.R.id;
import com.gittest.proj.R.layout;
import com.gittest.proj.R.menu;
import com.gittest.proj.R.string;
import com.github.mobile.ui.repo.RepositorySearchSuggestionsProvider;
import com.github.mobile.util.ToastUtils;
import com.github.rtyley.android.sherlock.roboguice.activity.RoboSherlockFragmentActivity;
import com.gittest.proj.ui.activity.HomeActivity;
import com.gittest.proj.ui.fragment.SearchRepositoryListFragment;

public class RepositorySearchActivity extends RoboSherlockFragmentActivity {

    private SearchRepositoryListFragment repoFragment;
    private String LANGUAGE = " ruby/rails";

    @Override
    public boolean onCreateOptionsMenu(Menu options) {
        getSupportMenuInflater().inflate(menu.search, options);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case id.m_search:
            onSearchRequested();
            return true;
        case id.m_clear:
            RepositorySearchSuggestionsProvider.clear(this);
            ToastUtils.show(this, string.search_history_cleared);
            return true;
        case android.R.id.home:
            Intent intent = new Intent(this, HomeActivity.class);
            intent.addFlags(FLAG_ACTIVITY_CLEAR_TOP | FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(layout.repo_search);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setSubtitle(string.repositories);
        actionBar.setDisplayHomeAsUpEnabled(true);

        repoFragment = (SearchRepositoryListFragment) getSupportFragmentManager()
                .findFragmentById(android.R.id.list);

        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        repoFragment.setListShown(false);
        handleIntent(intent);
        repoFragment.refresh();
    }

    private void handleIntent(Intent intent) {
        if (ACTION_SEARCH.equals(intent.getAction()))
            search(intent.getStringExtra(QUERY));
    }

    private void search(final String query) {
        getSupportActionBar().setTitle(query);
        RepositorySearchSuggestionsProvider.save(this, query);
        repoFragment.setQuery(query  + LANGUAGE);
    }
}
