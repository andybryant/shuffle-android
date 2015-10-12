package org.dodgybits.shuffle.android.widget;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.widget.RelativeLayout;
import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.model.Context;
import org.dodgybits.shuffle.android.core.view.DrawableUtils;
import org.dodgybits.shuffle.android.core.view.TextColours;

import java.util.HashMap;
import java.util.List;

public class ContextBitmapProvider {
    private static final Bitmap sEmptyBitmap = Bitmap.createBitmap(8, 40, Bitmap.Config.ARGB_8888);

    private HashMap<Integer, Bitmap> mGradientCache;
    private TextColours mColours;
    private android.content.Context mAndroidContext;

    public ContextBitmapProvider(android.content.Context androidContext) {
        mAndroidContext = androidContext;
        mColours = TextColours.getInstance(androidContext);
        mGradientCache = new HashMap<>(mColours.getNumColours());
    }

    public Bitmap getBitmapForContexts(List<Context> contexts) {
        Bitmap gradientBitmap = sEmptyBitmap;
        final int contextCount = contexts.size();
        int radius = mAndroidContext.getResources().getDimensionPixelSize(R.dimen.context_widget_corner_radius);
        if (contextCount > 0) {
            if (contextCount == 1) {
                // special case for 1 - show gradient and cache
                int colourIndex = contexts.get(0).getColourIndex();
                gradientBitmap = mGradientCache.get(colourIndex);
                if (gradientBitmap == null) {
                    int colour = mColours.getBackgroundColour(colourIndex);
                    GradientDrawable drawable = DrawableUtils.createGradient(colour, GradientDrawable.Orientation.TOP_BOTTOM);
                    drawable.setCornerRadius(radius);
                    gradientBitmap = createFromDrawable(drawable);
                    mGradientCache.put(colourIndex, gradientBitmap);
                }
            } else {
                int[] backgroundColors = new int[contextCount];
                for (int i = 0; i < contextCount; i++) {
                    int colourIndex = contexts.get(i).getColourIndex();
                    backgroundColors[i] = mColours.getBackgroundColour(colourIndex);
                }
                GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, backgroundColors);
                drawable.setCornerRadius(radius);
                gradientBitmap = createFromDrawable(drawable);
            }
        }
        return gradientBitmap;
    }

    private Bitmap createFromDrawable(Drawable drawable) {
        Bitmap bitmap = Bitmap.createBitmap(16, 80, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        RelativeLayout l = new RelativeLayout(mAndroidContext);
        l.setBackgroundDrawable(drawable);
        l.layout(0, 0, 16, 80);
        l.draw(canvas);
        return Bitmap.createBitmap(bitmap, 6, 0, 10, 80);
    }
}
