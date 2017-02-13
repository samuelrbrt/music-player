package com.zionbhavan.musicplayer.adapter;

import android.content.res.Resources;
import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.zionbhavan.musicplayer.AlbumModel;
import com.zionbhavan.musicplayer.R;

import java.util.ArrayList;

/**
 * @author Samuel Robert <samuelrbrt16@gmail.com>
 * @created on 2017-02-12 at 11:33 AM
 * @signed_off_by Samuel Robert <samuelrbrt@gmail.com>
 */
public class AlbumAdapter extends RecyclerView.Adapter<AlbumAdapter.ViewHolder> {
	private ArrayList<AlbumModel> mAlbumList;
	private Resources mResources;

	public AlbumAdapter() {
		mAlbumList = new ArrayList<>();
	}

	@Override
	public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		mResources = parent.getContext().getResources();

		View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_album, parent, false);
		return new ViewHolder(itemView);
	}

	@Override
	public void onBindViewHolder(ViewHolder holder, int position) {
		AlbumModel album = mAlbumList.get(position);

		holder.albumArtIV.setImageURI(album.getAlbumArt());
		holder.titleTV.setText(album.getTitle());
		holder.artistTV.setText(album.getArtist());
	}

	@Override
	public int getItemCount() {
		return mAlbumList.size();
	}

	public void setAlbumList(ArrayList<AlbumModel> albumList) {
		mAlbumList = albumList;
		notifyDataSetChanged();
	}

	class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
		ImageView albumArtIV;
		TextView titleTV, artistTV;
		ImageView menuIB;

		ViewHolder(View itemView) {
			super(itemView);

			albumArtIV = (ImageView) itemView.findViewById(R.id.item_iv_album_art);
			titleTV = (TextView) itemView.findViewById(R.id.item_tv_title);
			artistTV = (TextView) itemView.findViewById(R.id.item_tv_artist);
			menuIB = (ImageView) itemView.findViewById(R.id.item_ib_menu);

			itemView.setOnClickListener(this);
			menuIB.setOnClickListener(this);
		}

		@Override
		public void onClick(View v) {
			switch (v.getId()) {
				case R.id.item_ib_menu:
					//TODO: item menu implementation
					break;

				default:
					//TODO: open album songs
					break;
			}
		}
	}
}
