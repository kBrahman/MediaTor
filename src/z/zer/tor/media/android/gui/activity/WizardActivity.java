package z.zer.tor.media.android.gui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ViewFlipper;

import z.zer.tor.media.R;
import z.zer.tor.media.android.StoragePicker;
import z.zer.tor.media.android.core.ConfigurationManager;
import z.zer.tor.media.android.core.Constants;
import z.zer.tor.media.android.gui.util.UIUtils;
import z.zer.tor.media.android.gui.views.AbstractActivity;
import z.zer.tor.media.android.gui.views.GeneralWizardPage;
import z.zer.tor.media.android.gui.views.WizardPageView;
import z.zer.tor.media.android.gui.views.WizardPageView.OnCompleteListener;

public class WizardActivity extends AbstractActivity {
    private final OnCompleteListener completeListener;
    private Button buttonPrevious;
    private Button buttonNext;
    private ViewFlipper viewFlipper;
    private View currentPageView;

    public WizardActivity() {
        super(R.layout.activity_wizard);
        completeListener = (pageView, complete) -> {
            if (pageView == currentPageView) {
                buttonNext.setEnabled(complete);
            }
        };
    }

    @Override
    protected void onCreate(Bundle savedInstance) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstance);
    }

    @Override
    protected void initComponents(Bundle savedInstanceState) {
        buttonPrevious = findView(R.id.activity_wizard_button_previous);
        buttonPrevious.setOnClickListener(v -> previousPage());

        buttonNext = findView(R.id.activity_wizard_button_next);
        buttonNext.setOnClickListener(v -> nextPage());

        viewFlipper = findView(R.id.activity_wizard_view_flipper);
        int count = viewFlipper.getChildCount();
        for (int i = 0; i < count; i++) {
            View view = viewFlipper.getChildAt(i);
            if (view instanceof WizardPageView) {
                ((WizardPageView) view).setOnCompleteListener(completeListener);
            }
            view.setTag(i);
        }

        setupViewPage();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // user picked a folder in the GeneralWizardPage.
        if (requestCode == StoragePicker.SELECT_FOLDER_REQUEST_CODE) {
            onStoragePathChanged(requestCode, resultCode, data);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void onStoragePathChanged(int requestCode, int resultCode, Intent data) {
        View view = viewFlipper.getCurrentView();
        if (view instanceof GeneralWizardPage) {
            String newPath = StoragePicker.handle(this, requestCode, resultCode, data);
            if (newPath != null) {
                ((GeneralWizardPage) view).updateStoragePathTextView(newPath);
            }
        }
    }

    private void previousPage() {
        viewFlipper.showPrevious();
        setupViewPage();
    }

    private void nextPage() {
        View view = viewFlipper.getCurrentView();
        if (view instanceof WizardPageView) {
            WizardPageView pageView = (WizardPageView) view;
            pageView.finish();
            if (!pageView.hasNext()) {
                ConfigurationManager CM = ConfigurationManager.instance();
                CM.setBoolean(Constants.PREF_KEY_GUI_TOS_ACCEPTED, true);
                CM.setBoolean(Constants.PREF_KEY_GUI_INITIAL_SETTINGS_COMPLETE, true);
                CM.setLong(Constants.PREF_KEY_GUI_INSTALLATION_TIMESTAMP, System.currentTimeMillis());
            } else {
                viewFlipper.showNext();
                setupViewPage();
            }
        } else {
            viewFlipper.showNext();
            setupViewPage();
        }
    }

    private void setupViewPage() {
        View view = viewFlipper.getCurrentView();
        currentPageView = view;
        if (view instanceof WizardPageView) {
            WizardPageView pageView = (WizardPageView) view;

            buttonPrevious.setVisibility(pageView.hasPrevious() ? View.VISIBLE : View.INVISIBLE);
            buttonNext.setText(pageView.hasNext() ? R.string.wizard_next : R.string.wizard_finish);
            buttonNext.setEnabled(false);

            pageView.load();
        } else {
            buttonPrevious.setVisibility(View.VISIBLE);
            buttonNext.setEnabled(true);
        }
    }

    @Override
    public void onBackPressed() {
        final View view = viewFlipper.getCurrentView();
        if (view instanceof WizardPageView && !((WizardPageView) view).hasPrevious()) {
            UIUtils.sendShutdownIntent(this);
            finish();
        } else {
            previousPage();
        }
    }
}
