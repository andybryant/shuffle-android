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

import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.util.AnalyticsUtils;
import org.dodgybits.shuffle.android.core.view.TextColours;
import org.dodgybits.shuffle.android.list.view.LabelView;

import roboguice.activity.RoboActivity;
import roboguice.inject.InjectView;

public class DateTimePickerActivity extends RoboActivity {

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setContentView(R.layout.datetime_picker);
        mGrid.setAdapter(new ColourAdaptor(this));
        mGrid.setOnItemClickListener(this);
    }

    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
        Bundle bundle = new Bundle();
        bundle.putString("colour", String.valueOf(position));
        Intent mIntent = new Intent();
        mIntent.putExtras(bundle);
        setResult(RESULT_OK, mIntent);
        finish();
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

    public class ColourAdaptor extends BaseAdapter {
        private TextColours textColours;

        public ColourAdaptor(Context context) {
            textColours = TextColours.getInstance(context);
        }


        public View getView(int position, View convertView, ViewGroup parent) {
            LabelView view;
            if (convertView instanceof LabelView) {
                view = (LabelView)convertView;
            } else {
                view = new LabelView(ColourPickerActivity.this);
                view.setText("Abc");
                view.setGravity(Gravity.CENTER);
            }
            view.setColourIndex(position);
            view.setIcon(null);
            return view;
        }

        public final int getCount() {
            return textColours.getNumColours();
        }

        public final Object getItem(int position) {
            return textColours.getTextColour(position);
        }

        public final long getItemId(int position) {
            return position;
        }
    }

}
