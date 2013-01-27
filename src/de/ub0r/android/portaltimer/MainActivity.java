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

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class MainActivity extends Activity implements OnClickListener {

	private class UpdateHandler extends Handler {
		@Override
		public void dispatchMessage(final Message msg) {
			mTimer0.refresh();
			mTimer1.refresh();
			mTimer2.refresh();
			mText0.setText(mTimer0.getFormated());
			mText1.setText(mTimer1.getFormated());
			mText2.setText(mTimer2.getFormated());
		}
	}

	private class UpdateThread extends Thread {
		@Override
		public void run() {
			while (mThread == this) {
				mHandler.sendEmptyMessage(0);
				long t = Math.max(mTimer0.getTarget(),
						Math.max(mTimer1.getTarget(), mTimer2.getTarget()));
				long d = (t > SystemClock.elapsedRealtime() ? 1000 : 5000);
				try {
					sleep(d);
				} catch (InterruptedException e) {
					// ignore
				}
			}
		}
	}

	private Timer mTimer0, mTimer1, mTimer2;
	private TextView mText0, mText1, mText2;

	UpdateHandler mHandler = null;
	UpdateThread mThread = null;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mTimer0 = new Timer(this, Timer.TIMER0);
		mTimer1 = new Timer(this, Timer.TIMER1);
		mTimer2 = new Timer(this, Timer.TIMER2);
		mText0 = (TextView) findViewById(R.id.timer0);
		mText1 = (TextView) findViewById(R.id.timer1);
		mText2 = (TextView) findViewById(R.id.timer2);

		findViewById(R.id.reset0).setOnClickListener(this);
		findViewById(R.id.reset1).setOnClickListener(this);
		findViewById(R.id.reset2).setOnClickListener(this);
		findViewById(R.id.start0).setOnClickListener(this);
		findViewById(R.id.start1).setOnClickListener(this);
		findViewById(R.id.start2).setOnClickListener(this);

		mHandler = new UpdateHandler();
	}

	@Override
	protected void onResume() {
		super.onResume();
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
		switch (v.getId()) {
		case R.id.reset0:
			mTimer0.reset(this);
			break;
		case R.id.reset1:
			mTimer1.reset(this);
			break;
		case R.id.reset2:
			mTimer2.reset(this);
			break;
		case R.id.start0:
			mTimer0.start(this);
			break;
		case R.id.start1:
			mTimer1.start(this);
			break;
		case R.id.start2:
			mTimer2.start(this);
			break;
		default:
			break;
		}
		mHandler.sendEmptyMessage(0);
	}
}
