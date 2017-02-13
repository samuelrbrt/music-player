package com.zionbhavan.musicplayer.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
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

import com.zionbhavan.musicplayer.R;
import com.zionbhavan.musicplayer.activity.HomeActivity;
import com.zionbhavan.musicplayer.content.Provider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Samuel Robert <samuelrbrt16@gmail.com>
 * @created on 2017-01-11 at 6:23 PM
 * @signed_off_by Samuel Robert <samuelrbrt16@gmail.com>
 */

@SuppressWarnings("ConstantConditions")
public class MediaService extends MediaBrowserServiceCompat implements AudioManager.OnAudioFocusChangeListener {
	private static final String TAG = "MediaService";
	private static final int NOTIFICATION_ID = 158;

	private MediaSessionCompat mMediaSession;
	private MediaPlayer mPlayer;

	private BecomingNoisyReceiver mNoisyAudioStreamReceiver;
	private IntentFilter mNoisyIntentFilter;

	private MediaSessionCallback mSessionCallback;
	private MediaPlayerListeners mMediaListener;

	private ArrayList<MediaItem> mLibrary;

	private int mCurrentPosition = 0;

	public MediaService() {
	}

	@Override
	public void onCreate() {
		super.onCreate();

		// Create a MediaSessionCompat
		mMediaSession = new MediaSessionCompat(getApplicationContext(), TAG);
		// Set the session's token so that client activities can communicate with it.
		setSessionToken(mMediaSession.getSessionToken());

		// Enable callbacks from MediaButtons and TransportControls
		mMediaSession.setFlags(
		    MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
		);

		// Set an initial PlaybackState with ACTION_PLAY, so media buttons can start the player
		PlaybackStateCompat.Builder mStateBuilder = new PlaybackStateCompat.Builder()
		    .setActions(
			PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
			    PlaybackStateCompat.ACTION_PLAY_PAUSE |
			    PlaybackStateCompat.ACTION_SKIP_TO_NEXT
		    );

		mMediaSession.setPlaybackState(mStateBuilder.build());

		// MediaSessionCallback has methods that handle callbacks from UI
		mSessionCallback = new MediaSessionCallback();
		mMediaSession.setCallback(mSessionCallback);

		// For Android 5.0 (API version 21) or greater
		// To enable restarting an inactive session in the background,
		// You must create a pending intent and setMediaButtonReceiver.
		Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
		mediaButtonIntent.setClass(getApplicationContext(), MediaButtonReceiver.class);
		PendingIntent mbrIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, mediaButtonIntent, 0);
		mMediaSession.setMediaButtonReceiver(mbrIntent);

		// Media player
		mPlayer = new MediaPlayer();
		mMediaListener = new MediaPlayerListeners();

		// Noisy receiver
		mNoisyAudioStreamReceiver = new BecomingNoisyReceiver();
		mNoisyIntentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);

		// Build library
		Provider mProvider = new Provider(getApplicationContext());
		mLibrary = new ArrayList<>();
		mProvider.buildLibrary(mLibrary);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		MediaButtonReceiver.handleIntent(mMediaSession, intent);
		return super.onStartCommand(intent, flags, startId);
	}


	@Nullable
	@Override
	public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
		if (allowBrowsing(clientPackageName)) {
			return new BrowserRoot(getString(R.string.app_name), null);
		}

		return null;
	}

	private boolean allowBrowsing(String clientPackageName) {
		return getPackageName().equals(clientPackageName);
	}

	@Override
	public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
		result.sendResult(mLibrary);
	}

	@Override
	public void onDestroy() {
		Log.w(TAG, "onDestroy: ");
		super.onDestroy();
		mMediaSession.release();
	}

	private Notification getNotification() {
		// Get the session's metadata
		MediaControllerCompat controller = mMediaSession.getController();
		MediaDescriptionCompat description = controller.getMetadata().getDescription();

		Intent intent = new Intent(getApplicationContext(), HomeActivity.class);
		PendingIntent activity = PendingIntent.getActivity(getApplicationContext(), 1, intent, PendingIntent
		    .FLAG_UPDATE_CURRENT);

		// Given a media session and its context (usually the component containing the session)
		// Create a NotificationCompat.Builder
		NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext());

		builder.setContentTitle(description.getTitle())
		    .setContentText(description.getSubtitle())
		    .setSubText(description.getDescription())
		    .setLargeIcon(description.getIconBitmap())

		    // Enable launching the player by clicking the notification
		    .setPriority(NotificationCompat.PRIORITY_MAX)
		    .setContentIntent(activity)
		    .setWhen(0)
		    .setShowWhen(false)

		    // Stop the service when the notification is swiped away
		    .setDeleteIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(this,
			PlaybackStateCompat.ACTION_STOP))

		    // Make the transport controls visible on the lockscreen
		    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

		    // Add an app icon and set its accent color
		    .setSmallIcon(R.mipmap.ic_launcher)
		    .setColor(ContextCompat.getColor(this, R.color.grey_800))
		    .addAction(
			new NotificationCompat.Action(
			    R.drawable.ic_skip_previous_white_24dp, getString(R.string.skip_to_previous),
			    MediaButtonReceiver.buildMediaButtonPendingIntent(
				getApplicationContext(),
				PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
			    )
			)
		    )
		    .addAction(
			new NotificationCompat.Action(
			    R.drawable.ic_pause_white_36dp, getString(R.string.pause),
			    MediaButtonReceiver.buildMediaButtonPendingIntent(
				getApplicationContext(),
				PlaybackStateCompat.ACTION_PLAY_PAUSE
			    )
			)
		    )
		    .addAction(
			new NotificationCompat.Action(
			    R.drawable.ic_skip_next_white_24dp, getString(R.string.skip_to_next),
			    MediaButtonReceiver.buildMediaButtonPendingIntent(
				getApplicationContext(),
				PlaybackStateCompat.ACTION_SKIP_TO_NEXT
			    )
			)
		    )

		    // Take advantage of MediaStyle features
		    .setStyle(new NotificationCompat.MediaStyle()
			.setMediaSession(mMediaSession.getSessionToken())
			.setShowActionsInCompactView(0, 1, 2)
			// Add a cancel button
			.setShowCancelButton(true)
			.setCancelButtonIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(this,
			    PlaybackStateCompat.ACTION_STOP)));

		return builder.build();
	}

	@Override
	public void onAudioFocusChange(int focusChange) {
		switch (focusChange) {
			case AudioManager.AUDIOFOCUS_LOSS:
				mPlayer.pause();
				break;

			case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
				mPlayer.pause();
				break;

			case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
				break;

			case AudioManager.AUDIOFOCUS_GAIN:
				mPlayer.start();
				break;

			case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT:
				break;

			case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE:
				break;

			case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK:
				break;
		}
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
				if (mMediaSession.getController().getPlaybackState().getState() != PlaybackStateCompat.STATE_PAUSED) {
					try {
						// Start the service
						startService(new Intent(getApplicationContext(), MediaService.class));

						mPlayer.release();
						mPlayer = new MediaPlayer();
						mPlayer.setOnPreparedListener(mMediaListener);
						mPlayer.setOnErrorListener(mMediaListener);
						mPlayer.setOnInfoListener(mMediaListener);
						mPlayer.setOnSeekCompleteListener(mMediaListener);
						mPlayer.setOnCompletionListener(mMediaListener);

						// Set player data source
						mPlayer.setDataSource(
						    getApplicationContext(),
						    ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
							Long.valueOf(mLibrary.get(mCurrentPosition).getMediaId()))
						);
						mPlayer.prepare();
					} catch (IOException e) {
						Log.e(TAG, "onPlay: ", e);
					}
				} else {
					mPlayer.start();
				}

				// Register BECOME_NOISY BroadcastReceiver
				registerReceiver(mNoisyAudioStreamReceiver, mNoisyIntentFilter);
				mMediaSession.setActive(true);

				// Set current playback state and metadata
				mMediaSession.setPlaybackState(
				    new PlaybackStateCompat.Builder()
					.setState(PlaybackStateCompat.STATE_PLAYING, mPlayer.getCurrentPosition(), 1f)
					.setActions(PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
					    PlaybackStateCompat.ACTION_PLAY_PAUSE |
					    PlaybackStateCompat.ACTION_SKIP_TO_NEXT)
					.build()
				);

				setCurrentMetadata();

				// Put the service in the foreground, post notification
				startForeground(NOTIFICATION_ID, getNotification());
			}
		}

		@Override
		public void onStop() {
			Log.d(TAG, "onStop: ");
			AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
			// Abandon audio focus
			am.abandonAudioFocus(MediaService.this);
			unregisterReceiver(mNoisyAudioStreamReceiver);

			// Stop the service
			stopSelf();

			// Set the session inactive  (and update metadata and state)
			mMediaSession.setActive(false);
			// stop the player (custom call)
			mPlayer.stop();

			// Take the service out of the foreground, remove notification
			stopForeground(true);

			mMediaSession.setPlaybackState(
			    new PlaybackStateCompat.Builder()
				.setState(PlaybackStateCompat.STATE_STOPPED, 0, 1f)
				.setActions(PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
				    PlaybackStateCompat.ACTION_PLAY_PAUSE |
				    PlaybackStateCompat.ACTION_SKIP_TO_NEXT)
				.build()
			);
		}

		@Override
		public void onPause() {
			Log.d(TAG, "onPause: ");

			// pause the player
			mPlayer.pause();

			// unregister BECOME_NOISY BroadcastReceiver
			unregisterReceiver(mNoisyAudioStreamReceiver);

			// Take the service out of the foreground, retain the notification
			stopForeground(false);

			mMediaSession.setPlaybackState(
			    new PlaybackStateCompat.Builder()
				.setState(PlaybackStateCompat.STATE_PAUSED, mPlayer.getCurrentPosition(), 1f)
				.setActions(PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
				    PlaybackStateCompat.ACTION_PLAY_PAUSE |
				    PlaybackStateCompat.ACTION_SKIP_TO_NEXT)
				.build()
			);
		}

		@Override
		public void onSkipToNext() {
			mMediaSession.setPlaybackState(
			    new PlaybackStateCompat.Builder()
				.setState(PlaybackStateCompat.STATE_SKIPPING_TO_NEXT, 0, 1f)
				.setActions(PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
				    PlaybackStateCompat.ACTION_PLAY_PAUSE |
				    PlaybackStateCompat.ACTION_SKIP_TO_NEXT)
				.build()
			);
			mCurrentPosition = (++mCurrentPosition) % mLibrary.size();
			onPlay();
		}

		@Override
		public void onSkipToPrevious() {
			mMediaSession.setPlaybackState(
			    new PlaybackStateCompat.Builder()
				.setState(PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS, 0, 1f)
				.setActions(PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
				    PlaybackStateCompat.ACTION_PLAY_PAUSE |
				    PlaybackStateCompat.ACTION_SKIP_TO_NEXT)
				.build()
			);

			mCurrentPosition = (--mCurrentPosition + mLibrary.size()) % mLibrary.size();
			onPlay();
		}
	}

	private void setCurrentMetadata() {
		// TODO: Add all metadata of the current song
		MediaDescriptionCompat currDescription = mLibrary.get(mCurrentPosition).getDescription();
		MediaMetadataCompat metadata = new MediaMetadataCompat.Builder()
		    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, currDescription.getTitle().toString())
		    .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, currDescription.getMediaId())
		    .build();
		mMediaSession.setMetadata(metadata);
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
			mMediaSession.getController().getTransportControls().skipToNext();
		}

		@Override
		public boolean onInfo(MediaPlayer mp, int what, int extra) {
			Log.d(TAG, "onInfo: " + what);
			return false;
		}
	}
}
