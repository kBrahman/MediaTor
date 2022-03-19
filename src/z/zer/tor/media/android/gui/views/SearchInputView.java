package z.zer.tor.media.android.gui.views;

import android.content.Context;

import com.google.android.material.tabs.TabLayout;

import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout;

import z.zer.tor.media.R;
import z.zer.tor.media.android.core.ConfigurationManager;
import z.zer.tor.media.android.core.Constants;
import z.zer.tor.media.android.gui.views.ClearableEditTextView.OnActionListener;
import z.zer.tor.media.util.Ref;

public class SearchInputView extends LinearLayout {
    private final TextInputClickListener textInputListener;
    private final SuggestionsAdapter adapter;
    private ClearableEditTextView textInput;
    private OnSearchListener onSearchListener;

    public SearchInputView(Context context, AttributeSet set) {
        super(context, set);
        this.textInputListener = new TextInputClickListener(this);
        this.adapter = new SuggestionsAdapter(context);
    }

    public void setShowKeyboardOnPaste(boolean show) {
        textInput.setShowKeyboardOnPaste(show);
    }

    public void setOnSearchListener(OnSearchListener listener) {
        this.onSearchListener = listener;
    }

    public boolean isEmpty() {
        return textInput.getText().length() == 0;
    }

    public String getText() {
        return textInput.getText();
    }

    public void setText(String text) {
        textInput.setText(text);
    }

    public void showTextInput() {
        textInput.setVisibility(View.VISIBLE);
    }

    public void hideTextInput() {
        textInput.setVisibility(View.GONE);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        View.inflate(getContext(), R.layout.view_searchinput, this);

        if (isInEditMode()) {
            return;
        }

        textInput = findViewById(R.id.view_search_input_text_input);
        textInput.setOnKeyListener(textInputListener);
        textInput.setOnActionListener(textInputListener);
        textInput.setOnItemClickListener(textInputListener);
        textInput.setAdapter(adapter);

        updateHint();
    }

    private void startSearch(View v) {
        hideSoftInput(v);
        textInput.setListSelection(-1);
        textInput.dismissDropDown();
        adapter.discardLastResult();
        String query = textInput.getText().trim();
        if (query.length() > 0) {
            int mediaTypeId = ConfigurationManager.instance().getLastMediaTypeFilter();
            onSearch(query, mediaTypeId);
        }
    }

    private void onSearch(String query, int mediaTypeId) {
        if (onSearchListener != null) {
            onSearchListener.onSearch(this, query, mediaTypeId);
        }
    }

    private void onClear() {
        if (onSearchListener != null) {
            onSearchListener.onClear(this);
        }
    }

    private void hideSoftInput(View v) {
        InputMethodManager manager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (manager != null) {
            manager.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }

    private void updateHint() {
        final String searchFiles = getContext().getString(R.string.search_label) + " " + getContext().getString(R.string.files);
        final String orEnterYTorSCUrl = getContext().getString(R.string.or_enter_url);
        textInput.setHint(searchFiles + " " + orEnterYTorSCUrl);
    }

    public interface OnSearchListener {
        void onSearch(View v, String query, int mediaTypeId);

        void onClear(View v);
    }

    private static final class TextInputClickListener extends ClickAdapter<SearchInputView> implements OnItemClickListener, OnActionListener {

        private static final String TAG = SearchInputView.class.getSimpleName();

        TextInputClickListener(SearchInputView owner) {
            super(owner);
        }

        @Override
        public boolean onKey(SearchInputView owner, View v, int keyCode, KeyEvent event) {
            Log.i(TAG, "on key");
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP) {
                owner.startSearch(v);
                return true;
            } else if (keyCode == KeyEvent.ACTION_DOWN) {
                owner.showTextInput();
            }
            return false;
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (Ref.alive(ownerRef)) {
                SearchInputView owner = ownerRef.get();
                owner.startSearch(owner.textInput);
            }
        }

        @Override
        public void onTextChanged(View v, String str) {
        }

        @Override
        public void onClear(View v) {
            if (Ref.alive(ownerRef)) {
                ownerRef.get().onClear();
            }
        }
    }
}
