/**
 * Copyright (C) 2014 Android Shuffle Open Source Project
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
package org.dodgybits.shuffle.android.core.view;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Outline;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.Button;
import android.widget.Checkable;
import android.widget.FrameLayout;
import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.util.OSUtils;

/**
 * A Floating Action Button view distinguished by a circled
 * icon floating above the UI, with special motion behaviors.
 */
public class FloatingActionButton extends FrameLayout  {

    private static final String TAG = "FloatingActionButton";

    public FloatingActionButton(Context context) {
        this(context, null, 0, 0);
    }

    public FloatingActionButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0, 0);
    }

    public FloatingActionButton(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public FloatingActionButton(Context context, AttributeSet attrs, int defStyleAttr,
                                int defStyleRes) {
        super(context, attrs, defStyleAttr);

        setClickable(true);

        if (OSUtils.atLeastLollipop()) {
            initOutline();
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void initOutline() {
        Log.d(TAG, "setup outline");
        // Set the outline provider for this view. The provider is given the outline which it can
        // then modify as needed. In this case we set the outline to be an oval fitting the height
        // and width.
        setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                final int size = getResources().getDimensionPixelSize(R.dimen.fab_size);
                outline.setOval(0, 0, size, size);
            }
        });

        // Finally, enable clipping to the outline, using the provider we set above
        setClipToOutline(true);
    }

    @SuppressLint("NewApi")
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        Log.d(TAG, "size changed");

        if (OSUtils.atLeastLollipop()) {
            // As we have changed size, we should invalidate the outline so that
            // it resizes to correct size
            invalidateOutline();
        }
    }

}
