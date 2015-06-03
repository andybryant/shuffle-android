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

package org.dodgybits.shuffle.android.core.util;

import org.dodgybits.android.shuffle.R;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.util.Log;

public class AlertUtils {
	private static final String cTag = "AlertUtils";
	
	
	private AlertUtils() {
		//deny
	}
	
	public static void showDeleteGroupWarning(final Context context, 
			final String groupName, final String childName, 
			final int childCount, final OnClickListener buttonListener) {
		CharSequence title = context.getString(R.string.warning_title);
		CharSequence message = context.getString(R.string.delete_warning, 
				groupName.toLowerCase(), childCount, childName.toLowerCase());
		CharSequence deleteButtonText = context.getString(R.string.menu_delete);
		CharSequence cancelButtonText = context.getString(R.string.cancel_button_title);
		OnCancelListener cancelListener = new OnCancelListener() {
			public void onCancel(DialogInterface dialog) {
				Log.d(cTag, "Cancelled delete. Do nothing.");
			}
		};
		Builder builder = new Builder(context);
		builder.setTitle(title).setIcon(R.drawable.ic_warning_black_24dp)
			.setMessage(message)
			.setNegativeButton(cancelButtonText, buttonListener)
			.setPositiveButton(deleteButtonText, buttonListener)
			.setOnCancelListener(cancelListener);
		builder.create().show();
	}
	
	public static void showWarning(final Context context, final String message) {
		CharSequence title = context.getString(R.string.warning_title);
		CharSequence buttonText = context.getString(R.string.ok_button_title);
		Builder builder = new Builder(context);
		builder.setTitle(title).setIcon(R.drawable.ic_warning_black_24dp)
			.setMessage(message)
			.setPositiveButton(buttonText, null);
		builder.create().show();
	}
	
	public static void showFileExistsWarning(final Context context, final String filename,
			final OnClickListener buttonListener, final OnCancelListener cancelListener) {
		CharSequence title = context.getString(R.string.warning_title);
		CharSequence message = context.getString(R.string.warning_filename_exists, filename);
		CharSequence replaceButtonText = context.getString(R.string.replace_button_title);
		CharSequence cancelButtonText = context.getString(R.string.cancel_button_title);

		Builder builder = new Builder(context);
		builder.setTitle(title).setIcon(R.drawable.ic_warning_black_24dp)
			.setMessage(message)
			.setNegativeButton(cancelButtonText, buttonListener)
			.setPositiveButton(replaceButtonText, buttonListener)
			.setOnCancelListener(cancelListener);
		builder.create().show();
	}	
}
