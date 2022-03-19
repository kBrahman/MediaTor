/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml),
 *            Jose Molina (@votaguz)
 * Copyright (c) 2011-2017, FrostWire(R). All rights reserved.
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
import android.view.View;

import java.lang.ref.WeakReference;

import z.zer.tor.media.R;
import z.zer.tor.media.android.gui.views.MenuAction;
import z.zer.tor.media.util.Ref;

public class TransferDetailsMenuAction extends MenuAction {

    private final String infohash;
    private WeakReference<View> clickedViewRef; // used for possible toast message

    public TransferDetailsMenuAction(Context context, int stringId, String infoHash) {
        super(context, R.drawable.contextmenu_icon_file, stringId);
        this.infohash = infoHash;
    }

    @Override
    public void onClick(Context context) {

    }

    public TransferDetailsMenuAction setClickedView(View clickedView) {
        clickedViewRef = Ref.weak(clickedView);
        return this;
    }
}
