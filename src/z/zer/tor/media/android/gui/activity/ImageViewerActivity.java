package z.zer.tor.media.android.gui.activity;

import android.content.Intent;
import android.os.Bundle;

import z.zer.tor.media.R;
import z.zer.tor.media.android.core.FileDescriptor;
import z.zer.tor.media.android.gui.fragments.ImageViewerFragment;
import z.zer.tor.media.android.gui.views.AbstractActivity;

public final class ImageViewerActivity extends AbstractActivity {
    public ImageViewerActivity() {
        super(R.layout.activity_image_viewer);
    }

    @Override
    protected void initComponents(Bundle savedInstanceState) {
        Intent intent = getIntent();
        Bundle fileDescriptorBundle = intent.getBundleExtra(ImageViewerFragment.EXTRA_FILE_DESCRIPTOR_BUNDLE);

        if (fileDescriptorBundle != null && !fileDescriptorBundle.isEmpty()) {
            FileDescriptor fd = new FileDescriptor(fileDescriptorBundle);
            int position = intent.getIntExtra(ImageViewerFragment.EXTRA_ADAPTER_FILE_OFFSET, -1);
            ImageViewerFragment imageViewerFragment = findFragment(R.id.fragment_image_viewer);
            imageViewerFragment.updateData(fd, position);
            fileDescriptorBundle.clear();
        }
    }
}
