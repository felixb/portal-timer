/*
 * Copyright (C) 2013 Felix Bechstein
 * 
 * This file is part of Portal Timer.
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; If not, see <http://www.gnu.org/licenses/>.
 */
package de.ub0r.android.portaltimer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class MainActivity extends Activity implements OnClickListener {
	private static final String TAG = "portal-timer/ma";
	public static final String INGRESS_PACKAGE = "com.nianticproject.ingress";

	@SuppressLint("HandlerLeak")
	private class UpdateHandler extends Handler {
		@Override
		public void dispatchMessage(final Message msg) {
			for (int j = 0; j < Timer.TIMER_IDS.length; j++) {
				mTimers[j].refresh();
				if (mTextViews[j] != null) mTextViews[j].setText(mTimers[j]
						.getFormated());
			}
		}
	}

	private class UpdateThread extends Thread {
		@Override
		public void run() {
			while (mThread == this) {
				mHandler.sendEmptyMessage(0);
				long t = 0;
				for (int j = 0; j < Timer.TIMER_IDS.length; j++) {
					t = Math.max(t, mTimers[j].getTarget());
				}
				long d = (t > SystemClock.elapsedRealtime() ? 1000 : 5000);
				try {
					sleep(d);
				} catch (InterruptedException e) {
					// ignore
				}
			}
		}
	}

	private Timer[] mTimers;
	private TextView[] mTextViews;

	UpdateHandler mHandler = null;
	UpdateThread mThread = null;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Log.d(TAG, "onCreate()");

		mHandler = new UpdateHandler();

		if (getIntent().hasCategory(Intent.CATEGORY_LAUNCHER)
				&& PreferenceManager.getDefaultSharedPreferences(this)
						.getBoolean("start_ingress", false)) {
			try {
				Intent i = getPackageManager().getLaunchIntentForPackage(
						INGRESS_PACKAGE);
				i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(i);
				UpdateReceiver.trigger(this);
				finish();
			} catch (NullPointerException e) {
				Log.e(TAG, "unable to launch intent", e);
			} catch (ActivityNotFoundException e) {
				Log.e(TAG, "unable to launch intent", e);
			}
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.d(TAG, "onResume()");
		initTimers();
		mThread = new UpdateThread();
		mThread.start();
		UpdateReceiver.trigger(this);
	}

	@Override
	protected void onPause() {
		UpdateThread t = mThread;
		mThread = null;
		t.interrupt();
		super.onPause();
	}

	@Override
	public void onClick(final View v) {
		int id = v.getId();
		for (int j = 0; j < Timer.TIMER_IDS.length; j++) {
			if (id == Timer.RESET_IDS[j]) {
				mTimers[j].reset(this);
				mHandler.sendEmptyMessage(0);
				return;
			} else if (id == Timer.START_IDS[j]) {
				mTimers[j].start(this);
				mHandler.sendEmptyMessage(0);
				return;
			}
		}
	}

	private void initTimers() {
		mTimers = new Timer[Timer.TIMER_IDS.length];
		mTextViews = new TextView[Timer.TIMER_IDS.length];
		for (int j = 0; j < mTimers.length; j++) {
			mTimers[j] = new Timer(this, j);
			mTextViews[j] = (TextView) findViewById(Timer.TIMER_IDS[j]);
			if (mTextViews[j] != null) {
				findViewById(Timer.RESET_IDS[j]).setOnClickListener(this);
				findViewById(Timer.START_IDS[j]).setOnClickListener(this);
			}
		}
	}
}
