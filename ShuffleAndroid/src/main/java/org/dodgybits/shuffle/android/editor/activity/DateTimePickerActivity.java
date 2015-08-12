package org.dodgybits.shuffle.android.editor.activity;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;
import android.view.View;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.TimePicker;
import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.util.AnalyticsUtils;
import org.dodgybits.shuffle.android.core.util.FontUtils;
import roboguice.activity.RoboActivity;
import roboguice.inject.InjectView;


public class DateTimePickerActivity extends RoboActivity implements View.OnClickListener,
        DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener {
    private static final String TAG = "DateTimePickerActivity";

    public static final String TYPE = "vnd.android.cursor.dir/vnd.dodgybits.datetime";

    public static final String DATETIME_VALUE = "dateTimeValue";
    public static final String TITLE = "title";

    @InjectView(R.id.title) TextView titleView;
    @InjectView(R.id.tomorrow_row) View tomorrowRow;
    @InjectView(R.id.tomorrow_datetime) private TextView tomorrowDateTime;
    @InjectView(R.id.next_week_row) private View nextWeekRow;
    @InjectView(R.id.next_week_datetime) private TextView nextWeekDateTime;
    @InjectView(R.id.next_month_row) private View nextMonthRow;
    @InjectView(R.id.next_month_datetime) private TextView nextMonthDateTime;
    @InjectView(R.id.last_row) private View lastRow;
    @InjectView(R.id.last_datetime) private TextView lastDateTime;
    @InjectView(R.id.pick_row) private View pickRow;
    @InjectView(R.id.none_row) private View noneRow;

    private String title;
    private long tomorrowMillis;
    private long nextWeekMillis;
    private long nextMonthMillis;
    private long lastMillis;
    private Time pickedTime;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setContentView(R.layout.datetime_picker);
        View topView = findViewById(android.R.id.content);
        FontUtils.setCustomFont(topView, getAssets());

        Bundle extras = getIntent().getExtras();
        title = extras == null ? null : extras.getString(TITLE, null);
        lastMillis = extras == null ? 0L : extras.getLong(DATETIME_VALUE, 0L);
        tomorrowMillis = System.currentTimeMillis() + DateUtils.DAY_IN_MILLIS;
        nextWeekMillis = System.currentTimeMillis() + DateUtils.WEEK_IN_MILLIS;
        nextMonthMillis = System.currentTimeMillis() + (31L *  DateUtils.DAY_IN_MILLIS);
        Log.d(TAG, "Received " + lastMillis + " title " + title);
    }

    @Override
    protected void onResume() {
        super.onResume();

        setupView();
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

    private void setupView() {
        if (title != null) {
            titleView.setText(title);
        }
        int flags = DateUtils.FORMAT_SHOW_TIME |
                DateUtils.FORMAT_SHOW_WEEKDAY  |
                DateUtils.FORMAT_ABBREV_WEEKDAY;
        int fullFlags = DateUtils.FORMAT_SHOW_TIME |
                DateUtils.FORMAT_SHOW_WEEKDAY  |
                DateUtils.FORMAT_ABBREV_WEEKDAY |
                DateUtils.FORMAT_SHOW_DATE |
                DateUtils.FORMAT_ABBREV_MONTH;
        tomorrowRow.setOnClickListener(this);
        tomorrowDateTime.setText(DateUtils.formatDateTime(this, tomorrowMillis, flags));
        nextWeekRow.setOnClickListener(this);
        nextWeekDateTime.setText(DateUtils.formatDateTime(this, nextWeekMillis, fullFlags));
        nextMonthRow.setOnClickListener(this);
        nextMonthDateTime.setText(DateUtils.formatDateTime(this, nextMonthMillis, fullFlags));
        if (lastMillis != 0L) {
            lastRow.setVisibility(View.VISIBLE);
            lastRow.setOnClickListener(this);
            lastDateTime.setText(DateUtils.formatDateTime(this, lastMillis, fullFlags));
            noneRow.setVisibility(View.VISIBLE);
        } else {
            lastRow.setVisibility(View.GONE);
            noneRow.setVisibility(View.GONE);
        }
        pickRow.setOnClickListener(this);
        noneRow.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tomorrow_row:
                returnResult(tomorrowMillis);
                break;
            case R.id.next_week_row:
                returnResult(nextWeekMillis);
                break;
            case R.id.next_month_row:
                returnResult(nextMonthMillis);
                break;
            case R.id.last_row:
                returnResult(lastMillis);
                break;
            case R.id.pick_row:
                pickedTime = new Time();
                pickedTime.set(lastMillis);
                if (Time.isEpoch(pickedTime)) {
                    updateToDefault(pickedTime);
                }
                new DatePickerDialog(this, this, pickedTime.year,
                        pickedTime.month, pickedTime.monthDay).show();
                break;
            case R.id.none_row:
                returnResult(0L);
                break;
            default:
                Log.w(TAG, "Unhandled click on view " + v);
        }
    }

    private void returnResult(long result) {
        Bundle bundle = new Bundle();
        bundle.putLong(DATETIME_VALUE, result);
        Intent mIntent = new Intent();
        mIntent.putExtras(bundle);
        setResult(RESULT_OK, mIntent);
        finish();
    }

    private void updateToDefault(Time displayTime) {
        displayTime.setToNow();
        displayTime.second = 0;
        int minute = displayTime.minute;
        if (minute > 0 && minute <= 30) {
            displayTime.minute = 30;
        } else {
            displayTime.minute = 0;
            displayTime.hour += 1;
        }
    }

    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        pickedTime.year = year;
        pickedTime.month = monthOfYear;
        pickedTime.monthDay = dayOfMonth;
        new TimePickerDialog(this, this,
                pickedTime.hour, pickedTime.minute,
                DateFormat.is24HourFormat(this)).show();
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        pickedTime.hour = hourOfDay;
        pickedTime.minute = minute;
        returnResult(pickedTime.normalize(true));
    }

}
