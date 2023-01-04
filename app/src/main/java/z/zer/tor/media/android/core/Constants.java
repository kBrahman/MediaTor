package z.zer.tor.media.android.core;


public final class Constants {

    // preference keys
    public static final String IMAGE_URL = "image_url";
    public static final String PREF_KEY_CORE_UUID = "zig.zak.media.tor.core.uuid";
    public static final String PREF_KEY_CORE_LAST_SEEN_VERSION_BUILD = "zig.zak.media.tor.core.last_seen_version_build";
    public static final String PREF_KEY_MAIN_APPLICATION_ON_CREATE_TIMESTAMP = "zig.zak.media.tor.core.main_application_on_create_timestamp";

    public static final String PREF_KEY_NETWORK_USE_WIFI_ONLY = "frostwire.prefs.network.use_wifi_only";

    public static final String PREF_KEY_SEARCH_COUNT_DOWNLOAD_FOR_TORRENT_DEEP_SCAN = "frostwire.prefs.search.count_download_for_torrent_deep_scan";
    public static final String PREF_KEY_SEARCH_COUNT_ROUNDS_FOR_TORRENT_DEEP_SCAN = "frostwire.prefs.search.count_rounds_for_torrent_deep_scan";
    public static final String PREF_KEY_SEARCH_INTERVAL_MS_FOR_TORRENT_DEEP_SCAN = "frostwire.prefs.search.interval_ms_for_torrent_deep_scan";
    public static final String PREF_KEY_SEARCH_MIN_SEEDS_FOR_TORRENT_DEEP_SCAN = "frostwire.prefs.search.min_seeds_for_torrent_deep_scan";
    public static final String PREF_KEY_SEARCH_MIN_SEEDS_FOR_TORRENT_RESULT = "frostwire.prefs.search.min_seeds_for_torrent_result";
    public static final String PREF_KEY_SEARCH_MAX_TORRENT_FILES_TO_INDEX = "frostwire.prefs.search.max_torrent_files_to_index";
    public static final String PREF_KEY_SEARCH_FULLTEXT_SEARCH_RESULTS_LIMIT = "frostwire.prefs.search.fulltext_search_results_limit";

    public static final String PREF_KEY_SEARCH_USE_ZOOQLE = "frostwire.prefs.search.use_zooqle";
    public static final String PREF_KEY_SEARCH_USE_VERTOR = "frostwire.prefs.search.use_vertor";
    public static final String PREF_KEY_SEARCH_USE_YOUTUBE = "frostwire.prefs.search.use_youtube";
    public static final String PREF_KEY_SEARCH_USE_SOUNDCLOUD = "frostwire.prefs.search.use_soundcloud";
    public static final String PREF_KEY_SEARCH_USE_ARCHIVEORG = "frostwire.prefs.search.use_archiveorg";
    public static final String PREF_KEY_SEARCH_USE_FROSTCLICK = "frostwire.prefs.search.use_frostclick";
    public static final String PREF_KEY_SEARCH_USE_TORLOCK = "frostwire.prefs.search.use_torlock";
    public static final String PREF_KEY_SEARCH_USE_TORRENTDOWNLOADS = "frostwire.prefs.search.use_torrentdownloads";
    public static final String PREF_KEY_SEARCH_USE_LIMETORRENTS = "frostwire.prefs.search.use_limetorrents";
    public static final String PREF_KEY_SEARCH_USE_EZTV = "frostwire.prefs.search.use_eztv";
    public static final String PREF_KEY_SEARCH_USE_APPIA = "frostwire.prefs.search.use_appia";
    public static final String PREF_KEY_SEARCH_USE_TPB = "frostwire.prefs.search.use_tpb";
    public static final String PREF_KEY_SEARCH_USE_YIFY = "frostwire.prefs.search.use_yify";
    public static final String PREF_KEY_SEARCH_USE_TORRENTSFM = "frostwire.prefs.search.use_torrentsfm";
    public static final String PREF_KEY_SEARCH_USE_PIXABAY = "frostwire.prefs.search.use_pixabay";

    public static final String PREF_KEY_GUI_VIBRATE_ON_FINISHED_DOWNLOAD = "frostwire.prefs.gui.vibrate_on_finished_download";
    public static final String PREF_KEY_GUI_LAST_MEDIA_TYPE_FILTER = "frostwire.prefs.gui.last_media_type_filter";
    static final String PREF_KEY_GUI_ALREADY_RATED_US_IN_MARKET = "frostwire.prefs.gui.already_rated_in_market";
    public static final String PREF_KEY_GUI_FINISHED_DOWNLOADS_BETWEEN_RATINGS_REMINDER = "frostwire.prefs.gui.finished_downloads_between_ratings_reminder";
    public static final String PREF_KEY_GUI_INITIAL_SETTINGS_COMPLETE = "frostwire.prefs.gui.initial_settings_complete";
    public static final String PREF_KEY_GUI_ENABLE_PERMANENT_STATUS_NOTIFICATION = "frostwire.prefs.gui.enable_permanent_status_notification";
    public static final String PREF_KEY_GUI_SEARCH_KEYWORDFILTERDRAWER_TIP_TOUCHTAGS_DISMISSED = "frostwire.prefs.gui.search.keywordfilterdrawer.tip_touchtags_dismissed";
    public static final String PREF_KEY_GUI_SEARCH_FILTER_DRAWER_BUTTON_CLICKED = "frostwire.prefs.gui.search.search.filter_drawer_button_clicked";
    public static final String PREF_KEY_GUI_SHOW_TRANSFERS_ON_DOWNLOAD_START = "frostwire.prefs.gui.show_transfers_on_download_start";
    public static final String PREF_KEY_GUI_SHOW_NEW_TRANSFER_DIALOG = "frostwire.prefs.gui.show_new_transfer_dialog";
    public static final String PREF_KEY_GUI_USE_APPLOVIN = "frostwire.prefs.gui.use_applovin";
    public static final String PREF_KEY_GUI_USE_REMOVEADS = "frostwire.prefs.gui.use_removeads";
    public static final String PREF_KEY_GUI_USE_MOPUB = "frostwire.prefs.gui.use_mopub";
    public static final String PREF_KEY_GUI_REMOVEADS_BACK_TO_BACK_THRESHOLD = "frostwire.prefs.gui.removeads_back_to_back_threshold";
    public static final String PREF_KEY_GUI_MOPUB_SEARCH_HEADER_BANNER_THRESHOLD = "frostwire.prefs.gui.mopub_search_header_banner_threshold";
    public static final String PREF_KEY_GUI_MOPUB_SEARCH_HEADER_BANNER_DISMISS_INTERVAL_IN_MS = "frostwire.prefs.gui.mopub_search_header_banner_dismiss_interval_in_ms_int";
    public static final String PREF_KEY_GUI_OGURY_THRESHOLD = "frostwire.prefs.gui.ogury_threshold";

    public static final String PREF_KEY_GUI_INTERSTITIAL_OFFERS_TRANSFER_STARTS = "frostwire.prefs.gui.interstitial_offers_transfer_starts";
    public static final String PREF_KEY_GUI_INTERSTITIAL_TRANSFER_OFFERS_TIMEOUT_IN_MINUTES = "frostwire.prefs.gui.interstitial_transfer_offers_timeout_in_minutes";
    public static final String PREF_KEY_GUI_INTERSTITIAL_FIRST_DISPLAY_DELAY_IN_MINUTES = "frostwire.prefs.gui.interstitial_on_resume_first_display_delay_in_minutes";
    public static final String PREF_KEY_GUI_INTERSTITIAL_LAST_DISPLAY = "frostwire.prefs.gui.interstitial_on_resume_last_display";
    public static final String PREF_KEY_GUI_INSTALLATION_TIMESTAMP = "frostwire.prefs.gui.installation_timestamp";

    public static final String PREF_KEY_GUI_PLAYER_REPEAT_MODE = "com.frostwire.android.player.REPEAT_MODE";
    public static final String PREF_KEY_GUI_PLAYER_SHUFFLE_ENABLED = "com.frostwire.android.player.SHUFFLE_ENABLED";

    public static final String PREF_KEY_GUI_OFFERS_WATERFALL = "frostwire.prefs.gui.offers_waterfall";
    public static final String PREF_KEY_GUI_HAPTIC_FEEDBACK_ON = "frostwire.prefs.gui.haptic_feedback_on";
    public static final String PREF_KEY_GUI_DISTRACTION_FREE_SEARCH = "frostwire.prefs.gui.distraction_free_search";
    public static final String PREF_KEY_ADNETWORK_ASK_FOR_LOCATION_PERMISSION = "frostwire.prefs.gui.adnetwork_ask_for_location";

    public static final String PREF_KEY_TORRENT_MAX_DOWNLOAD_SPEED = "frostwire.prefs.torrent.max_download_speed";
    public static final String PREF_KEY_TORRENT_MAX_UPLOAD_SPEED = "frostwire.prefs.torrent.max_upload_speed";
    public static final String PREF_KEY_TORRENT_MAX_DOWNLOADS = "frostwire.prefs.torrent.max_downloads";
    public static final String PREF_KEY_TORRENT_MAX_UPLOADS = "frostwire.prefs.torrent.max_uploads";
    public static final String PREF_KEY_TORRENT_MAX_TOTAL_CONNECTIONS = "frostwire.prefs.torrent.max_total_connections";
    public static final String PREF_KEY_TORRENT_MAX_PEERS = "frostwire.prefs.torrent.max_peers";
    public static final String PREF_KEY_TORRENT_SEED_FINISHED_TORRENTS = "frostwire.prefs.torrent.seed_finished_torrents";
    public static final String PREF_KEY_TORRENT_SEED_FINISHED_TORRENTS_WIFI_ONLY = "frostwire.prefs.torrent.seed_finished_torrents_wifi_only";
    public static final String PREF_KEY_TORRENT_DELETE_STARTED_TORRENT_FILES = "frostwire.prefs.torrent.delete_started_torrent_files";
    public static final String PREF_KEY_TORRENT_TRANSFER_DETAIL_LAST_SELECTED_TAB_INDEX = "frostwire.prefs.torrent.transfer_detail_last_selected_tab_index";

    public static final String PREF_KEY_STORAGE_PATH = "frostwire.prefs.storage.path";

    public static final String PREF_KEY_UXSTATS_ENABLED = "frostwire.prefs.uxstats.enabled";

    public static final String ACTION_SHOW_TRANSFERS = "com.frostwire.android.ACTION_SHOW_TRANSFERS";
    public static final String ACTION_NOTIFY_DATA_INTERNET_CONNECTION = "com.frostwire.android.NOTIFY_CHECK_INTERNET_CONNECTION";
    public static final String EXTRA_DOWNLOAD_COMPLETE_NOTIFICATION = "com.frostwire.android.EXTRA_DOWNLOAD_COMPLETE_NOTIFICATION";
    public static final String EXTRA_DOWNLOAD_COMPLETE_PATH = "com.frostwire.android.EXTRA_DOWNLOAD_COMPLETE_PATH";
    public static final String EXTRA_REFRESH_FILE_TYPE = "com.frostwire.android.EXTRA_REFRESH_FILE_TYPE";
    public static final String EXTRA_FINISH_MAIN_ACTIVITY = "com.frostwire.android.EXTRA_FINISH_MAIN_ACTIVITY";

    public static final String ASKED_FOR_ACCESS_COARSE_LOCATION_PERMISSIONS = "frostwire.prefs.gui.asked_for_access_coarse_location_permissions";

    // generic file types
    public static final byte FILE_TYPE_AUDIO = 0x00;
    public static final byte FILE_TYPE_MY_MUSIC = 0x01;
    public static final String AD_NETWORK_SHORTCODE_APPLOVIN = "AL";

    public static final String FROSTWIRE_VPN_URL = "http://www.frostwire.com/vpn";

    public static final String EXPRESSVPN_URL = "https://www.linkev.com/?offer=3monthsfree&a_fid=frostwire";
    public static final String NORDVPN_URL = "https://go.nordvpn.net/aff_c?offer_id=222&aff_id=11226";

    public static final int NOTIFICATION_FROSTWIRE_STATUS = 1000;
    public static final int NOTIFICATION_DOWNLOAD_TRANSFER_FINISHED = 1001;
    public static final int NOTIFICATION_FROSTWIRE_PLAYER_STATUS = 1002;

    public static final String MEDIA_TOR_NOTIFICATION_CHANNEL_ID = "media_tor";

}
