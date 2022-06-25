package z.zer.tor.media.android.gui.views;

import android.content.Context;
import android.graphics.Paint;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import z.zer.tor.media.R;
import z.zer.tor.media.android.core.Constants;
import z.zer.tor.media.android.gui.util.UIUtils;

public class SearchProgressView extends LinearLayout {

    private TextView textTryFrostWirePlus;


    public SearchProgressView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        super.onRestoreInstanceState(state);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        View.inflate(getContext(), R.layout.view_search_progress, this);

        if (isInEditMode()) {
            return;
        }


        textTryFrostWirePlus = findViewById(R.id.view_search_progress_try_frostwire_plus);
        textTryFrostWirePlus.setPaintFlags(textTryFrostWirePlus.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
    }

    @Override
    public void setOnTouchListener(OnTouchListener l) {
        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            getChildAt(i).setOnTouchListener(l);
        }
        super.setOnTouchListener(l);
    }

    public interface CurrentQueryReporter {
    }
}
