package com.andrew.apollo.ui.activities;

import android.app.Fragment;
import android.os.Bundle;

import androidx.viewpager.widget.ViewPager;

import com.andrew.apollo.ui.fragments.phone.MusicBrowserPhoneFragment;

import z.zer.tor.media.R;

/**
 * This class is used to display the {@link ViewPager} used to swipe between the
 * main {@link Fragment}s used to browse the user's music.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public final class HomeActivity extends BaseActivity {

    public HomeActivity() {
        super(R.layout.activity_base);
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the music browser fragment
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction().replace(R.id.activity_base_content, new MusicBrowserPhoneFragment()).commit();
        }
    }
}
