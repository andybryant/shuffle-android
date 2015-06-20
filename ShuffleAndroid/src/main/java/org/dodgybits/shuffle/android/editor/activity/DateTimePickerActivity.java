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

    @InjectView(R.id.tomorrow) TextView tomorrow;
    @InjectView(R.id.tomorrow_datetime) private TextView tomorrowDateTime;
    @InjectView(R.id.next_week) private TextView nextWeek;
    @InjectView(R.id.next_week_datetime) private TextView nextWeekDateTime;
    @InjectView(R.id.next_month) private TextView nextMonth;
    @InjectView(R.id.next_month_datetime) private TextView nextMonthDateTime;
    @InjectView(R.id.last_row) private View lastRow;
    @InjectView(R.id.last) private TextView last;
    @InjectView(R.id.last_datetime) private TextView lastDateTime;


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
