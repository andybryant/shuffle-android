package org.dodgybits.shuffle.android.core.util;

import android.content.AsyncQueryHandler;
import android.net.Uri;

public class CalendarUtils {


    // We can't use the constants from the provider since it's not a public portion of the SDK.

    private static final Uri CALENDAR_CONTENT_URI_FROYO_PLUS =
        Uri.parse("content://com.android.calendar/calendars"); // Calendars.CONTENT_URI

    private static final String[] CALENDARS_PROJECTION_ICS_PLUS = new String[] {
            "_id", // BaseColumns._ID,
            "calendar_displayName" //CalendarColumns.CALENDAR_DISPLAY_NAME
    };

    private static final String CALENDARS_WHERE_ICS_PLUS =
            "calendar_access_level>=500 AND sync_events=1";
//        CalendarColumns.CALENDAR_ACCESS_LEVEL + ">=" +
//        CalendarColumns.CAL_ACCESS_CONTRIBUTOR + " AND " + CalendarColumns.SYNC_EVENTS + "=1";


    private static final Uri EVENT_CONTENT_URI_FROYO_PLUS =
        Uri.parse("content://com.android.calendar/events"); // Calendars.CONTENT_URI

    public static final String EVENT_BEGIN_TIME = "beginTime"; // android.provider.Calendar.EVENT_BEGIN_TIME
    public static final String EVENT_END_TIME = "endTime"; // android.provider.Calendar.EVENT_END_TIME
    
    private static Uri getCalendarContentUri() {
        return CALENDAR_CONTENT_URI_FROYO_PLUS;
    }

    private static String[] getCalendarProjection() {
        return CALENDARS_PROJECTION_ICS_PLUS;
    }

    private static String getCalendarWhereClause() {
        return CALENDARS_WHERE_ICS_PLUS;
    }

    public static Uri getEventContentUri() {
        return EVENT_CONTENT_URI_FROYO_PLUS;
    }

    public static void startQuery(AsyncQueryHandler queryHandler) {
        queryHandler.startQuery(0, null, getCalendarContentUri(),
                CalendarUtils.getCalendarProjection(),
                CalendarUtils.getCalendarWhereClause(),
                null /* selection args */, null /* use default sort */);
    }

}
