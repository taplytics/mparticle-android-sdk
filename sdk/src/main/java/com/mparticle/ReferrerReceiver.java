package com.mparticle;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

/**
 * <code>BroadcastReceiver</code> required to capture attribution data via the Google Play install referrer broadcast.
 *
 *
 * When Google Play installs an application it will broadcast an <code>Intent</code> with the <code>com.android.vending.INSTALL_REFERRER</code> action.
 * From this <code>Intent</code>, mParticle will extract any available referral data for use in measuring the success of advertising or install campaigns.
 *
 *
 * This {@code BroadcastReceiver} must be specified within the {@code <application>} block of your application's {@code AndroidManifest.xml} file:
 *
 *
 * <pre>
 * {@code
 *  <receiver
 *      android:name="com.mparticle.ReferrerReceiver"
 *      android:exported="true">
 *      <intent-filter>
 *          <action android:name="com.android.vending.INSTALL_REFERRER"/>
 *      </intent-filter>
 *
 * </receiver> }</pre>
 *
 */
public class ReferrerReceiver extends BroadcastReceiver {
    @Override
    public final void onReceive(Context context, Intent intent) {
        if ("com.android.vending.INSTALL_REFERRER".equals(intent.getAction())) {
            String referrer = intent.getStringExtra("referrer");
            SharedPreferences preferences = context.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE);
            preferences.edit().putString(Constants.PrefKeys.INSTALL_REFERRER, referrer).commit();
        }
    }
}
