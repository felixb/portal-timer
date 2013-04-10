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

import java.util.ArrayList;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;

public class UpdateReceiver extends BroadcastReceiver {

	private static final String TAG = "portal-timer/ur";

	private static long[] VIBRATE = new long[] { 100, 500, 500, 500, 500, 500,
			500 };

	private long mNow = 0L;
	private long mNextTarget = 0L;

	private static long lastUpdate = 0L;

	public static void trigger(final Context context) {
		if (lastUpdate < System.currentTimeMillis() - 1000L) {
			new UpdateReceiver().updateNotification(context);
		}
		context.sendBroadcast(new Intent(context, UpdateReceiver.class));
	}

	@Override
	public void onReceive(final Context context, final Intent intent) {
		String a = intent.getAction();
		Log.d(TAG, "onReceive(" + a + ")");
		for (int j = 0; j < Timer.TIMER_IDS.length; j++) {
			if (Timer.TIMER_KEYS[j].equals(a)) {
				Timer t = new Timer(context, j);
				t.start(context);
			}
		}
		if (updateNotification(context)) {
			schedNext(context);
		}
	}

	private boolean updateNotification(final Context context) {
		Log.d(TAG, "updateNotification()");
		lastUpdate = System.currentTimeMillis();
		NotificationManager nm = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		ArrayList<Timer> timers = new ArrayList<Timer>();
		mNow = System.currentTimeMillis();
		mNextTarget = 0;
		boolean alert = false;
		Log.d(TAG, "mNow: " + mNow);

		for (int j = 0; j < Timer.TIMER_IDS.length; j++) {
			Timer t = new Timer(context, j);
			timers.add(t);
			long tt = t.getTarget();
			Log.d(TAG, "target(" + j + "): " + tt);

			if (tt > 0) {
				if (mNextTarget == 0 || tt < mNextTarget) {
					mNextTarget = tt;
				}
				if (tt < mNow) {
					alert = true;
					t.reset(context);
				}
			}
		}
		Log.d(TAG, "mNextTarget: " + mNextTarget);

		NotificationCompat.Builder b = new NotificationCompat.Builder(context);
		b.setPriority(1000);
		Intent i = new Intent(context, MainActivity.class);
		i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		b.setContentIntent(PendingIntent.getActivity(context, 0, i,
				PendingIntent.FLAG_CANCEL_CURRENT));

		b.setContentTitle(context.getString(R.string.app_name));
		b.setLargeIcon(BitmapFactory.decodeResource(context.getResources(),
				R.drawable.ic_launcher));
		b.setSmallIcon(R.drawable.ic_stat_timer);
		b.setAutoCancel(false);
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) { // GB-
			b.setContentText(context.getString(R.string.notification_text,
					timers.get(0).getFormatted(), timers.get(1).getFormatted(),
					timers.get(2).getFormatted()));
		} else { // HC+
			RemoteViews v = new RemoteViews(context.getPackageName(),
					R.layout.notification);
			for (int j = 0; j < Timer.TIMER_IDS.length; j++) {
				v.setTextViewText(Timer.TIMER_IDS[j], timers.get(j)
						.getFormatted().toString());
				Intent ij = new Intent(Timer.TIMER_KEYS[j], null, context,
						UpdateReceiver.class);
				v.setOnClickPendingIntent(Timer.TIMER_IDS[j], PendingIntent
						.getBroadcast(context, 0, ij,
								PendingIntent.FLAG_UPDATE_CURRENT));
			}
			v.setOnClickPendingIntent(R.id.settings, PendingIntent.getActivity(
					context, 0, new Intent(context, SettingsActivity.class),
					PendingIntent.FLAG_UPDATE_CURRENT));
			b.setContent(v);
		}

		if (mNextTarget <= 0 && !alert) {
			// we don't need any notification
			b.setOngoing(false);
			nm.notify(0, b.build());
			return false;
		} else if (alert) {
			// show notification without running Timer
			b.setOngoing(mNextTarget > 0);
			SharedPreferences p = PreferenceManager
					.getDefaultSharedPreferences(context);
			if (p.getBoolean("vibrate", true)) {
				b.setVibrate(VIBRATE);
			}
			String n = p.getString("notification", null);
			if (n == null) { // default
				b.setSound(RingtoneManager
						.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
			} else if (n.length() > 1) {
				try {
					b.setSound(Uri.parse(n));
				} catch (Exception e) {
					Log.e(TAG, "invalid notification uri", e);
					b.setSound(RingtoneManager
							.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
				}
			} // else: silent
			nm.notify(0, b.build());
			return true;
		} else {
			// show notification with running Timer
			b.setOngoing(true);
			nm.notify(0, b.build());
			return true;
		}
	}

	private void schedNext(final Context context) {
		Log.d(TAG, "schedNext()");
		AlarmManager am = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		Log.d(TAG, "current: " + mNow);
		long t;
		PowerManager pm = (PowerManager) context
				.getSystemService(Context.POWER_SERVICE);
		if (pm.isScreenOn()) {
			t = mNow + 1000L;
		} else {
			t = mNextTarget - mNow;
			Log.d(TAG, "t: " + t);
			if (t < 0) { // IllegalState?
				t = 30000;
			} else if (t < 30000) {
				t = 5000;
			} else if (t < 60000) {
				t = 15000;
			} else {
				t = 30000;
			}
			Log.d(TAG, "t: " + t);
			long diff = mNextTarget - (mNow + t);
			diff = (diff / 5000) * 5000;
			Log.d(TAG, "diff: " + diff);
			if (diff == 0) {
				t = mNow + 5000;
			} else {
				t = mNextTarget - diff - 1000;
			}
		}
		Log.d(TAG, "next: " + t);
		long et;
		if (t - System.currentTimeMillis() < 100) { // IllegalState?
			et = 1000 + SystemClock.elapsedRealtime();
		} else {
			et = t - System.currentTimeMillis() + SystemClock.elapsedRealtime();
		}
		am.set(AlarmManager.ELAPSED_REALTIME, et, PendingIntent.getBroadcast(
				context, 0, new Intent(context, UpdateReceiver.class),
				PendingIntent.FLAG_UPDATE_CURRENT));
	}
}
