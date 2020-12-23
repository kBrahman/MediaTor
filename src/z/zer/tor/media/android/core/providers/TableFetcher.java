package z.zer.tor.media.android.core.providers;

import android.database.Cursor;
import android.net.Uri;

import z.zer.tor.media.android.core.FileDescriptor;

public interface TableFetcher {

    String[] getColumns();

    String getSortByExpression();

    Uri getContentUri();

    void prepare(Cursor cur);

    FileDescriptor fetch(Cursor cur);

    byte getFileType();

    String where();

    String[] whereArgs();
}
