package com.zionbhavan.musicplayer.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.zionbhavan.musicplayer.R;
import com.zionbhavan.musicplayer.adapter.AlbumAdapter;

import java.util.ArrayList;

/**
 * @author Samuel Robert <samuelrbrt16@gmail.com>
 * @created on 2017-02-12 at 11:24 AM
 * @signed_off_by Samuel Robert <samuelrbrt16@gmail.com>
 */

public class AlbumFragment extends MusicFragment {
	private static final String ARG_ALBUM_LIST = "arg_album_list";

	private ArrayList<MediaItem> mAlbumList;

	private RecyclerView mAlbumRV;
	private AlbumAdapter mAdapter;

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_album, container, false);

		//mAlbumList = getArguments().getParcelableArrayList(ARG_ALBUM_LIST);
		mAdapter = new AlbumAdapter();

		mAlbumRV = (RecyclerView) rootView.findViewById(R.id.rv_album);
		mAlbumRV.setLayoutManager(new GridLayoutManager(getContext(), 3));
		mAlbumRV.setAdapter(mAdapter);

		return rootView;
	}

	@Override
	protected void buildTransportControls() {
		super.buildTransportControls();
	}
}
