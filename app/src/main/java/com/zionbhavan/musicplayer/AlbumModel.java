package com.zionbhavan.musicplayer;

import android.net.Uri;

/**
 * @author Samuel Robert <samuelrbrt16@gmail.com>
 * @created on 2017-02-13 at 3:54 PM
 * @signed_off_by Samuel Robert <samuelrbrt@gmail.com>
 */

public class AlbumModel {
	private final long id;
	private final String title;
	private final String artist;
	private final Uri albumArt;
	private final int noOfSongs;

	public AlbumModel(long id, String title, String artist, String albumArt, int noOfSongs) {
		this.id = id;
		this.title = title;
		this.artist = artist;
		this.noOfSongs = noOfSongs;
		this.albumArt = albumArt == null ? null: Uri.parse(albumArt);
	}

	public long getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public String getArtist() {
		return artist;
	}

	public Uri getAlbumArt() {
		return albumArt;
	}

	public int getNoOfSongs() {
		return noOfSongs;
	}
}
