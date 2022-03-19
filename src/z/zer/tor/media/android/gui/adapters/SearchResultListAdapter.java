package z.zer.tor.media.android.gui.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.SystemClock;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.apache.commons.io.FilenameUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import z.zer.tor.media.R;
import z.zer.tor.media.android.core.Constants;
import z.zer.tor.media.android.core.MediaType;
import z.zer.tor.media.android.gui.LocalSearchEngine;
import z.zer.tor.media.android.gui.activity.PlayerActivity;
import z.zer.tor.media.android.gui.util.UIUtils;
import z.zer.tor.media.android.gui.views.AbstractListAdapter;
import z.zer.tor.media.android.gui.views.ClickAdapter;
import z.zer.tor.media.android.gui.views.MediaPlaybackOverlayPainter;
import z.zer.tor.media.android.gui.views.MediaPlaybackStatusOverlayView;
import z.zer.tor.media.android.util.ImageLoader;
import z.zer.tor.media.search.FileSearchResult;
import z.zer.tor.media.search.KeywordFilter;
import z.zer.tor.media.search.SearchResult;
import z.zer.tor.media.search.StreamableSearchResult;
import z.zer.tor.media.search.soundcloud.SoundCloudSearchResult;
import z.zer.tor.media.search.torrent.TorrentSearchResult;
import z.zer.tor.media.util.Ref;

public abstract class SearchResultListAdapter extends AbstractListAdapter<SearchResult> {

    private static final int NO_FILE_TYPE = -1;
    private static final String TAG = SearchResultListAdapter.class.getSimpleName();
    private final PreviewClickListener previewClickListener;
    private int fileType;
    private final ImageLoader thumbLoader;
    private final List<KeywordFilter> keywordFiltersPipeline;
    private final AtomicLong lastFilterCallTimestamp = new AtomicLong();
    private FilteredSearchResults cachedFilteredSearchResults = null;

    protected SearchResultListAdapter(Context context) {
        super(context, R.layout.view_bittorrent_search_result_list_item);
        this.previewClickListener = new PreviewClickListener(context, this);
        this.fileType = NO_FILE_TYPE;
        this.thumbLoader = ImageLoader.getInstance(context);
        this.keywordFiltersPipeline = new LinkedList<>();
    }

    public PreviewClickListener getPreviewClickListener() {
        return previewClickListener;
    }

    public int getFileType() {
        return fileType;
    }

    public void setFileType(int fileType) {
        this.fileType = fileType;
        cachedFilteredSearchResults = null;
        filter();
    }

    public void addResults(List<? extends SearchResult> completeList, List<? extends SearchResult> filteredList) {
        visualList.addAll(filteredList); // java, java, and type erasure
        list.addAll(completeList);
        notifyDataSetChanged();
    }

    @Override
    public void clear() {
        super.clear();
        cachedFilteredSearchResults = null;
        clearKeywordFilters();
    }

    @Override
    protected void populateView(View view, SearchResult sr) {
        if (sr instanceof FileSearchResult) {
            populateFilePart(view, (FileSearchResult) sr);
        }
        if (sr instanceof TorrentSearchResult) {
            populateTorrentPart(view, (TorrentSearchResult) sr);
        }

        maybeMarkTitleOpened(view, sr);
        populateThumbnail(view, sr);
    }

    private void maybeMarkTitleOpened(View view, SearchResult sr) {
        int clickedColor = getContext().getResources().getColor(R.color.my_files_listview_item_inactive_foreground);
        int unclickedColor = getContext().getResources().getColor(R.color.app_text_primary);
        TextView title = findView(view, R.id.view_bittorrent_search_result_list_item_title);
        title.setTextColor(LocalSearchEngine.instance().hasBeenOpened(sr) ? clickedColor : unclickedColor);
    }

    private void populateFilePart(View view, FileSearchResult sr) {
        ImageView fileTypeIcon = findView(view, R.id.view_bittorrent_search_result_list_item_filetype_icon);
        fileTypeIcon.setImageResource(getFileTypeIconId());

        TextView adIndicator = findView(view, R.id.view_bittorrent_search_result_list_item_ad_indicator);
        adIndicator.setVisibility(View.GONE);

        TextView title = findView(view, R.id.view_bittorrent_search_result_list_item_title);
        title.setText(sr.getDisplayName());

        TextView fileSize = findView(view, R.id.view_bittorrent_search_result_list_item_file_size);
        if (sr.getSize() > 0) {
            fileSize.setText(UIUtils.getBytesInHuman(sr.getSize()));
        } else {
            fileSize.setText("...");
        }
        TextView seeds = findView(view, R.id.view_bittorrent_search_result_list_item_text_seeds);
        seeds.setText("");

    }

    private void populateThumbnail(View view, SearchResult sr) {
        ImageView fileTypeIcon = findView(view, R.id.view_bittorrent_search_result_list_item_filetype_icon);
        if (sr.getThumbnailUrl() != null) {
            thumbLoader.load(Uri.parse(sr.getThumbnailUrl()), fileTypeIcon, 96, 96, getFileTypeIconId());
        }

        MediaPlaybackStatusOverlayView overlayView = findView(view, R.id.view_bittorrent_search_result_list_item_filetype_icon_media_playback_overlay_view);
        if (isAudio(sr)) {
            view.setTag(sr);
            overlayView.setVisibility(View.VISIBLE);
            overlayView.setPlaybackState(MediaPlaybackOverlayPainter.MediaPlaybackState.PREVIEW);
            overlayView.setOnClickListener(previewClickListener);
        } else {
            fileTypeIcon.setTag(null);
            overlayView.setTag(null);
            overlayView.setVisibility(View.GONE);
            overlayView.setPlaybackState(MediaPlaybackOverlayPainter.MediaPlaybackState.NONE);
        }
    }

    private void populateTorrentPart(View view, TorrentSearchResult sr) {
        TextView seeds = findView(view, R.id.view_bittorrent_search_result_list_item_text_seeds);
        if (sr.getSeeds() > 0) {
            seeds.setText(getContext().getResources().getQuantityString(R.plurals.count_seeds_source, sr.getSeeds(), sr.getSeeds()));
        } else {
            seeds.setText("");
        }
    }

    @Override
    protected void onItemClicked(View v) {
        searchResultClicked(v);
    }

    abstract protected void searchResultClicked(View v);

    public FilteredSearchResults filter() {
        long now = SystemClock.currentThreadTimeMillis();
        long timeSinceLastFilterCall = now - lastFilterCallTimestamp.get();
        if (cachedFilteredSearchResults != null && timeSinceLastFilterCall < 250) {
            return cachedFilteredSearchResults;
        }
        lastFilterCallTimestamp.set(now);
        cachedFilteredSearchResults = filter(list);

        this.visualList = cachedFilteredSearchResults.filtered;
        notifyDataSetChanged();
        notifyDataSetInvalidated();
        return cachedFilteredSearchResults;
    }

    public FilteredSearchResults filter(List<SearchResult> results) {
        FilteredSearchResults fsr = new FilteredSearchResults();
        ArrayList<SearchResult> mediaTypedFiltered = new ArrayList<>();
        ArrayList<SearchResult> keywordFiltered = new ArrayList<>();
        List<KeywordFilter> keywordFilters = getKeywordFiltersPipeline();
        for (SearchResult sr : results) {
            String extension = FilenameUtils.getExtension(((FileSearchResult) sr).getFilename());
            MediaType mt = MediaType.getMediaTypeForExtension(extension);

            if ("youtube".equals(extension)) {
                mt = MediaType.getVideoMediaType();
            } else if (mt != null && mt.equals(MediaType.getVideoMediaType())) {
                // NOTE: this excludes all non .youtube youtube search results (e.g. 3gp, webm) from appearing on results
                mt = null;
            }

            boolean passedKeywordFilter = KeywordFilter.passesFilterPipeline(sr, keywordFilters);
            if (isFileSearchResultMediaTypeMatching(sr, mt)) {
                if (keywordFilters.isEmpty() || passedKeywordFilter) {
                    mediaTypedFiltered.add(sr);
                    keywordFiltered.add(sr);
                }
            } else if (mt != null && passedKeywordFilter) {
                keywordFiltered.add(sr);
            }
        }
        fsr.filtered = mediaTypedFiltered;
        fsr.keywordFiltered = keywordFiltered;
        return fsr;
    }

    private boolean isFileSearchResultMediaTypeMatching(SearchResult sr, MediaType mt) {
        return sr instanceof FileSearchResult && mt != null;
    }

    private static boolean isAudio(SearchResult sr) {
        return sr instanceof SoundCloudSearchResult;
    }

    private int getFileTypeIconId() {
        if (fileType == Constants.FILE_TYPE_MY_MUSIC) {
            return R.mipmap.ic_launcher;
        }
        return R.drawable.list_item_question_mark;
    }

    public List<KeywordFilter> getKeywordFiltersPipeline() {
        return keywordFiltersPipeline;
    }

    public FilteredSearchResults setKeywordFiltersPipeline(List<KeywordFilter> keywordFiltersPipeline) {
        // if another instance is being assigned, we clear and copy its members
        if (keywordFiltersPipeline != this.keywordFiltersPipeline) {
            this.keywordFiltersPipeline.clear();
            cachedFilteredSearchResults = null;
            if (keywordFiltersPipeline != null && keywordFiltersPipeline.size() > 0) {
                this.keywordFiltersPipeline.addAll(keywordFiltersPipeline);
            }
        }
        return filter();
    }

    public FilteredSearchResults addKeywordFilter(KeywordFilter kf) {
        if (!keywordFiltersPipeline.contains(kf)) {
            this.keywordFiltersPipeline.add(kf);
            cachedFilteredSearchResults = null;
            return filter();
        }
        return null;
    }

    public FilteredSearchResults removeKeywordFilter(KeywordFilter kf) {
        this.keywordFiltersPipeline.remove(kf);
        cachedFilteredSearchResults = null;
        return filter();
    }

    public FilteredSearchResults clearKeywordFilters() {
        this.keywordFiltersPipeline.clear();
        cachedFilteredSearchResults = null;
        return filter();
    }

    public static class FilteredSearchResults {
        public List<SearchResult> filtered;
        public List<SearchResult> keywordFiltered;

        public int numAudio;
        public int numVideo;
        public int numPictures;
        public int numApplications;
        public int numDocuments;
        public int numTorrents;

        public int numFilteredAudio;
        public int numFilteredVideo;
        public int numFilteredPictures;
        public int numFilteredApplications;
        public int numFilteredDocuments;
        public int numFilteredTorrents;

    }

    public static final class PreviewClickListener extends ClickAdapter<Context> {
        private static final String TAG = PreviewClickListener.class.getSimpleName();

        public final WeakReference<SearchResultListAdapter> adapterRef;

        PreviewClickListener(Context ctx, SearchResultListAdapter adapter) {
            super(ctx);
            adapterRef = Ref.weak(adapter);
        }

        @Override
        public void onClick(Context ctx, View v) {
            StreamableSearchResult sr = (StreamableSearchResult) v.getTag();
            if (sr != null) {
                LocalSearchEngine.instance().markOpened(sr, (Ref.alive(adapterRef)) ? adapterRef.get() : null);
                Intent i = new Intent(ctx, PlayerActivity.class);
                i.putExtra("displayName", sr.getDisplayName());
                i.putExtra("source", sr.getSource());
                i.putExtra("streamUrl", sr.getStreamUrl());
                i.putExtra("image_url", sr.getThumbnailUrl());
                ctx.startActivity(i);
            }
        }

    }
}
