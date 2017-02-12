package com.zionbhavan.musicplayer.activity;

import android.content.ComponentName;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import com.zionbhavan.musicplayer.R;
import com.zionbhavan.musicplayer.service.MediaService;

/**
 * @author Samuel Robert <samuelrbrt16@gmail.com>
 * @created on 2017-01-11 at 6:00 PM
 * @signed_off_by Samuel Robert <samuelrbrt16@gmail.com>
 */

public class HomeActivity extends AppCompatActivity
    implements NavigationView.OnNavigationItemSelectedListener {
	MediaControllerCompat.Callback controllerCallback =
	    new MediaControllerCompat.Callback() {
		    @Override
		    public void onMetadataChanged(MediaMetadataCompat metadata) {
		    }

		    @Override
		    public void onPlaybackStateChanged(PlaybackStateCompat state) {
		    }
	    };
	private MediaBrowserCompat mMediaBrowser;
	private MediaCallback mConnectionCallbacks;
	private AppCompatImageButton mPlayPauseIB;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home);
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
		    this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
		drawer.addDrawerListener(toggle);
		toggle.syncState();

		NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
		navigationView.setNavigationItemSelectedListener(this);
		
		mPlayPauseIB = (AppCompatImageButton) findViewById(R.id.ib_play_pause);
		
		// Create MediaBrowserServiceCompat
		mConnectionCallbacks = new MediaCallback();
		mMediaBrowser = new MediaBrowserCompat(this, new ComponentName(this, MediaService.class),
		    mConnectionCallbacks, null);
	}

	@Override
	public void onStart() {
		super.onStart();
		mMediaBrowser.connect();
	}

	@Override
	public void onStop() {
		super.onStop();

		// (see "stay in sync with the MediaSession")
		if (MediaControllerCompat.getMediaController(this) != null) {
			MediaControllerCompat.getMediaController(this).unregisterCallback(controllerCallback);
		}
		mMediaBrowser.disconnect();
	}
	
	private void buildTransportControls() {
		// Attach a listener to the button
		mPlayPauseIB.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// Since this is a play/pause button, you'll need to test the current state
				// and choose the action accordingly
				
				int pbState = MediaControllerCompat.getMediaController(HomeActivity.this).getPlaybackState()
				    .getState();
				if (pbState == PlaybackState.STATE_PLAYING) {
					MediaControllerCompat.getMediaController(HomeActivity.this).getTransportControls().pause();
				} else {
					MediaControllerCompat.getMediaController(HomeActivity.this).getTransportControls().play();
				}
			}
		});
		
		MediaControllerCompat mediaController = MediaControllerCompat.getMediaController(HomeActivity.this);
		
		// Display the initial state
		MediaMetadataCompat metadata = mediaController.getMetadata();
		PlaybackStateCompat pbState = mediaController.getPlaybackState();
		
		// Register a Callback to stay in sync
		mediaController.registerCallback(controllerCallback);
	}
	
	
	@Override
	public void onBackPressed() {
		DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		if (drawer.isDrawerOpen(GravityCompat.START)) {
			drawer.closeDrawer(GravityCompat.START);
		} else {
			super.onBackPressed();
		}
	}

	@Override
	public boolean onNavigationItemSelected(@NonNull MenuItem item) {
		switch (item.getItemId()) {
			case R.id.nav_listen_now:
				break;

			case R.id.nav_music_library:
				break;
		}

		DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		drawer.closeDrawer(GravityCompat.START);
		return true;
	}

	private class MediaCallback extends MediaBrowserCompat.ConnectionCallback {
		private static final String TAG = "MediaCallback";

		@Override
		public void onConnected() {
			try {
				// Get the token for the MediaSession
				MediaSessionCompat.Token token = mMediaBrowser.getSessionToken();

				// Create a MediaControllerCompat
				MediaControllerCompat mediaController =
				    new MediaControllerCompat(HomeActivity.this, token);

				// Save the controller
				MediaControllerCompat.setMediaController(HomeActivity.this, mediaController);

				// Finish building the UI
				buildTransportControls();
			} catch (RemoteException e) {
				Log.e(TAG, e.getMessage(), e);
			}

		}

		@Override
		public void onConnectionSuspended() {
			// The Service has crashed. Disable transport controls until it automatically reconnects
		}

		@Override
		public void onConnectionFailed() {
			// The Service has refused our connection
		}
	}
}
