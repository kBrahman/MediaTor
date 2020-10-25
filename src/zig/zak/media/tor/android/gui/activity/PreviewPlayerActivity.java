package zig.zak.media.tor.android.gui.activity;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.andrew.apollo.utils.MusicUtils;
import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.AdOptionsView;
import com.facebook.ads.MediaView;
import com.facebook.ads.NativeAd;
import com.facebook.ads.NativeAdLayout;
import com.facebook.ads.NativeAdListener;

import org.json.JSONObject;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

import androidx.annotation.NonNull;
import zig.zak.media.tor.R;
import zig.zak.media.tor.android.core.Constants;
import zig.zak.media.tor.android.core.player.CoreMediaPlayer;
import zig.zak.media.tor.android.gui.dialogs.NewTransferDialog;
import zig.zak.media.tor.android.gui.services.Engine;
import zig.zak.media.tor.android.gui.views.AbstractActivity;
import zig.zak.media.tor.android.gui.views.AbstractDialog;
import zig.zak.media.tor.search.FileSearchResult;
import zig.zak.media.tor.util.Logger;
import zig.zak.media.tor.util.Ref;


public final class PreviewPlayerActivity extends AbstractActivity implements AbstractDialog.OnDialogClickListener, MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnInfoListener, AudioManager.OnAudioFocusChangeListener, SeekBar.OnSeekBarChangeListener, Runnable {

    private static final Logger LOG = Logger.getLogger(PreviewPlayerActivity.class);
    private static final String TAG = PreviewPlayerActivity.class.getSimpleName();
    public static WeakReference<FileSearchResult> srRef;

    private MediaPlayer androidMediaPlayer;
    private String displayName;
    private String source;
    private String streamUrl;
    private boolean isFullScreen = false;
    private boolean changedActionBarTitleToNonBuffering = false;
    private NativeAd nativeAd;
    private SeekBar seekBar;
    private final Handler handler = new Handler();

    public PreviewPlayerActivity() {
        super(R.layout.activity_preview_player);
    }

    @Override
    protected void initComponents(Bundle savedInstanceState) {
        Intent i = getIntent();
        if (i == null) {
            finish();
            return;
        }
        stopAnyOtherPlayers();
        displayName = i.getStringExtra("displayName");
        source = i.getStringExtra("source");
        streamUrl = i.getStringExtra("streamUrl");
        isFullScreen = i.getBooleanExtra("isFullScreen", false);

        int mediaTypeStrId = R.string.audio;
        setTitle(getString(R.string.media_preview, getString(mediaTypeStrId)) + getString(R.string.buffering));
        final TextView trackName = findView(R.id.activity_preview_player_track_name);
        final TextView artistName = findView(R.id.activity_preview_player_artist_name);
        seekBar = findViewById(R.id.sb);
        seekBar.setOnSeekBarChangeListener(this);

        findViewById(R.id.play).setOnClickListener((v) -> {
            ImageButton button = (ImageButton) v;
            if (androidMediaPlayer.isPlaying()) {
                androidMediaPlayer.pause();
                button.setImageResource(android.R.drawable.ic_media_play);
            } else {
                androidMediaPlayer.start();
                button.setImageResource(android.R.drawable.ic_media_pause);
            }
        });
        trackName.setText(displayName);
        artistName.setText(source);
        play();
        loadNativeAd();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        if (outState != null) {
            super.onSaveInstanceState(outState);
            outState.putString("displayName", displayName);
            outState.putString("source", source);
            outState.putString("streamUrl", streamUrl);
            outState.putBoolean("isFullScreen", isFullScreen);
            if (androidMediaPlayer != null && androidMediaPlayer.isPlaying()) {
                outState.putInt("currentPosition", androidMediaPlayer.getCurrentPosition());
            }
        }
    }

    private void loadNativeAd() {
        // Instantiate a NativeAd object.
        // NOTE: the placement ID will eventually identify this as your App, you can ignore it for
        // now, while you are testing and replace it later when you have signed up.
        // While you are using this temporary code you will only get test ads and if you release
        // your code like this to the Google Play your users will not receive ads (you will get a no fill error).
        nativeAd = new NativeAd(this, getString(R.string.id_ad_native));

        NativeAdListener listener = new NativeAdListener() {
            @Override
            public void onMediaDownloaded(Ad ad) {
                Log.e(TAG, "Native ad finished downloading all assets.");
            }

            @Override
            public void onError(Ad ad, AdError adError) {
                // Native ad failed to load
                Log.e(TAG, "Native ad failed to load: " + adError.getErrorMessage());
            }

            @Override
            public void onAdLoaded(Ad ad) {
                if (nativeAd == null || nativeAd != ad) {
                    return;
                }
                // Inflate Native Ad into Container
                inflateAd(nativeAd);
            }


            @Override
            public void onAdClicked(Ad ad) {
                // Native ad clicked
                Log.d(TAG, "Native ad clicked!");
            }

            @Override
            public void onLoggingImpression(Ad ad) {
                // Native ad impression
                Log.d(TAG, "Native ad impression logged!");
            }
        };

        // Request an ad
        nativeAd.loadAd(nativeAd.buildLoadAdConfig().withAdListener(listener).build());
    }

    private void inflateAd(NativeAd nativeAd) {
        Log.i(TAG, "inflateAd");
        nativeAd.unregisterView();

        // Add the Ad view into the ad container.
        NativeAdLayout nativeAdLayout = findViewById(R.id.preview_native_ad_container);
        LayoutInflater inflater = LayoutInflater.from(this);
        // Inflate the Ad view.  The layout referenced should be the one you created in the last step.
        View adView = inflater.inflate(R.layout.native_ad_layout, nativeAdLayout, false);
        nativeAdLayout.addView(adView);

        // Add the AdOptionsView
        LinearLayout adChoicesContainer = findViewById(R.id.ad_choices_container);
        AdOptionsView adOptionsView = new AdOptionsView(this, nativeAd, nativeAdLayout);
        adChoicesContainer.removeAllViews();
        adChoicesContainer.addView(adOptionsView, 0);

        // Create native UI using the ad metadata.
        TextView nativeAdTitle = adView.findViewById(R.id.native_ad_title);
        MediaView nativeAdMedia = adView.findViewById(R.id.native_ad_media);
        TextView nativeAdSocialContext = adView.findViewById(R.id.native_ad_social_context);
        TextView nativeAdBody = adView.findViewById(R.id.native_ad_body);
        TextView sponsoredLabel = adView.findViewById(R.id.native_ad_sponsored_label);
        Button nativeAdCallToAction = adView.findViewById(R.id.native_ad_call_to_action);

        // Set the Text.
        nativeAdTitle.setText(nativeAd.getAdvertiserName());
        nativeAdBody.setText(nativeAd.getAdBodyText());
        nativeAdSocialContext.setText(nativeAd.getAdSocialContext());
        nativeAdCallToAction.setVisibility(nativeAd.hasCallToAction() ? View.VISIBLE : View.INVISIBLE);
        nativeAdCallToAction.setText(nativeAd.getAdCallToAction());
        sponsoredLabel.setText(nativeAd.getSponsoredTranslation());

        // Register the Title and CTA button to listen for clicks.
        nativeAd.registerViewForInteraction(adView, nativeAdMedia);
    }

    private String getFinalUrl(String url) {
        HttpURLConnection con = null;
        try {
            con = (HttpURLConnection) (new URL(url).openConnection());
            con.connect();
            InputStream inputStream = con.getInputStream();
            Scanner scanner = new Scanner(inputStream).useDelimiter("\\A");
            String result = scanner.hasNext() ? scanner.next() : "";
            return new JSONObject(result).getString("url");
        } catch (Throwable e) {
            LOG.error("Unable to detect final url", e);
        } finally {
            if (con != null) {
                try {
                    con.disconnect();
                } catch (Throwable e) {
                    // ignore
                }
            }
        }
        return url;
    }

    @Override
    public void onDialogClick(String tag, int which) {
        if (tag.equals(NewTransferDialog.TAG) && which == Dialog.BUTTON_POSITIVE) {
            if (Ref.alive(NewTransferDialog.srRef)) {
                releaseMediaPlayer();
                Intent i = new Intent(this, MainActivity.class);
                i.setAction(Constants.ACTION_START_TRANSFER_FROM_PREVIEW);
                i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(i);
            }
            finish();
        }
    }

    private void releaseMediaPlayer() {
        if (androidMediaPlayer != null) {
            androidMediaPlayer.stop();
            androidMediaPlayer.setSurface(null);
            try {
                androidMediaPlayer.release();
            } catch (Throwable t) {
                //there could be a runtime exception thrown inside stayAwake()
            }
            androidMediaPlayer = null;

            AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
            if (audioManager != null) {
                audioManager.abandonAudioFocus(this);
            }
        }
    }

    public void play() {
        Thread t = new Thread("PreviewPlayerActivity-onSurfaceTextureAvailable") {
            @Override
            public void run() {
                final String url = getFinalUrl(streamUrl);
                final Uri uri = Uri.parse(url);
                androidMediaPlayer = new MediaPlayer();
                try {
                    androidMediaPlayer.setDataSource(PreviewPlayerActivity.this, uri);
                    androidMediaPlayer.setOnBufferingUpdateListener(PreviewPlayerActivity.this);
                    androidMediaPlayer.setOnCompletionListener(PreviewPlayerActivity.this);
                    androidMediaPlayer.setOnPreparedListener(PreviewPlayerActivity.this);
                    androidMediaPlayer.setOnInfoListener(PreviewPlayerActivity.this);
                    androidMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    androidMediaPlayer.prepare();
                    startSeekBar();
                    androidMediaPlayer.start();
                    if (MusicUtils.isPlaying()) {
                        MusicUtils.playOrPause();
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        };
        t.start();
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        finish();
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (am != null) {
            am.requestAudioFocus(PreviewPlayerActivity.this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        }
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        boolean startedPlayback = false;
        switch (what) {
            case MediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING:
                //LOG.warn("Media is too complex to decode it fast enough.");
                //startedPlayback = true;
                break;
            case MediaPlayer.MEDIA_INFO_BUFFERING_START:
                //LOG.warn("Start of media buffering.");
                //startedPlayback = true;
                break;
            case MediaPlayer.MEDIA_INFO_BUFFERING_END:
                //LOG.warn("End of media buffering.");
                startedPlayback = true;
                break;
            case MediaPlayer.MEDIA_INFO_BAD_INTERLEAVING:
                break;
            case MediaPlayer.MEDIA_INFO_NOT_SEEKABLE:
                break;
            case MediaPlayer.MEDIA_INFO_METADATA_UPDATE:
                break;
            case MediaPlayer.MEDIA_INFO_UNKNOWN:
            default:
                break;
        }

        if (startedPlayback && !changedActionBarTitleToNonBuffering) {
            int mediaTypeStrId = R.string.audio;
            setTitle(getString(R.string.media_preview, getString(mediaTypeStrId)));
            changedActionBarTitleToNonBuffering = true;
        }

        return false;
    }

    public void stopAnyOtherPlayers() {
        try {
            final CoreMediaPlayer mediaPlayer = Engine.instance().getMediaPlayer();
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
        } catch (Throwable ignored) {
        }

        AudioManager mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (mAudioManager != null && mAudioManager.isMusicActive()) {
            Intent i = new Intent("com.android.music.musicservicecommand");
            i.putExtra("command", "pause");
            getApplication().sendBroadcast(i);
        }
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(this);
        stopAnyOtherPlayers();
        releaseMediaPlayer();
        super.onDestroy();
    }


    @Override
    protected void onPause() {
        releaseMediaPlayer();
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        changedActionBarTitleToNonBuffering = false;
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        if (focusChange == AudioManager.AUDIOFOCUS_LOSS || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
            releaseMediaPlayer();

            int mediaTypeStrId = R.string.audio;
            setTitle(getString(R.string.media_preview, getString(mediaTypeStrId)));
        }
    }

    private void startSeekBar() {
        handler.postDelayed(this, 1000);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            seekBar.setProgress(progress);
            androidMediaPlayer.seekTo(progress * androidMediaPlayer.getDuration() / 100);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void run() {
        if (androidMediaPlayer==null)return;
        int currentPosition = androidMediaPlayer.getCurrentPosition();
        int progress = currentPosition * 100 / androidMediaPlayer.getDuration();
        seekBar.setProgress(progress);
        startSeekBar();
    }
}
