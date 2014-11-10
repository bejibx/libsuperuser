package eu.chainfire.libsuperuser;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import java.io.File;

public class SqliteDatabaseLink
{
    private static final String TAG = "SqliteDatabaseLink";
    private static final boolean DEBUG = BuildConfig.DEBUG;

    private String mPermissions;
    private String mOwnerAndGroup;
    private String mLink;
    private String mApplicationDatabaseDir;
    private String mDatabasePath;
    private String mTemporaryDatabasePath;

    private SQLiteDatabase mDatabase = null;

    private boolean mIsOpenedInLockMode = false;

    public SqliteDatabaseLink(Context context)
    {
        mApplicationDatabaseDir = context.getApplicationInfo().dataDir + File.separator;
    }

    public boolean open(String pathToDb)
    {
        if (mDatabase != null)
        {
            close();
        }

        if (ShellHelper.makeDirectory(mApplicationDatabaseDir))
        {
            mLink = mApplicationDatabaseDir + new File(pathToDb).getName();
            ShellHelper.createHardLink(pathToDb, mLink);

            mPermissions = ShellHelper.getPermissions(mLink);
            mOwnerAndGroup = ShellHelper.getOwnerAndGroup(mLink);
            ShellHelper.setPermissions(mLink, "rw-rw-rw-");
            try
            {
                mDatabase = SQLiteDatabase.openDatabase(mLink, null, SQLiteDatabase.OPEN_READWRITE);
            }
            catch (SQLiteException e)
            {
                if (DEBUG)
                {
                    String errorMessage = "Failed to open file \"" + pathToDb + "\" because " +
                            "database is locked. Trying to open database in locked mode.";
                    Log.e(TAG, errorMessage, e);
                }

                ShellHelper.removeFile(mLink);

                mDatabasePath = pathToDb;
                mTemporaryDatabasePath = pathToDb + ".tmp";
                mLink += ".tmp";
                ShellHelper.copyFile(pathToDb, mTemporaryDatabasePath);
                ShellHelper.createHardLink(mTemporaryDatabasePath, mLink);
                ShellHelper.setPermissions(mLink, "rw-rw-rw-");

                try
                {
                    mDatabase = SQLiteDatabase.openDatabase(mLink, null, SQLiteDatabase.OPEN_READWRITE);
                    mIsOpenedInLockMode = true;
                }
                catch (SQLiteException ee)
                {
                    if (DEBUG)
                    {
                        String errorMessage = "Failed to open file \"" + pathToDb + "\" in " +
                                "locked mode. Epic fail.";
                        Log.e(TAG, errorMessage, e);
                    }
                    mDatabase = null;
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public void close()
    {
        if (mDatabase != null)
        {
            mDatabase.close();
        }
        ShellHelper.setPermissions(mLink, mPermissions);
        ShellHelper.removeFile(mLink);
        if (mIsOpenedInLockMode)
        {
            if (ShellHelper.moveFile(mDatabasePath, mDatabasePath + ".bcp"))
            {
                if (ShellHelper.moveFile(mTemporaryDatabasePath, mDatabasePath))
                {
                    ShellHelper.removeFile(mDatabasePath + ".bcp");
                }
                else
                {
                    ShellHelper.moveFile(mDatabasePath + ".bcp", mDatabasePath);
                }
                ShellHelper.setOwnerAndGroup(mDatabasePath, mOwnerAndGroup);
                ShellHelper.removeFile(mDatabasePath + "-shm");
                ShellHelper.removeFile(mDatabasePath + "-wal");
                mIsOpenedInLockMode = false;
            }
        }
    }

    public int delete(String table, String whereClause, String[] whereArgs)
    {
        if (mDatabase != null)
        {
            try
            {
                return mDatabase.delete(table, whereClause, whereArgs);
            }
            catch (SQLiteException e)
            {
                return 0;
            }
        }
        else return 0;
    }

    public Cursor query(String table, String[] columns, String selection, String[] selectionArgs)
    {
        if (mDatabase != null)
        {
            return mDatabase.query(table, columns, selection, selectionArgs, null, null, null);
        }
        else return null;
    }

    public int update(String table, String fieldName, String value, String whereClause, String[] whereArgs)
    {
        if (mDatabase != null)
        {
            ContentValues contentValues = new ContentValues();
            contentValues.put(fieldName, value);
            try
            {
                return mDatabase.update(table, contentValues, whereClause, whereArgs);
            }
            catch (SQLiteException e)
            {
                return 0;
            }
        }
        return 0;
    }
}
