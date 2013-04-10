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
import android.preference.PreferenceManager;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import static java.lang.Math.max;
import static java.text.MessageFormat.format;
import static java.util.Arrays.asList;

public class Timer {
	private static final String TAG = "portal-timer/timer";

	private static final SimpleDateFormat MINUTE_FORMAT =
            new SimpleDateFormat("m:ss");
    private static final SimpleDateFormat HOUR_FORMAT =
            new SimpleDateFormat("h:mm'h'");

    private static final long SECOND = 1000l;
    private static final long MINUTE = 60l * SECOND;
    private static final long HOUR = 60l * MINUTE;

    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+");

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

	public static long parseCooldownString(final String s) {
		final List<String> periodParts = asList(s.trim().split(":"));
        final int length = periodParts.size();
        if (length > 3 || length < 1) {
            Log.d(TAG,
                    format("Failed to parse %s. Falling back to 5 minutes", s));
            return 5 * MINUTE;
        }
        Collections.reverse(periodParts);
        long cooldown = Long.parseLong(periodParts.get(0)) * SECOND;
        if (length >= 2) {
            cooldown += Long.parseLong(periodParts.get(1)) * MINUTE;
        }
        if (length == 3) {
            cooldown += Long.parseLong(periodParts.get(2)) * HOUR;
        }
		return cooldown;
	}

    public static boolean isValidCooldownString(final String s) {
        final List<String> periodParts = asList(s.trim().split(":"));
        final int length = periodParts.size();
        if (length > 3 || length < 1) {
            return false;
        }
        for (String part: periodParts) {
            if (!NUMBER_PATTERN.matcher(part).matches()) {
                return false;
            }
        }
        return true;
    }

	public Timer(final Context context, final int j) {
		mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		mKey = TIMER_KEYS[j];
        mCooldown = parseCooldownString(mPrefs.getString(COOLDOWN_KEYS[j],
                context.getString(R.string.cooldown)));
		Log.d(TAG, "new Timer(" + mKey + "): cooldown=" + mCooldown);
		refresh();
	}

	public long getTarget() {
		return mTarget;
	}

	public CharSequence getFormated() {
		final long value = mTarget == 0 ?
                mCooldown : max(mTarget - System.currentTimeMillis(), 0);
        final SimpleDateFormat format = value >= HOUR ?
                HOUR_FORMAT : MINUTE_FORMAT;
		return format.format(value);
	}

	public void start(final Context context) {
		mTarget = System.currentTimeMillis() + mCooldown;
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
		if (mTarget < System.currentTimeMillis() - mCooldown) {
			mTarget = 0;
		}
	}

	private void persist() {
		mPrefs.edit().putLong(PREF_TARGET + mKey, mTarget).commit();
	}
}
