package org.dodgybits.shuffle.android.core.util;

import android.content.AsyncQueryHandler;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.CalendarContract;

public class CalendarUtils {


    private static final String[] CALENDARS_PROJECTION_ICS_PLUS = new String[] {
            BaseColumns._ID,
            "calendar_displayName" // CalendarContract.CalendarColumns.CALENDAR_DISPLAY_NAME
    };

    private static final String CALENDARS_WHERE_ICS_PLUS =
            "calendar_access_level>=500 AND sync_events=1";
//        CalendarColumns.CALENDAR_ACCESS_LEVEL + ">=" +
//        CalendarColumns.CAL_ACCESS_CONTRIBUTOR + " AND " + CalendarColumns.SYNC_EVENTS + "=1";

    private static Uri getCalendarContentUri() {
        return CalendarContract.Calendars.CONTENT_URI;
    }

    private static String[] getCalendarProjection() {
        return CALENDARS_PROJECTION_ICS_PLUS;
    }

    private static String getCalendarWhereClause() {
        return CALENDARS_WHERE_ICS_PLUS;
    }

    public static Uri getEventContentUri() {
        return CalendarContract.Events.CONTENT_URI;
    }

    public static void startQuery(AsyncQueryHandler queryHandler) {
        queryHandler.startQuery(0, null, getCalendarContentUri(),
                getCalendarProjection(),
                getCalendarWhereClause(),
                null /* selection args */,
                null /* use default sort */);
    }

}
