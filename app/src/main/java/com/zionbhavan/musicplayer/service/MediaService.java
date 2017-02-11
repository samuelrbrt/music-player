package com.zionbhavan.musicplayer.service;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.NotificationCompat;

import com.zionbhavan.musicplayer.R;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Samuel Robert <samuelrbrt16@gmail.com>
 * @created on 2017-01-11 at 6:23 PM
 * @signed_off_by Samuel Robert <samuelrbrt16@gmail.com>
 */

public class MediaService extends MediaBrowserServiceCompat {
	private static final String TAG = "MediaService";
	private static final String MEDIA_ROOT_ID = "media_root_id";
	private static final int NOTIFICATION_ID = 158;

	private MediaSessionCompat mMediaSession;
	private PlaybackStateCompat.Builder mStateBuilder;

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
		mStateBuilder = new PlaybackStateCompat.Builder()
		    .setActions(PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PLAY_PAUSE);
		mMediaSession.setPlaybackState(mStateBuilder.build());

		// MediaController has methods that handle callbacks from UI
		mMediaSession.setCallback(new MediaController());

		// For Android 5.0 (API version 21) or greater
		// To enable restarting an inactive session in the background,
		// You must create a pending intent and setMediaButtonReceiver.
		Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);

		mediaButtonIntent.setClass(getApplicationContext(), MediaService.class);
		PendingIntent mbrIntent = PendingIntent.getService(getApplicationContext(), 0, mediaButtonIntent, 0);

		mMediaSession.setMediaButtonReceiver(mbrIntent);

		// Set the session's token so that client activities can communicate with it.
		setSessionToken(mMediaSession.getSessionToken());

	}

	@Nullable
	@Override
	public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
		if (allowBrowsing(clientPackageName, clientUid)) {
			return new BrowserRoot(MEDIA_ROOT_ID, null);
		} else {
			return null;
		}
	}

	private boolean allowBrowsing(String clientPackageName, int clientUid) {
		return getPackageName().equals(clientPackageName);
	}

	@Override
	public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
		// Assume for example that the music catalog is already loaded/cached.

		List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();

		// Check if this is the root menu:
		if (MEDIA_ROOT_ID.equals(parentId)) {

			// build the MediaItem objects for the top level,
			// and put them in the mediaItems list
		} else {

			// examine the passed parentMediaId to see which submenu we're at,
			// and put the children of that menu in the mediaItems list
		}

		result.sendResult(mediaItems);
	}

	private void buildNotification() {
		// Given a media session and its context (usually the component containing the session)
		// Create a NotificationCompat.Builder

		// Get the session's metadata
		MediaControllerCompat controller = mMediaSession.getController();
		MediaMetadataCompat mediaMetadata = controller.getMetadata();
		MediaDescriptionCompat description = mediaMetadata.getDescription();

		NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext());

		builder
		    // Add the metadata for the currently playing track
		    .setContentTitle(description.getTitle())
		    .setContentText(description.getSubtitle())
		    .setSubText(description.getDescription())
		    .setLargeIcon(description.getIconBitmap())

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

		// Display the notification and place the service in the foreground
		startForeground(NOTIFICATION_ID, builder.build());
	}

	public class MediaController extends MediaSessionCompat.Callback {
		@Override
		public void onPlay() {
			super.onPlay();

			buildNotification();
		}
	}
}
