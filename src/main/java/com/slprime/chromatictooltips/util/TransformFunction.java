package com.slprime.chromatictooltips.util;

public class TransformFunction {

    public static float linear(float t) {
        return t;
    }

    public static float ease(float t) {
        return t * t * (3 - 2 * t);
    }

    public static float easeIn(float t) {
        return t * t * t;
    }

    public static float easeOut(float t) {
        t = t - 1;
        return t * t * t + 1;
    }

    public static float easeInOut(float t) {
        if (t < 0.5f) {
            return 4 * t * t * t;
        } else {
            t = t - 1;
            return 4 * t * t * t + 1;
        }
    }
}
