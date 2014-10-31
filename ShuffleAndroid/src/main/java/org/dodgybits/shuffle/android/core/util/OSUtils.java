package org.dodgybits.shuffle.android.core.util;

import android.os.Build;

import java.lang.reflect.Field;

public class OSUtils {

    public static boolean atLeastLollipop() {
        return osAtLeast(Build.VERSION_CODES.LOLLIPOP);
    }

    public static boolean atLeastKitkat() {
        return osAtLeast(Build.VERSION_CODES.KITKAT);
    }

    private static int sVersion = -1;

    private static boolean osAtLeast(int requiredVersion) {
        if (sVersion == -1) {
            try {
                Field field = Build.VERSION.class.getDeclaredField("SDK_INT");
                sVersion = field.getInt(null);
            } catch (Exception e) {
                // ignore exception - field not available
                sVersion = 0;
            }
        }

        return sVersion >= requiredVersion;
    }

    
}
