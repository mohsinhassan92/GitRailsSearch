package com.gittest.proj.ui.fragment;

import static android.app.Activity.RESULT_OK;
import static com.github.mobile.Intents.EXTRA_REPOSITORY;
import static com.github.mobile.RequestCodes.COMMIT_VIEW;
import static com.github.mobile.RequestCodes.REF_UPDATE;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.kevinsawicki.wishlist.SingleTypeAdapter;
import com.github.kevinsawicki.wishlist.ViewUtils;
import com.github.mobile.ThrowableLoader;
import com.gittest.proj.R.id;
import com.gittest.proj.R.layout;
import com.gittest.proj.R.string;
import com.github.mobile.core.ResourcePager;
import com.github.mobile.core.commit.CommitPager;
import com.github.mobile.core.commit.CommitStore;
import com.github.mobile.core.ref.RefUtils;
import com.github.mobile.ui.DialogFragmentActivity;
import com.github.mobile.ui.DialogResultListener;
import com.github.mobile.ui.ItemListFragment;
import com.github.mobile.ui.PagedItemFragment;
import com.github.mobile.ui.ref.RefDialog;
import com.github.mobile.ui.ref.RefDialogFragment;
import com.github.mobile.util.AvatarLoader;
import com.github.mobile.util.TypefaceUtils;
import com.gittest.proj.ui.adapter.CommitListAdapter;
import com.google.inject.Inject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.egit.github.core.Reference;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.client.PageIterator;
import org.eclipse.egit.github.core.service.CommitService;
import org.eclipse.egit.github.core.service.DataService;
import org.eclipse.egit.github.core.service.RepositoryService;

public class RepositoryCodeFragment extends PagedItemFragment<RepositoryCommit>
implements DialogResultListener {

	@Inject
	protected AvatarLoader avatars;

	@Inject
	private CommitService service;

	@Inject
	private CommitStore store;

	private Repository repository;

	private RefDialog dialog;

	private TextView branchIconView;

	private TextView branchView;

	private View branchFooterView;

	@Inject
	private DataService dataService;

	@Inject
	private RepositoryService repoService;

	private final int MAX_ITEMS_TO_SHOW = 25;

	private List<RepositoryCommit> mCommitsList;

	private String ref;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		repository = getSerializableExtra(EXTRA_REPOSITORY);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setEmptyText(string.no_commits);
		
		mCommitsList = new ArrayList<RepositoryCommit>();
		populateUniqueList(items);
		Collections.sort(mCommitsList, Collections.reverseOrder());
	}

	private void populateUniqueList(List<RepositoryCommit> items) {
		for (RepositoryCommit rc : items) {
			if (/*rc.getAuthor().getName() != null &&*/ !contains(rc)) {
				mCommitsList.add(rc);
			}
		}
	}

	private boolean contains(RepositoryCommit repoItem) {
		
		for (RepositoryCommit rc : mCommitsList) {
			if (rc != null && repoItem != null)
				if (rc.getAuthor().getId() == repoItem.getAuthor().getId()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Loader<List<RepositoryCommit>> onCreateLoader(int id, Bundle bundle) {
		final ThrowableLoader<List<RepositoryCommit>> parentLoader = (ThrowableLoader<List<RepositoryCommit>>) super
				.onCreateLoader(id, bundle);
		
		mCommitsList = new ArrayList<RepositoryCommit>();
		populateUniqueList(items);
		Collections.sort(mCommitsList, Collections.reverseOrder());

		if (mCommitsList.size() > MAX_ITEMS_TO_SHOW) {
			mCommitsList = mCommitsList.subList(0, MAX_ITEMS_TO_SHOW - 1);
		}

		return new ThrowableLoader<List<RepositoryCommit>>(getActivity(), mCommitsList) {
			@Override
			public List<RepositoryCommit> loadData() throws Exception {
				/*/
				if (TextUtils.isEmpty(ref)) {
					String defaultBranch = repository.getMasterBranch();
					if (TextUtils.isEmpty(defaultBranch)) {
						defaultBranch = repoService.getRepository(repository)
								.getMasterBranch();
						if (TextUtils.isEmpty(defaultBranch))
							defaultBranch = "master";
					}
					ref = defaultBranch;
				}
				//*/
				return parentLoader.loadData();
			}
		};
	}

	public void onLoadFinished(Loader<List<RepositoryCommit>> loader,
			List<RepositoryCommit> items) {

		populateUniqueList(items);
		Collections.sort(mCommitsList, Collections.reverseOrder());
		if (mCommitsList.size() > MAX_ITEMS_TO_SHOW) {
			mCommitsList = mCommitsList.subList(0, MAX_ITEMS_TO_SHOW - 1);
		}

		//super.onLoadFinished(loader, items);
		super.onLoadFinished(loader, mCommitsList);

		if (ref != null)
			updateRefLabel();
	}

	@Override
	protected ResourcePager<RepositoryCommit> createPager() {
		return new CommitPager(repository, store) {

			private String last;

			@Override
			protected RepositoryCommit register(RepositoryCommit resource) {
				// Store first parent of last commit registered for next page
				// lookup
				//List<Commit> parents = resource.getParents();
				//if (parents != null && !parents.isEmpty())
				//    last = parents.get(0).getSha();
				//else
				last = null;

				return super.register(resource);
			}

			@Override
			public PageIterator<RepositoryCommit> createIterator(int page,
					int size) {
				if (page > 1 || ref == null)
					return service.pageCommits(repository, last, null, size);
				else
					return service.pageCommits(repository, ref, null, size);
			}

			@Override
			public ResourcePager<RepositoryCommit> clear() {
				last = null;
				return super.clear();
			}
		};
	}

	@Override
	protected int getLoadingMessage() {
		return string.max_commits;
	}

	@Override
	protected int getErrorMessage(Exception exception) {
		return string.error_commits_load;
	}

	@Override
	protected SingleTypeAdapter<RepositoryCommit> createAdapter(
			List<RepositoryCommit> items) {
		Collections.sort(items, Collections.reverseOrder());
		if (items.size() > MAX_ITEMS_TO_SHOW) {
			items = items.subList(0, MAX_ITEMS_TO_SHOW - 1);
		}
		/*
		return new CommitListAdapter(layout.commit_item, getActivity()
				.getLayoutInflater(), items, avatars);
		//*/
		

		mCommitsList = new ArrayList<RepositoryCommit>();
		populateUniqueList(items);
		Collections.sort(mCommitsList, Collections.reverseOrder());
		
		return new CommitListAdapter(layout.commit_item, getActivity()
				.getLayoutInflater(), mCommitsList, avatars);
	}

	/*
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		Object item = l.getItemAtPosition(position);

		List<RepositoryCommit> authorCommitsList = new ArrayList<RepositoryCommit>();
		String name = items.get(position).getAuthor().getName();

		for (RepositoryCommit cc : items) {
			if (cc.getAuthor().getName().equals(name)) {
				authorCommitsList.add(cc);
			}
		}

		if (item instanceof RepositoryCommit)
			startActivityForResult(CommitViewActivity.createIntent(repository,
					position, items), COMMIT_VIEW);
	}
	//*/

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == COMMIT_VIEW) {
			notifyDataSetChanged();
			return;
		}

		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onDialogResult(int requestCode, int resultCode, Bundle arguments) {
		if (RESULT_OK != resultCode)
			return;

		switch (requestCode) {
		case REF_UPDATE:
			setRef(RefDialogFragment.getSelected(arguments));
			break;
		}
	}

	private void updateRefLabel() {
		branchView.setText(RefUtils.getName(ref));
		if (RefUtils.isTag(ref))
			branchIconView.setText(string.icon_tag);
		else
			branchIconView.setText(string.icon_fork);
	}

	private void setRef(final Reference ref) {
		this.ref = ref.getRef();
		updateRefLabel();
		refreshWithProgress();
	}

	private void switchRefs() {
		if (ref == null)
			return;

		if (dialog == null)
			dialog = new RefDialog((DialogFragmentActivity) getActivity(),
					REF_UPDATE, repository, dataService);
		dialog.show(new Reference().setRef(ref));
	}

	@Override
	public ItemListFragment<RepositoryCommit> setListShown(boolean shown,
			boolean animate) {
		ViewUtils.setGone(branchFooterView, !shown);
		return super.setListShown(shown, animate);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		branchFooterView = finder.find(id.rl_branch);
		branchView = finder.find(id.tv_branch);
		branchIconView = finder.find(id.tv_branch_icon);
		TypefaceUtils.setOcticons(branchIconView);
		branchFooterView.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				switchRefs();
			}
		});
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(layout.commit_list, null);
	}
}
