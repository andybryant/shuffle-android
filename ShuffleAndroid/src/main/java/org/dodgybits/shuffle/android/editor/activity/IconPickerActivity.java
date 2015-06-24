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

package org.dodgybits.shuffle.android.editor.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.util.AnalyticsUtils;
import org.dodgybits.shuffle.android.core.util.FontUtils;
import roboguice.activity.RoboActivity;
import roboguice.inject.InjectView;

public class IconPickerActivity extends RoboActivity implements OnItemClickListener, View.OnClickListener {

    @SuppressWarnings("unused")
	private static final String cTag = "IconPickerActivity";

    public static final String TYPE = "vnd.android.cursor.dir/vnd.dodgybits.icons";

    public static final String ICON_NAME = "iconName";
    public static final String ICON_SET = "iconSet";

    @InjectView(R.id.icon_grid) GridView mGrid;
    @InjectView(R.id.none_button) Button mNoneButton;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setContentView(R.layout.icon_picker);
        mGrid.setAdapter(new IconAdapter(this));
        mGrid.setOnItemClickListener(this);
        mNoneButton.setOnClickListener(this);
        FontUtils.setCustomFont(mNoneButton, getAssets());
    }

    @Override
    protected void onStart() {
        super.onStart();
        AnalyticsUtils.activityStart(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        AnalyticsUtils.activityStop(this);
    }

    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
		Bundle bundle = new Bundle();
		int iconId = (Integer)mGrid.getAdapter().getItem(position);
		String iconName = getResources().getResourceEntryName(iconId);
	    bundle.putString(ICON_NAME, iconName);
	    bundle.putBoolean(ICON_SET, true);
	    Intent mIntent = new Intent();
	    mIntent.putExtras(bundle);
	    setResult(RESULT_OK, mIntent);
		finish();
    }

    @Override
    public void onClick(View v) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(ICON_SET, false);
        Intent mIntent = new Intent();
        mIntent.putExtras(bundle);
        setResult(RESULT_OK, mIntent);
        finish();
    }

    public class IconAdapter extends BaseAdapter {
        public IconAdapter(Context context) {
        	loadIcons();
        }
        
        private int[] mIconIds;
        
        private void loadIcons() {
        	mIconIds = new int[] {
    			R.drawable.accessories_calculator,
                R.drawable.accessories_clock,
    			R.drawable.accessories_text_editor,
                R.drawable.animals_bumble_bee,
                R.drawable.animals_butterfly,
    			R.drawable.applications_accessories,
    			R.drawable.applications_development,
    			R.drawable.applications_games,
    			R.drawable.applications_graphics,
    			R.drawable.applications_internet,
    			R.drawable.ic_drawer_projects,
    			R.drawable.applications_system,
                R.drawable.applications_toys,
                R.drawable.applications_utilities,
                R.drawable.audio_x_generic,
                R.drawable.bookmark,
                R.drawable.call_start,
                R.drawable.camera_photo,
                R.drawable.cat,
                R.drawable.colorize,
                R.drawable.computer,
                R.drawable.configure,
                R.drawable.document_print_2,
                R.drawable.document_properties,
                R.drawable.edit_bomb,
                R.drawable.edit_clear,
                R.drawable.edit_find_9,
                R.drawable.emblem_favorite,
                R.drawable.emblem_generic,
                R.drawable.emblem_important,
                R.drawable.emblem_web_2,
                R.drawable.face_monkey,
                R.drawable.flag_blue,
                R.drawable.flag_green,
                R.drawable.flag_red,
                R.drawable.flag_yellow,
                R.drawable.food_cupcake_iced_with_cherry,
                R.drawable.format_justify_fill,
                R.drawable.format_text_bold_3,
                R.drawable.go_home,
                R.drawable.help,
                R.drawable.light_bulb,
                R.drawable.lock,
    			R.drawable.network_wireless,
    			R.drawable.office_calendar,
                R.drawable.office_chart_pie,
                R.drawable.pictogram_din_w008_electricity,
                R.drawable.phone,
                R.drawable.road_sign_us_stop,
    			R.drawable.ic_drawer_next_actions,
    			R.drawable.system_file_manager,    			
    			R.drawable.system_search,    			
    			R.drawable.system_users,
                R.drawable.thumbnail,
                R.drawable.tools_report_bug,
                R.drawable.transportation_plane_cessna,
    			R.drawable.video_x_generic,
    			R.drawable.weather_showers_scattered,    			
    			R.drawable.x_office_address_book,
    			// new icons
        	};
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView i = new ImageView(IconPickerActivity.this);
            Integer iconId = mIconIds[position];
            i.setImageResource(iconId);
            i.setScaleType(ImageView.ScaleType.CENTER);
            return i;
        }


        public final int getCount() {
            return mIconIds.length;
        }

        public final Object getItem(int position) {
            return mIconIds[position];
        }

        public final long getItemId(int position) {
            return position;
        }
    }

}
