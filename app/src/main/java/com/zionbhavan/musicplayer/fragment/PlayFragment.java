package com.zionbhavan.musicplayer.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.zionbhavan.musicplayer.R;

/**
 * @author Samuel Robert <samuelrbrt16@gmail.com>
 * @created on 2017-02-11 at 9:16 PM
 * @signed_off_by Samuel Robert <samuelrbrt16@gmail.com>
 */

public class PlayFragment extends Fragment {
	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_play, container, false);


		return rootView;
	}
}
