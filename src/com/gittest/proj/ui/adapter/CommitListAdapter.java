package com.gittest.proj.ui.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;

import com.github.kevinsawicki.wishlist.SingleTypeAdapter;
import com.gittest.proj.R.id;
import com.github.mobile.core.commit.CommitUtils;
import com.github.mobile.ui.StyledText;
import com.github.mobile.util.AvatarLoader;

import java.util.Collection;

import org.eclipse.egit.github.core.RepositoryCommit;

public class CommitListAdapter extends SingleTypeAdapter<RepositoryCommit> {

    private final AvatarLoader avatars;

    public CommitListAdapter(int viewId, LayoutInflater inflater,
            Collection<RepositoryCommit> elements, AvatarLoader avatars) {
        super(inflater, viewId);

        this.avatars = avatars;
        setItems(elements);
    }

    @Override
    public long getItemId(int position) {
        String sha = getItem(position).getSha();
        if (!TextUtils.isEmpty(sha))
            return sha.hashCode();
        else
            return super.getItemId(position);
    }

    @Override
    protected int[] getChildViewIds() {
        return new int[] { id.tv_commit_id, id.tv_commit_author, id.iv_avatar,
                id.tv_commit_message, id.tv_commit_comments };
    }

    @Override
    protected View initialize(View view) {
        view = super.initialize(view);
        return view;
    }

    @Override
    protected void update(int position, RepositoryCommit item) {
        setText(0, CommitUtils.abbreviate(item.getSha()));

        StyledText authorText = new StyledText();
        authorText.bold(CommitUtils.getAuthor(item));
        authorText.append(' ');
        authorText.append(CommitUtils.getAuthorDate(item));
        setText(1, authorText);

        CommitUtils.bindAuthor(item, avatars, imageView(2));
        setText(3, item.getCommit().getMessage());
        setText(4, CommitUtils.getCommentCount(item));
    }
}
