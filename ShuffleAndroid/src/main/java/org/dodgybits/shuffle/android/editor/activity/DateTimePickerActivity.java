package org.dodgybits.shuffle.android.editor.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;

import android.widget.TextView;
import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.util.AnalyticsUtils;
import org.dodgybits.shuffle.android.core.util.FontUtils;
import org.dodgybits.shuffle.android.core.view.TextColours;
import org.dodgybits.shuffle.android.list.view.LabelView;

import roboguice.activity.RoboActivity;
import roboguice.inject.InjectView;

public class DateTimePickerActivity extends RoboActivity {

    private TextView tomorrow;
    private TextView tomorrowDateTime;
    private TextView nextWeek;
    private TextView nextWeekDateTime;
    private TextView nextMonth;
    private TextView nextMonthDateTime;
    private TextView last;
    private TextView lastDateTime;


    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setContentView(R.layout.datetime_picker);
        View topView = findViewById(android.R.id.content);
        FontUtils.setCustomFont(topView, getAssets());
    }

    @Override
    protected void onResume() {
        super.onResume();


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


}
