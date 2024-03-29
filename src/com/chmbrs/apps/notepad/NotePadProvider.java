/*
 
    Copyright 2011 Marcelo Zambrana Villarroel.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
 */

package com.chmbrs.apps.notepad;

import java.util.HashMap;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.LiveFolders;
import android.text.TextUtils;
import android.util.Log;

public class NotePadProvider extends ContentProvider 
{
    private static final String TAG = "NotePadProvider";
    
    private static final String DATABASE_NAME = "chmbrs_note_pad.db";
    private static final int DATABASE_VERSION = 2;
    private static final String NOTES_TABLE_NAME = "chmbrs_notes";
    
    private static HashMap<String, String> sNotesProjectionMap;
    private static HashMap<String, String> sLiveFolderProjectionMap;
    
    private static final int NOTES = 1;
    private static final int NOTE_ID = 2;
    private static final int LIVE_FOLDER_NOTES = 3;
    
    private static final UriMatcher sUriMatcher;
    
    private DatabaseHelper mOpenHelper;
    
    private NotePadApplication app;
    
    /**
     * This class helps open, create, and upgrade the database file.
     */
    private static class DatabaseHelper extends SQLiteOpenHelper 
    {

        DatabaseHelper(Context context)
        {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) 
        {
        	Log.w(TAG, "Creating Database " + db.getPath());
            db.execSQL("CREATE TABLE " + NOTES_TABLE_NAME + " ("
                    + Notes._ID + " INTEGER PRIMARY KEY,"
                    + Notes.SERVER_ID + " TEXT, "
                    + Notes.USER_ACCOUNT + " TEXT,"
                    + Notes.TITLE + " TEXT,"
                    + Notes.NOTE + " TEXT NOT NULL DEFAULT '',"
                    + Notes.CREATED_DATE + " INTEGER NOT NULL DEFAULT 0,"
                    + Notes.MODIFIED_DATE + " INTEGER NOT NULL DEFAULT 0,"
                    + Notes.PENDING_DELETE + " BOOLEAN NOT NULL DEFAULT 0"
                    + ");");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS " + NOTES_TABLE_NAME);
            onCreate(db);
        }
    }

	@Override
	public int delete(Uri uri, String where, String[] whereArgs) 
	{
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;
        switch (sUriMatcher.match(uri)) {
        case NOTES:
            count = db.delete(NOTES_TABLE_NAME, where, whereArgs);
            break;

        case NOTE_ID:
            String noteId = uri.getPathSegments().get(1);
            count = db.delete(NOTES_TABLE_NAME, Notes._ID + "=" + noteId
                    + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
            break;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;	}

	@Override
	public String getType(Uri uri) 
	{
        switch (sUriMatcher.match(uri)) 
        {
        case NOTES:
	        case LIVE_FOLDER_NOTES:
	            return Notes.CONTENT_TYPE;
	
	        case NOTE_ID:
	            return Notes.CONTENT_ITEM_TYPE;
	
	        default:
	            throw new IllegalArgumentException("Unknown URI " + uri);
        }
	}

	@Override
	public Uri insert(Uri uri, ContentValues initialValues) 
	{
		//Log.i(TAG," insert " + uri);
		// Validate the requested uri
        if (sUriMatcher.match(uri) != NOTES) {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        ContentValues values;
        if (initialValues != null) {
            values = new ContentValues(initialValues);
        } else {
            values = new ContentValues();
        }

        Long now = Long.valueOf(System.currentTimeMillis());

        // Make sure that the fields are all set
        if (values.containsKey(Notes.CREATED_DATE) == false) {
            values.put(Notes.CREATED_DATE, now);
        }

        if (values.containsKey(Notes.MODIFIED_DATE) == false) {
            values.put(Notes.MODIFIED_DATE, now);
        }

        if (values.containsKey(Notes.TITLE) == false) {
            Resources r = Resources.getSystem();
            values.put(Notes.TITLE, r.getString(android.R.string.untitled));
        }

        if (values.containsKey(Notes.NOTE) == false) {
            values.put(Notes.NOTE, "");
        }

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        long rowId = db.insert(NOTES_TABLE_NAME, Notes.NOTE, values);
        if (rowId > 0) {
            Uri noteUri = ContentUris.withAppendedId(Notes.CONTENT_URI, rowId);
            getContext().getContentResolver().notifyChange(noteUri, null);
            return noteUri;
        }

        throw new SQLException("Failed to insert row into " + uri);	
    }

	@Override
	public boolean onCreate() 
	{
		mOpenHelper = new DatabaseHelper(getContext());
		app = (NotePadApplication)getContext().getApplicationContext();
        return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) 
	{
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(NOTES_TABLE_NAME);
        switch (sUriMatcher.match(uri)) {
        case NOTES:
            qb.setProjectionMap(sNotesProjectionMap);
            break;

        case NOTE_ID:
            qb.setProjectionMap(sNotesProjectionMap);
            qb.appendWhere(Notes._ID + "=" + uri.getPathSegments().get(1));
            break;

        case LIVE_FOLDER_NOTES:
            qb.setProjectionMap(sLiveFolderProjectionMap);
            break;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        // If no sort order is specified use the default 
        String orderBy;
        if (TextUtils.isEmpty(sortOrder)) {
//			orderBy = Notes.DEFAULT_SORT_ORDER;  //now it gets the default sort type from the preferences. 
        	orderBy = app.getSortType();
        } else {
            orderBy = sortOrder;
        }
        //Log.i(TAG, "provider sort " + orderBy);

        // Get the database and run the query
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, orderBy);

        // Tell the cursor what uri to watch, so it knows when its source data changes
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
	}

	@Override
	public int update(Uri uri, ContentValues values, String where, String[] whereArgs) 
	{
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;
        switch (sUriMatcher.match(uri)) {
        case NOTES:
            count = db.update(NOTES_TABLE_NAME, values, where, whereArgs);
            break;

        case NOTE_ID:
            String noteId = uri.getPathSegments().get(1);
            count = db.update(NOTES_TABLE_NAME, values, Notes._ID + "=" + noteId
                    + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
            break;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;	
	}
	
	   static {
	        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
	        sUriMatcher.addURI(Notes.PROVIDER_NAME, "notes", NOTES);
	        sUriMatcher.addURI(Notes.PROVIDER_NAME, "notes/#", NOTE_ID);
	        sUriMatcher.addURI(Notes.PROVIDER_NAME, "live_folders/notes", LIVE_FOLDER_NOTES);

	        sNotesProjectionMap = new HashMap<String, String>();
	        sNotesProjectionMap.put(Notes._ID, Notes._ID);
	        sNotesProjectionMap.put(Notes.TITLE, Notes.TITLE);
	        sNotesProjectionMap.put(Notes.NOTE, Notes.NOTE);
	        sNotesProjectionMap.put(Notes.CREATED_DATE, Notes.CREATED_DATE);
	        sNotesProjectionMap.put(Notes.MODIFIED_DATE, Notes.MODIFIED_DATE);

	        // Support for Live Folders.
	        sLiveFolderProjectionMap = new HashMap<String, String>();
	        sLiveFolderProjectionMap.put(LiveFolders._ID, Notes._ID + " AS " +
	                LiveFolders._ID);
	        sLiveFolderProjectionMap.put(LiveFolders.NAME, Notes.TITLE + " AS " +
	                LiveFolders.NAME);
	        sLiveFolderProjectionMap.put(LiveFolders.DESCRIPTION, Notes.NOTE + " AS " +
	        		LiveFolders.DESCRIPTION);
	        // Add more columns here for more robust Live Folders.
	    }
}
