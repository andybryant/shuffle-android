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

package org.dodgybits.shuffle.android.core.view;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.GradientDrawable.Orientation;

public class DrawableUtils {

	private DrawableUtils() {
		//deny
	}

    public static GradientDrawable createGradient(int colour, Orientation orientation) {
        return createGradient(colour, orientation, 1.03f, 0.97f);
    }

    public static GradientDrawable createGradient(int colour, Orientation orientation, float startOffset, float endOffset) {
		int[] colours = new int[2];
		float[] hsv1 = new float[3];
		float[] hsv2 = new float[3];
		Color.colorToHSV(colour, hsv1);
		Color.colorToHSV(colour, hsv2);
		hsv1[2] *= startOffset;
		hsv2[2] *= endOffset;
		colours[0] = Color.HSVToColor(hsv1);
		colours[1] = Color.HSVToColor(hsv2);
    	return new GradientDrawable(orientation, colours);
    }

}
