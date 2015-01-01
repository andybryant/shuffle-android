package org.dodgybits.shuffle.android.core.view;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.util.FontUtils;

public class NavDrawerEntityView extends LinearLayout implements NavDrawerEntityListener {
    private ImageView mIcon;
    private TextView mName;
    private TextView mCount;

    public NavDrawerEntityView(android.content.Context context) {
        this(context, R.layout.navdrawer_item);
    }

    public NavDrawerEntityView(android.content.Context context, int layoutId) {
        super(context);
        init(context, layoutId);
    }

    public void init(android.content.Context androidContext, int layoutId) {
        LayoutInflater vi = (LayoutInflater)androidContext.
                getSystemService(android.content.Context.LAYOUT_INFLATER_SERVICE);
        ViewGroup view = (ViewGroup) vi.inflate(layoutId, this, true);

        FontUtils.setCustomFont(view, androidContext.getAssets());
        mName = (TextView) findViewById(R.id.name);
        mCount = (TextView) findViewById(R.id.count);
        mIcon = (ImageView) findViewById(R.id.icon);
    }

    public void init(final int iconResId, final String name, final Integer count) {
        mName.setText(name);
        mCount.setText(formatCount(count));
        mIcon.setImageResource(iconResId);
    }

    @Override
    public void onUpdateCount(final Integer count) {
        post(new Runnable() {
            public void run() {
                mCount.setText(formatCount(count));
            }
        });
    }

    private String formatCount(Integer count) {
        String value = "";
        if (count != null && count > 0) {
            value = count.toString();
        }
        return value;
    }

}
