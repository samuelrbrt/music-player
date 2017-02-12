package com.zionbhavan.musicplayer.fragment;

import android.content.ComponentName;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.CallSuper;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserCompat.ConnectionCallback;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaControllerCompat.Callback;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import com.zionbhavan.musicplayer.service.MediaService;

/**
 * @author Samuel Robert <samuelrbrt16@gmail.com>
 * @created on 2017-02-12 at 12:58 PM
 * @signed_off_by Samuel Robert <samuelrbrt@gmail.com>
 */

abstract class MusicFragment extends Fragment {
	private MediaBrowserCompat mMediaBrowser;
	private ControllerCallback mControllerCallback;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Create MediaBrowserServiceCompat
		MediaConnectionCallback connectionCallbacks = new MediaConnectionCallback();
		mMediaBrowser = new MediaBrowserCompat(getContext(), new ComponentName(getContext(), MediaService.class),
		    connectionCallbacks, null);
		mControllerCallback = new ControllerCallback();
	}

	@Override
	public void onStart() {
		super.onStart();
		mMediaBrowser.connect();
	}

	@Override
	public void onStop() {
		super.onStop();

		if (MediaControllerCompat.getMediaController(getActivity()) != null) {
			MediaControllerCompat.getMediaController(getActivity()).unregisterCallback(mControllerCallback);
		}
		mMediaBrowser.disconnect();
	}

	/**
	 * Override this method to add music control functionality on music service
	 */
	@CallSuper
	protected void buildTransportControls() {
		MediaControllerCompat mediaController = MediaControllerCompat.getMediaController(getActivity());
		mediaController.registerCallback(mControllerCallback);
	}

	private class ControllerCallback extends Callback {
		@Override
		public void onMetadataChanged(MediaMetadataCompat metadata) {
		}

		@Override
		public void onPlaybackStateChanged(PlaybackStateCompat state) {
		}
	}

	private class MediaConnectionCallback extends ConnectionCallback {
		private static final String TAG = "MediaCallback";

		@Override
		public void onConnected() {
			try {
				// Get the token for the MediaSession
				MediaSessionCompat.Token token = mMediaBrowser.getSessionToken();

				// Create a MediaControllerCompat
				MediaControllerCompat mediaController = new MediaControllerCompat(getContext(), token);

				// Save the controller
				MediaControllerCompat.setMediaController(getActivity(), mediaController);

				// Finish building the UI
				buildTransportControls();
			} catch (RemoteException e) {
				Log.e(TAG, e.getMessage(), e);
			}

		}

		@Override
		public void onConnectionSuspended() {
			Log.e(TAG, "onConnectionSuspended: The Service has crashed");

			// TODO: Disable transport controls until it automatically reconnects
		}

		@Override
		public void onConnectionFailed() {
			Log.e(TAG, "onConnectionFailed: The Service has refused our connection");
		}
	}
}
