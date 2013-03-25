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

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.util.Log;

public class Timer {
	private static final String TAG = "portal-timer/timer";

	private static final String FORMAT = "m:ss";

	private static final String PREF_TARGET = "target_";

	public static final String[] COOLDOWN_KEYS = new String[] { "cooldown0",
			"cooldown1", "cooldown2", "cooldown3" };
	public static final String[] TIMER_KEYS = new String[] { "timer0",
			"timer1", "timer2", "timer3" };
	public static final int[] TIMER_IDS = new int[] { R.id.timer0, R.id.timer1,
			R.id.timer2, R.id.timer3 };
	public static final int[] RESET_IDS = new int[] { R.id.reset0, R.id.reset1,
			R.id.reset2, R.id.reset3 };
	public static final int[] START_IDS = new int[] { R.id.start0, R.id.start1,
			R.id.start2, R.id.start3 };

	private final SharedPreferences mPrefs;
	private final String mKey;
	private long mCooldown;
	private long mTarget;

	@SuppressWarnings("deprecation")
	public static long parseCooldownString(final String s)
			throws NumberFormatException {
		String[] ss = s.trim().split(":");
		if (s.length() == 1) {
			return Integer.parseInt(ss[0]) * 1000;
		} else if (s.length() == 2) {
			return (Integer.parseInt(ss[0]) * 60 + Integer.parseInt(ss[1])) * 1000;
		} else {
			throw new NumberFormatException("invalid time: " + s);
		}
	}

	public Timer(final Context context, final int j) {
		mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		mKey = TIMER_KEYS[j];
		try {
			mCooldown = parseCooldownString(mPrefs.getString(COOLDOWN_KEYS[j],
					context.getString(R.string.cooldown)));
		} catch (NumberFormatException e) {
			Log.e(TAG, "parse error", e);
			mCooldown = 5 * 60 * 1000;
		}
		Log.d(TAG, "new Timer(" + mKey + "): cooldown=" + mCooldown);
		refresh();
	}

	public long getTarget() {
		return mTarget;
	}

	public CharSequence getFormated() {
		if (mTarget == 0) {
			return DateFormat.format(FORMAT, mCooldown);
		}
		long t = mTarget - SystemClock.elapsedRealtime();
		if (t < 0) {
			t = 0;
		}
		return DateFormat.format(FORMAT, t);

	}

	public void start(final Context context) {
		mTarget = SystemClock.elapsedRealtime() + mCooldown;
		Log.d(TAG, "start(" + mKey + "): " + mTarget);
		persist();
		UpdateReceiver.trigger(context);
	}

	public void resetFromReceiver(final Context context) {
		mTarget = 0;
		persist();
	}

	public void reset(final Context context) {
		resetFromReceiver(context);
		UpdateReceiver.trigger(context);
	}

	public void refresh() {
		mTarget = mPrefs.getLong(PREF_TARGET + mKey, 0);
		if (mTarget < SystemClock.elapsedRealtime() - mCooldown) {
			mTarget = 0;
		}
	}

	private void persist() {
		mPrefs.edit().putLong(PREF_TARGET + mKey, mTarget).commit();
	}
}
