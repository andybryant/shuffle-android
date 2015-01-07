package org.dodgybits.shuffle.android.core.view;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.util.FontUtils;

public class NavDrawerEntityView extends LinearLayout implements NavDrawerEntityListener {
    private static final String TAG = "NavDrawerEntityView";

    private ImageView mIconView;
    private TextView mNameView;
    private TextView mCountView;

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
        mNameView = (TextView) findViewById(R.id.name);
        mCountView = (TextView) findViewById(R.id.count);
        mIconView = (ImageView) findViewById(R.id.icon);
    }

    public void init(final int iconResId, final String name, final Integer count) {
        mNameView.setText(name);
        mCountView.setText(formatCount(count));
        mIconView.setImageResource(iconResId);
    }

    @Override
    public void onUpdateCount(final Integer count) {
        post(new Runnable() {
            public void run() {
                mCountView.setText(formatCount(count));
            }
        });
    }

    @Override
    public void setViewSelected(boolean selected) {
        mNameView.setTextColor(selected ?
                getResources().getColor(R.color.navdrawer_text_color_selected) :
                getResources().getColor(R.color.navdrawer_text_color));
    }

    private String formatCount(Integer count) {
        String value = "";
        if (count != null && count > 0) {
            value = count.toString();
        }
        return value;
    }

}
