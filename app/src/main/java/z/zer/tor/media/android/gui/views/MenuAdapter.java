package z.zer.tor.media.android.gui.views;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import z.zer.tor.media.R;
import z.zer.tor.media.util.Ref;

import java.lang.ref.WeakReference;
import java.util.List;


public class MenuAdapter extends BaseAdapter {

    private final LayoutInflater inflater;
    private final WeakReference<Context> contextRef;
    private final String title;
    private final List<MenuAction> items;

    public MenuAdapter(Context context, String title, List<MenuAction> items) {
        this.contextRef = new WeakReference<>(context);
        this.inflater = LayoutInflater.from(context);
        this.title = title;
        this.items = items;
    }

    public Context getContext() {
        Context result = null;
        if (Ref.alive(contextRef)) {
            result = contextRef.get();
        }
        return result;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        MenuAction item = getItem(position);

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.view_menu_list_item, parent, false);
        }

        TextView textView = (TextView) convertView;

        textView.setTag(item);
        textView.setText(item.getText());

        textView.setCompoundDrawablesWithIntrinsicBounds(item.getImage(), null, null, null);

        return convertView;
    }

    public int getCount() {
        return items.size();
    }

    public MenuAction getItem(int position) {
        return items.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    public String getTitle() {
        return title;
    }
}
