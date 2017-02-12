package com.zionbhavan.musicplayer.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.zionbhavan.musicplayer.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Samuel Robert <samuelrbrt16@gmail.com>
 * @created on 2017-01-11 at 6:23 PM
 * @signed_off_by Samuel Robert <samuelrbrt16@gmail.com>
 */

public class MediaService extends MediaBrowserServiceCompat implements AudioManager.OnAudioFocusChangeListener {
	private static final String TAG = "MediaService";
	private static final String MEDIA_ROOT_ID = "media_root_id";
	private static final int NOTIFICATION_ID = 158;

	private MediaSessionCompat mMediaSession;
	private MediaPlayer mPlayer;
	private BecomingNoisyReceiver myNoisyAudioStreamReceiver = new BecomingNoisyReceiver();
	private IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
	private MediaSessionCallback mSessionCallback;
	private MediaPlayerListeners mMediaListener;

	private ArrayList<MediaItem> mAlbums = new ArrayList<>();
	private ArrayList<MediaItem> mLibrary = new ArrayList<>();

	public MediaService() {
	}

	@Override
	public void onCreate() {
		super.onCreate();

		// Create a MediaSessionCompat
		mMediaSession = new MediaSessionCompat(getApplicationContext(), TAG);

		// Enable callbacks from MediaButtons and TransportControls
		mMediaSession.setFlags(
		    MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
			MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

		// Set an initial PlaybackState with ACTION_PLAY, so media buttons can start the player
		PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder()
		    .setActions(PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PLAY_PAUSE);
		mMediaSession.setPlaybackState(stateBuilder.build());

		// MediaSessionCallback has methods that handle callbacks from UI
		mSessionCallback = new MediaSessionCallback();
		mMediaSession.setCallback(mSessionCallback);

		// For Android 5.0 (API version 21) or greater
		// To enable restarting an inactive session in the background,
		// You must create a pending intent and setMediaButtonReceiver.
		Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
		mediaButtonIntent.setClass(getApplicationContext(), MediaService.class);
		PendingIntent mbrIntent = PendingIntent.getService(getApplicationContext(), 0, mediaButtonIntent, 0);
		mMediaSession.setMediaButtonReceiver(mbrIntent);

		// Set the session's token so that client activities can communicate with it.
		setSessionToken(mMediaSession.getSessionToken());

		// Media player
		mPlayer = new MediaPlayer();
		mMediaListener = new MediaPlayerListeners();

		// Build Library
		Thread thread = new Thread() {
			@Override
			public void run() {
				super.run();
				buildLibrary();
			}
		};
		thread.start();
	}

	@Nullable
	@Override
	public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
		if (allowBrowsing(clientPackageName)) {
			return new BrowserRoot(MEDIA_ROOT_ID, null);
		} else {
			return null;
		}
	}

	private boolean allowBrowsing(String clientPackageName) {
		return getPackageName().equals(clientPackageName);
	}

	@Override
	public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
		result.sendResult(mLibrary);
	}

	private void buildLibrary() {
		ContentResolver contentResolver = getContentResolver();
		Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
		String[] PROJECTION = new String[]{
		    MediaStore.Audio.Media._ID,
		    MediaStore.Audio.Media.TITLE,
		    MediaStore.Audio.Media.ALBUM,
		    MediaStore.Audio.Media.ARTIST
		};
		String SELECTION = MediaStore.Files.FileColumns.MIME_TYPE + "=?";
		String[] SELECTION_ARGS = new String[]{
		    MimeTypeMap.getSingleton().getMimeTypeFromExtension("mp3")
		};

		try (Cursor cursor = contentResolver.query(uri, PROJECTION, SELECTION, SELECTION_ARGS, null)) {
			while (cursor != null && cursor.moveToNext()) {
				long id = cursor.getLong(0);
				uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
				mLibrary.add(
				    new MediaBrowserCompat.MediaItem(
					new MediaDescriptionCompat.Builder()
					    .setMediaId(String.valueOf(id))
					    .setTitle(cursor.getString(1))
					    .setSubtitle(cursor.getString(2))
					    .setDescription(cursor.getString(3))
					    .setMediaUri(
						uri
					    )
					    .build(),
					MediaBrowserCompat.MediaItem.FLAG_BROWSABLE |
					    MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
				    )
				);
			}
		}
	}

	private Notification getNotification() {
		// Given a media session and its context (usually the component containing the session)
		// Create a NotificationCompat.Builder

		// Get the session's metadata
		MediaControllerCompat controller = mMediaSession.getController();
		MediaMetadataCompat mediaMetadata = controller.getMetadata();
		//MediaDescriptionCompat description = mediaMetadata.getDescription();

		NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext());

		builder
		    // Add the metadata for the currently playing track
		    .setContentTitle("Hello")
		    .setContentText("Cool")
		    .setSubText("Okay")
		    .setLargeIcon(null)

		    // Enable launching the player by clicking the notification
		    .setContentIntent(controller.getSessionActivity())

		    // Stop the service when the notification is swiped away
		    .setDeleteIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(this,
			PlaybackStateCompat.ACTION_STOP))

		    // Make the transport controls visible on the lockscreen
		    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

		    // Add an app icon and set its accent color
		    .setSmallIcon(R.mipmap.ic_launcher)
		    .setColor(ContextCompat.getColor(this, R.color.colorPrimary))

		    // Add a pause button
		    .addAction(new NotificationCompat.Action(
			R.drawable.ic_pause_white_24dp, getString(R.string.pause),
			MediaButtonReceiver.buildMediaButtonPendingIntent(getApplicationContext(),
			    PlaybackStateCompat.ACTION_PLAY_PAUSE)))

		    // Take advantage of MediaStyle features
		    .setStyle(new NotificationCompat.MediaStyle()
			.setMediaSession(mMediaSession.getSessionToken())
			.setShowActionsInCompactView(0)
			// Add a cancel button
			.setShowCancelButton(true)
			.setCancelButtonIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(this,
			    PlaybackStateCompat.ACTION_STOP)));

		return builder.build();
	}

	@Override
	public void onAudioFocusChange(int focusChange) {

	}

	public class MediaSessionCallback extends MediaSessionCompat.Callback {
		@Override
		public void onPlay() {
			Log.d(TAG, "onPlay: ");
			AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
			// Request audio focus for playback, this registers the afChangeListener
			int result = am.requestAudioFocus(MediaService.this,
			    // Use the music stream.
			    AudioManager.STREAM_MUSIC,
			    // Request permanent focus.
			    AudioManager.AUDIOFOCUS_GAIN);

			if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
				try {
					// Start the service
					startService(new Intent(getApplicationContext(), MediaService.class));
					// Set the session active  (and update metadata and state)
					mMediaSession.setActive(true);

					// Start player
					mPlayer.setDataSource(
					    getApplicationContext(),
					    ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
						Long.valueOf(mLibrary.get(0).getMediaId()))
					);
					mPlayer.prepare();
					mPlayer.setOnPreparedListener(mMediaListener);
					mPlayer.setOnErrorListener(mMediaListener);
					mPlayer.setOnInfoListener(mMediaListener);
					mPlayer.setOnSeekCompleteListener(mMediaListener);
					mPlayer.setOnCompletionListener(mMediaListener);

					// Register BECOME_NOISY BroadcastReceiver
					registerReceiver(myNoisyAudioStreamReceiver, intentFilter);

					// Put the service in the foreground, post notification
					startForeground(NOTIFICATION_ID, getNotification());
				} catch (IOException e) {
					Log.e(TAG, "onPlay: ", e);
				}
			}
		}

		@Override
		public void onStop() {
			Log.d(TAG, "onStop: ");
			AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
			// Abandon audio focus
			am.abandonAudioFocus(MediaService.this);
			unregisterReceiver(myNoisyAudioStreamReceiver);
			// Start the service
			stopSelf();
			// Set the session inactive  (and update metadata and state)
			mMediaSession.setActive(false);
			// stop the player (custom call)
			mPlayer.stop();
			// Take the service out of the foreground, remove notification
			stopForeground(true);
		}

		@Override
		public void onPause() {
			Log.d(TAG, "onPause: ");
			AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
			// Update metadata and state
			// pause the player (custom call)
			mPlayer.pause();

			// unregister BECOME_NOISY BroadcastReceiver
			unregisterReceiver(myNoisyAudioStreamReceiver);

			// Take the service out of the foreground, retain the notification
			stopForeground(false);
		}

	}

	private class BecomingNoisyReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
				mSessionCallback.onPause();
			}
		}
	}

	private class MediaPlayerListeners
	    implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnSeekCompleteListener,
	    MediaPlayer.OnCompletionListener, MediaPlayer.OnInfoListener {

		@Override
		public void onPrepared(MediaPlayer mp) {
			Log.d(TAG, "onPrepared: " + mp.getAudioSessionId());
			mp.start();
		}

		@Override
		public boolean onError(MediaPlayer mp, int what, int extra) {
			Log.e(TAG, "onError: " + what);
			return false;
		}

		@Override
		public void onSeekComplete(MediaPlayer mp) {
			Log.d(TAG, "onSeekComplete: ");
		}

		@Override
		public void onCompletion(MediaPlayer mp) {
			Log.d(TAG, "onCompletion: ");
		}

		@Override
		public boolean onInfo(MediaPlayer mp, int what, int extra) {
			Log.d(TAG, "onInfo: " + what);
			return false;
		}
	}
}
