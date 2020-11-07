/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml),
 *            Marcelina Knitter (@marcelinkaaa)
 * Copyright (c) 2011-2018, FrostWire(R). All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package z.zer.tor.media.android.gui.adapters.menu;

import android.content.Context;

import z.zer.tor.media.R;
import z.zer.tor.media.android.core.FileDescriptor;
import z.zer.tor.media.android.gui.Librarian;
import z.zer.tor.media.android.gui.views.AbstractDialog;

import java.util.Arrays;

import static z.zer.tor.media.android.util.Asyncs.async;

/**
 * @author gubatron
 * @author aldenml
 * @author marcelinkaaa
 */
public final class DeleteSingleFileMenuAction extends AbstractDeleteFilesMenuAction {

    private final FileDescriptor fileDescriptor;
    private final byte fileType;

    public DeleteSingleFileMenuAction(Context context, byte fileType, FileDescriptor file, AbstractDialog.OnDialogClickListener clickListener) {
        super(context, R.drawable.contextmenu_icon_trash, R.string.delete_file_menu_action, clickListener);
        this.fileType = fileType;
        this.fileDescriptor = file;
    }

    protected void onDeleteClicked() {
        if (getContext() != null && fileType != (byte) -1) {
            async(getContext(), Librarian.instance()::deleteFiles, fileType, Arrays.asList(fileDescriptor));
        }
    }
}
