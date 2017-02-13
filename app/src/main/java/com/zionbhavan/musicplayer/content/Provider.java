package com.zionbhavan.musicplayer.content;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio.Albums;
import android.provider.MediaStore.Audio.Media;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.webkit.MimeTypeMap;

import com.zionbhavan.musicplayer.AlbumModel;

import java.util.ArrayList;

/**
 * @author Samuel Robert <samuelrbrt16@gmail.com>
 * @created on 2017-02-13 at 11:42 AM
 * @signed_off_by Samuel Robert <samuelrbrt16@gmail.com>
 * <p>
 * Content reader class. Add the content provider query method in this class
 */

public class Provider {
	private ContentResolver mResolver;

	public Provider(Context context) {
		mResolver = context.getContentResolver();
	}

	public void buildLibrary(ArrayList<MediaBrowserCompat.MediaItem> library) {
		Uri uri = Media.EXTERNAL_CONTENT_URI;
		String[] PROJECTION = new String[]{Media._ID, Media.TITLE, Media.ALBUM, Media.ARTIST};
		String SELECTION = MediaStore.Files.FileColumns.MIME_TYPE + "=? AND " + MediaStore.Audio.Media.IS_MUSIC + "=1";
		String[] SELECTION_ARGS = new String[]{
		    MimeTypeMap.getSingleton().getMimeTypeFromExtension("mp3")
		};

		try (Cursor cursor = mResolver.query(uri, PROJECTION, SELECTION, SELECTION_ARGS, null)) {
			while (cursor != null && cursor.moveToNext()) {
				long id = cursor.getLong(0);
				library.add(
				    new MediaBrowserCompat.MediaItem(
					new MediaDescriptionCompat.Builder()
					    .setMediaId(String.valueOf(id))
					    .setTitle(cursor.getString(1))
					    .setSubtitle(cursor.getString(2))
					    .setDescription(cursor.getString(3))
					    .setMediaUri(
					        ContentUris.withAppendedId(Media.EXTERNAL_CONTENT_URI, id)
					    )
					    .build(),
					MediaBrowserCompat.MediaItem.FLAG_BROWSABLE |
					    MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
				    )
				);
			}
		}
	}

	public void getAllAlBum(ArrayList<AlbumModel> albumList) {
		String[] projection = new String[]{Albums._ID, Albums.ALBUM, Albums.ARTIST, Albums.ALBUM_ART, Albums
		    .NUMBER_OF_SONGS};
		String sortOrder = Media.ALBUM + " ASC";

		try (Cursor cursor = mResolver.query(Albums.EXTERNAL_CONTENT_URI, projection, null, null, sortOrder)) {
			while (cursor != null && cursor.moveToNext()) {
				albumList.add(new AlbumModel(
				    cursor.getLong(0), cursor.getString(1), cursor.getString(2),
				    cursor.getString(3),
				    cursor.getInt(4)
				));
			}
		}
	}
}
