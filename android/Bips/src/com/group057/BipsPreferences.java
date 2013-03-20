package com.group057;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class BipsPreferences extends PreferenceActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }
}
