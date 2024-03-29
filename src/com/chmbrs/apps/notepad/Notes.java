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

import android.net.Uri;
import android.provider.BaseColumns;

public final class Notes implements BaseColumns 
{
	public static final String PROVIDER_NAME = "com.chmbrs.apps.notepad";
    /**
     * The content:// style URL for this table
     */
    public static final Uri CONTENT_URI
            = Uri.parse("content://"+PROVIDER_NAME+"/notes");

    /**
     * The MIME type of {@link #CONTENT_URI} providing a directory of notes.
     */
    public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.chmbrs.note";

    /**
     * The MIME type of a {@link #CONTENT_URI} sub-directory of a single note.
     */
    public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.chmbrs.note";

    /**
     * The default sort order for this table
     */
    public static final String DEFAULT_SORT_ORDER = "modified DESC";
    
    
    /**
     * The note id assigned in the server. Used to sync with remote notes.
     * <P>Type: TEXT</P>
     */
    public static final String SERVER_ID = "serverID";
    
    /**
     * The user Google account used in the application.
     * <P>Type: TEXT</P>
     */
    public static final String USER_ACCOUNT = "userAccount";
    
    /**
     * The in order to sync deleted notes when there is no connection with the server.
     * Just a flag to know which notes need to be deleted in the server once the connection is back.
     * <P>Type: BOOLEAN</P>
     */
    public static final String PENDING_DELETE = "pendingDelete"; 

    /**
     * The title of the note
     * <P>Type: TEXT</P>
     */
    public static final String TITLE = "title";

    /**
     * The note itself
     * <P>Type: TEXT</P>
     */
    public static final String NOTE = "note";

    /**
     * The timestamp for when the note was created
     * <P>Type: INTEGER (long from System.curentTimeMillis())</P>
     */
    public static final String CREATED_DATE = "created_date";

    /**
     * The timestamp for when the note was last modified
     * <P>Type: INTEGER (long from System.curentTimeMillis())</P>
     */
    public static final String MODIFIED_DATE = "modified_date";
}
