/*
 * Copyright (C) 2009 Android Shuffle Open Source Project
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

package org.dodgybits.shuffle.android.preference.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import org.dodgybits.shuffle.android.core.model.Id;

import java.util.UUID;

public class Preferences {
    private static final String cTag = "Preferences";
    
    
	public static final String FIRST_TIME = "first_time";
	public static final String ANALYTICS_ENABLED = "send_analytics";
	
	public static final String LAST_VERSION = "last_version";

	public static final String TOP_LEVEL_COUNTS_KEY = "top_level_counts";
	public static final String CALENDAR_ID_KEY = "calendar_id";

    public static final String LAST_PERMANENTLY_DELETED_DATE = "last_premanently_deleted_date";

    public static final String SYNC_DEVICE_IDENTITY = "sync_device_identity";
    public static final String SYNC_ENABLED = "sync_enabled";
    public static final String SYNC_ACCOUNT = "sync_account";
    public static final String SYNC_AUTH_TOKEN = "sync_auth_token";
    public static final String SYNC_LAST_SYNC_GAE_DATE = "sync_last_sync_gae_date";
    public static final String SYNC_LAST_SYNC_LOCAL_DATE = "sync_last_sync_local_date";
    public static final String SYNC_LAST_SYNC_FAILURE_DATE = "sync_last_sync_failure_date";
    public static final String SYNC_LAST_SYNC_ID = "sync_last_sync_id";
    public static final String SYNC_COUNT = "sync_count";

    public static final String GCM_REGISTRATION_ID = "gcm_registration_id";

    public static final String WIDGET_QUERY_PREFIX = "widget_query_";
    public static final String WIDGET_PROJECT_ID_PREFIX = "widget_projectId_";
    public static final String WIDGET_CONTEXT_ID_PREFIX = "widget_contextId_";
    
    public static int getLastVersion(Context context) {
        return getSharedPreferences(context).getInt(LAST_VERSION, 0);
    }

	private static SharedPreferences getSharedPreferences(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context);
	}
	
	public static boolean isFirstTime(Context context) {
		return getSharedPreferences(context).getBoolean(FIRST_TIME, true);
	}

    public static boolean isAnalyticsEnabled(Context context) {
        return getSharedPreferences(context).getBoolean(ANALYTICS_ENABLED, true);
    }

    public static boolean isSyncEnabled(Context context) {
        return getSharedPreferences(context).getBoolean(SYNC_ENABLED, false);
    }

    public static String getSyncDeviceIdentity(Context context) {
        String id = getSharedPreferences(context).getString(SYNC_DEVICE_IDENTITY, null);
        if (id == null) {
            id = UUID.randomUUID().toString();
            getEditor(context).putString(SYNC_DEVICE_IDENTITY, id).commit();
        }
        return id;
    }

    public static String getSyncAccount(Context context) {
        return getSharedPreferences(context).getString(SYNC_ACCOUNT, "");
    }

    public static String getLastSyncId(Context context) {
        return getSharedPreferences(context).getString(SYNC_LAST_SYNC_ID, null);
    }

    public static long getLastSyncGaeDate(Context context) {
        return getSharedPreferences(context).getLong(SYNC_LAST_SYNC_GAE_DATE, 0L);
    }

    public static long getLastSyncLocalDate(Context context) {
        return getSharedPreferences(context).getLong(SYNC_LAST_SYNC_LOCAL_DATE, 0L);
    }

    public static long getLastSyncFailureDate(Context context) {
        return getSharedPreferences(context).getLong(SYNC_LAST_SYNC_FAILURE_DATE, 0L);
    }

    public static long getLastPermanentlyDeletedDate(Context context) {
        return getSharedPreferences(context).getLong(LAST_PERMANENTLY_DELETED_DATE, 0L);
    }

    public static String getSyncAuthToken(Context context) {
        return getSharedPreferences(context).getString(SYNC_AUTH_TOKEN, null);
    }

    public static int getSyncCount(Context context) {
        return getSharedPreferences(context).getInt(SYNC_COUNT, 0);
    }

    public static String getGcmRegistrationId(Context context) {
        return getSharedPreferences(context).getString(GCM_REGISTRATION_ID, "");
    }

    public static int[] getTopLevelCounts(Context context) {
		String countString = getSharedPreferences(context).getString(Preferences.TOP_LEVEL_COUNTS_KEY, null);
		int[] result = null;
		if (countString != null && !countString.isEmpty()) {
			String[] counts = countString.split(",");
			result = new int[counts.length];
			for(int i = 0; i < counts.length; i++) {
				result[i] = Integer.parseInt(counts[i]);
			}
		}
		return result;
	}
	
	public static int getCalendarId(Context context) {
        int id = 1;
        String calendarIdStr = getSharedPreferences(context).getString(CALENDAR_ID_KEY, null);
        if (calendarIdStr != null) {
            try {
                id = Integer.parseInt(calendarIdStr, 10);
            } catch (NumberFormatException e) {
                Log.e(cTag, "Failed to parse calendar id: " + e.getMessage());
            }
        }
        return id;
	}
	
	public static String getWidgetQueryKey(int widgetId) {
	    return WIDGET_QUERY_PREFIX + widgetId;
	}

    public static String getWidgetProjectIdKey(int widgetId) {
        return WIDGET_PROJECT_ID_PREFIX + widgetId;
    }

    public static String getWidgetContextIdKey(int widgetId) {
        return WIDGET_CONTEXT_ID_PREFIX + widgetId;
    }
    
	public static String getWidgetQuery(Context context, String key) {
        return getSharedPreferences(context).getString(key, null);
	}
	
    public static Id getWidgetId(Context context, String key) {
        return Id.create(getSharedPreferences(context).getLong(key, 0L));
    }
    
	public static SharedPreferences.Editor getEditor(Context context) {
		return getSharedPreferences(context).edit();
	}

}
