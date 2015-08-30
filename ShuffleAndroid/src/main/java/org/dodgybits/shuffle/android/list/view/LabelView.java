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

package org.dodgybits.shuffle.android.list.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.GradientDrawable.Orientation;
import android.util.AttributeSet;
import android.widget.TextView;

import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.view.DrawableUtils;
import org.dodgybits.shuffle.android.core.view.TextColours;

/**
 * A TextView with coloured text and a round edged coloured background.
 */
public class LabelView extends TextView {
	protected TextColours mTextColours;
	protected Drawable mIcon;
	protected int mTextColour;
	protected int mBgColour;
	
	public LabelView(Context context) {
		super(context);
		init(context);
	}
	
    public LabelView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public LabelView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}

    private void init(Context context) {
		mTextColours = TextColours.getInstance(context);
        int horizontalPadding = getResources().getDimensionPixelSize(R.dimen.context_small_horizontal_padding);
        int verticalPadding = getResources().getDimensionPixelSize(R.dimen.context_small_vertical_padding);
        int iconPadding = getResources().getDimensionPixelSize(R.dimen.context_small_icon_padding);
        setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding);
        setCompoundDrawablePadding(iconPadding);
    }
    
    public void setColourIndex(int colourIndex) {
    	mTextColour = mTextColours.getTextColour(colourIndex);
    	mBgColour = mTextColours.getBackgroundColour(colourIndex);
		setTextColor(mTextColour);
		GradientDrawable drawable = DrawableUtils.createGradient(mBgColour, Orientation.TOP_BOTTOM, 1f, 1f);
        int radius = getResources().getDimensionPixelSize(R.dimen.context_small_corner_radius);
    	drawable.setCornerRadius(radius);
    	setBackgroundDrawable(drawable);
    }

    public void setIcon(Drawable icon) {
    	mIcon = icon;
		setCompoundDrawablesWithIntrinsicBounds(mIcon, null, null, null);
    }
    
    
}
