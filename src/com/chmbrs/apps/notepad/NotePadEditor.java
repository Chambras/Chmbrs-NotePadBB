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


import com.google.android.apps.analytics.GoogleAnalyticsTracker;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.util.Linkify;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

public class NotePadEditor extends Activity
{
	private static final String TAG = "NoteEditor";
	 
    // The different distinct states the activity can be run in.
    private static final int STATE_EDIT = 0;
    private static final int STATE_INSERT = 1;
    
    private static final int COLUMN_INDEX_TITLE = 1;
    private static final int COLUMN_INDEX_NOTE = 2;
    private static final String ORIGINAL_CONTENT = "origContent";
    
    
    private int editorState;
    private Uri mUri;
	 
	private EditText noteText;
	private Cursor mCursor;
	private String noteOriginalContent;
	
	private GoogleAnalyticsTracker tracker;
	
    /**
     * Standard projection for the interesting columns of a normal note.
     */
    private static final String[] PROJECTION = new String[] {
            Notes._ID, // 0
            Notes.TITLE, // 0            
            Notes.NOTE, // 1
    };

	private static final String SET_REMINDER = "com.chmbrs.apps.notepad.notes.action.NOTE_SET_REMINDER";
	private final static String EDIT_TITLE = "com.chmbrs.apps.notepad.notes.action.EDIT_TITLE";
	private final static String REFRESH_NOTES = "com.chmbrs.apps.notepad.notes.action.REFRESH_NOTES";
	
	private NotePadApplication app;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		
		final Intent intent = getIntent();
		final String action = intent.getAction();
		
		app = ((NotePadApplication) NotePadEditor.this.getApplication());
		
		if (tracker == null)
		{
	        tracker = app.getTracker();			
		}
		
		if (Intent.ACTION_EDIT.equals(action))
		{
			editorState = STATE_EDIT;
			mUri = intent.getData();
			//Log.i(TAG, "editando una nota " + intent.getAction());
		}
		else if(Intent.ACTION_INSERT.equals(action))
		{	
			editorState = STATE_INSERT;
			//creating a blank note
			mUri = getContentResolver().insert(intent.getData(),null);
			
			//checking if the note was created. If not, a RESULT_CANCELED will be sent as a
			//response
			if(mUri == null)
			{
				Log.e(TAG, "Failed to insert blank note" + getIntent().getData());
				finish();
				return;
			}
			//Log.i(TAG, "suponemos q la nota fue creada tonces seguimos");
			setResult(RESULT_OK, (new Intent()).setAction(mUri.toString()));
			
		}
		else
		{
			Log.e(TAG, "accion no controlada no hacemos nada" + intent.getAction());
			return;
		}
		
		setContentView(R.layout.genericnote);
		
		if(editorState == STATE_EDIT)
		{
			((ViewGroup) findViewById(R.id.placeholder)).addView((LinearLayout)getLayoutInflater().inflate(R.layout.editnoteactionbar, null));
			//inflate edit menu
		}
		else if(editorState == STATE_INSERT)
		{
			((ViewGroup) findViewById(R.id.placeholder)).addView((LinearLayout)getLayoutInflater().inflate(R.layout.discardnoteactionbar, null));
			//inflate new note menu
		}
		
		noteText = (EditText)findViewById(R.id.editTextNewNote);
		noteText.setTextSize(app.getTextSize());
		noteText.setTypeface(app.getTypeface());
		if(app.getAutoLink())
		{
			noteText.setAutoLinkMask(Linkify.ALL);
		}
		 // Get the note!
        mCursor = managedQuery(mUri, PROJECTION, null, null, null);
        
        if(savedInstanceState != null)
        {
        	noteOriginalContent = savedInstanceState.getString(ORIGINAL_CONTENT);
        }
		
	}
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // Save away the original text, so we still have it if the activity
        // needs to be killed while paused.
    	Log.i(TAG, "saving original data");
        outState.putString(ORIGINAL_CONTENT, noteOriginalContent);
    }
	
	@Override
	protected void onResume() {
		super.onResume();
		if(mCursor != null)
		{
			mCursor.moveToFirst();
			if(editorState == STATE_EDIT)
			{
				setTitle(getText(R.string.editNoteTitle));
			}
			else if(editorState == STATE_INSERT)
			{
				setTitle(getText(R.string.createNewNoteTitle));
			}
			String note = mCursor.getString(COLUMN_INDEX_NOTE);
			noteText.setTextKeepState(note);
			//Log.i(TAG, "getting the note");
			
			if(noteOriginalContent == null)
			{
				noteOriginalContent = note;
			}
		}
		else
		{
			Log.e(TAG, "error getting the data ");
		}
	}

	@Override
	protected void onPause() 
	{
		super.onPause();
		if(mCursor != null)
		{
			String text = noteText.getText().toString().trim();
			int length = text.length();
			
			if(isFinishing() && (length ==0))
			{
				setResult(RESULT_CANCELED);
				deleteNote();
			}
			else
			{
				ContentValues values = new ContentValues();
				if(!noteOriginalContent.toLowerCase().trim().equalsIgnoreCase(text))
					values.put(Notes.MODIFIED_DATE, System.currentTimeMillis());
				
				if(editorState == STATE_INSERT)
				{
					int titleSize = app.getTitleSize();
					String title = text.substring(0, Math.min(titleSize, length));
					int newLine = title.indexOf("\n");
					if(newLine > 0)
					{
						title = title.substring(0, newLine);
					}
					/*if(length > 30)
					{
						//String title = text.replace("\n", " ").substring(0, Math.min(30, length));
					}*/
					//Log.i(TAG,"editando el titulo " + title);				
					values.put(Notes.TITLE, title);
				}
				values.put(Notes.NOTE, text);
				getContentResolver().update(mUri, values, null, null);
				String saveNoteMessage = (String) getText(R.string.noteSaved);
				Toast.makeText(this, saveNoteMessage, Toast.LENGTH_SHORT).show();
				//Log.i(TAG,"Guardadno la nota...");
				
			}
		}
	}
	
	
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) 
	{
		Log.i(TAG,"the device has rotated...");
		super.onConfigurationChanged(newConfig);
		
		if(editorState == STATE_EDIT)
		{
			((ViewGroup) findViewById(R.id.placeholder)).removeAllViews();
			((ViewGroup) findViewById(R.id.placeholder)).addView((LinearLayout)getLayoutInflater().inflate(R.layout.editnoteactionbar, null));
			//inflate edit menu
		}
		else if(editorState == STATE_INSERT)
		{
			((ViewGroup) findViewById(R.id.placeholder)).removeAllViews();
	        ((ViewGroup) findViewById(R.id.placeholder)).addView((LinearLayout)getLayoutInflater().inflate(R.layout.discardnoteactionbar, null));
		}
	}
	
    private void setAlarm() 
    {
    	Intent reminderIntent = new Intent(SET_REMINDER, getIntent().getData());
    	reminderIntent.putExtra("noteTitle", mCursor.getString(COLUMN_INDEX_TITLE));
    	reminderIntent.putExtra("noteBody", mCursor.getString(COLUMN_INDEX_NOTE));
    	startActivity(reminderIntent);		
	}
	private void shareNote() 
    {
		Intent share = new Intent(android.content.Intent.ACTION_SEND);
		share.setType("text/plain");
		share.putExtra(Intent.EXTRA_TEXT, noteText.getText().toString());
		startActivity(Intent.createChooser(share, "sharing my note"));
	}
	public void actionBarActions(View v)
	{
		switch (v.getId()) 
		{
		case R.id.buttonAppHome:
			String saveNoteMessage = (String) getText(R.string.noteSaved);
			finish();			
			break;
		case R.id.buttonDiscardNote:
			cancelNote();
			tracker.trackEvent(
	                "Edit Note",
	                "Options Menu",
	                "Discard Note",
	                 0);
			break;
		case R.id.buttonEditTitle:
			//Log.i(TAG, "editando titulo " + getIntent().getData());
			startActivity(new Intent(EDIT_TITLE, getIntent().getData()));
			tracker.trackPageView("/NotePad Title Editor");
			break;
		case R.id.buttonDiscardChanges:
			tracker.trackEvent(
	                "Edit Note",
	                "Options Menu",
	                "Discard Changes",
	                 0);
			cancelNote();
			break;
		case R.id.buttonDeleteNote:
			tracker.trackEvent(
	                "Edit Note",
	                "Options Menu",
	                "Delete Note",
	                 0);
			deleteNote();
			finish();
			break;
			case R.id.buttonShareNote:
			tracker.trackEvent(
	                "Edit Note",
	                "Options Menu",
	                "Share Note",
	                 0);
			shareNote();
			break;
		case R.id.buttonSetReminder:
			tracker.trackPageView("/NotePad Reminder");
			setAlarm();
			break;
		default:
			break;
		}
	}
	
	/**
     * Take care of canceling work on a note.  Deletes the note if we
     * had created it, otherwise reverts to the original text.
     */
    private final void cancelNote() 
    {
        if (mCursor != null) 
        {
        	if (editorState == STATE_EDIT) 
            {
                // Put the original note text back into the database
                mCursor.close();
                mCursor = null;
                ContentValues values = new ContentValues();
                values.put(Notes.NOTE, noteOriginalContent);
                getContentResolver().update(mUri, values, null, null);
                //Log.i(TAG, "descartando los cambios en la nota " + noteOriginalContent);
            } 
            else if (editorState == STATE_INSERT) 
            {
				//Log.i(TAG, "descartando la nota recien creada...");
                // We inserted an empty note, make sure to delete it
				deleteNote();
	        }
        }
	    setResult(RESULT_CANCELED);
	    finish();
	}
    
	/**
	 * Take care of deleting a note.  Simply deletes the entry.
	 */
	private final void deleteNote() 
	{
		if (mCursor != null) 
		{
			mCursor.close();
	        mCursor = null;
	        //Log.i(TAG, "deleting note " + mUri);
	        getContentResolver().delete(mUri, null, null);
	        noteText.setText("");
	    }
	}
}