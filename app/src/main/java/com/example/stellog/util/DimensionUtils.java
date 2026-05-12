package com.example.stellog.util;

import android.content.res.Resources;

/**
 * 尺寸换算工具。
 */
public final class DimensionUtils {
    private DimensionUtils() {
    }

    public static int dpToPx(Resources resources, int dp) {
        return Math.round(dp * resources.getDisplayMetrics().density);
    }
}
