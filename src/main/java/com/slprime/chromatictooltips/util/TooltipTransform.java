package com.slprime.chromatictooltips.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.lwjgl.opengl.GL11;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.slprime.chromatictooltips.api.TooltipStyle;

public class TooltipTransform {

    public static class Keyframe {

        public static final Keyframe LAST_KEYFRAME = new Keyframe(100, 0f, 0f, 1f, 0f);

        public final int progress;
        public final float translateX;
        public final float translateY;
        public final float scale;
        public final float rotate;

        public Keyframe(int progress, float translateX, float translateY, float scale, float rotate) {
            this.progress = progress;
            this.translateX = translateX;
            this.translateY = translateY;
            this.scale = scale;
            this.rotate = rotate;
        }
    }

    protected static final Map<String, Function<Float, Float>> TIMING_FUNCTIONS = new HashMap<>();

    static {
        TIMING_FUNCTIONS.put("linear", TransformFunction::linear);
        TIMING_FUNCTIONS.put("easeIn", TransformFunction::ease);
        TIMING_FUNCTIONS.put("easeIn", TransformFunction::easeIn);
        TIMING_FUNCTIONS.put("easeOut", TransformFunction::easeOut);
        TIMING_FUNCTIONS.put("easeInOut", TransformFunction::easeInOut);
    }

    protected float delay = 0f;
    protected float duration = 1_000f;
    protected int iterationCount = 1;
    protected Function<Float, Float> timingFunction;
    protected boolean pingPong = false;
    protected List<Keyframe> keyframes = new ArrayList<>();

    public TooltipTransform(TooltipStyle transformStyle) {
        this.delay = transformStyle.getAsFloat("delay", 0);
        this.duration = transformStyle.getAsFloat("duration", 1_000);
        this.iterationCount = transformStyle.getAsInt("iterationCount", 1);
        this.pingPong = transformStyle.getAsBoolean("pingPong", false);
        this.timingFunction = TIMING_FUNCTIONS
            .getOrDefault(transformStyle.getAsString("function", "linear"), TIMING_FUNCTIONS.get("linear"));

        for (JsonElement e : transformStyle.getAsJsonArray("keyframes", new JsonArray())) {
            if (e != null && e.isJsonObject()) {
                final TooltipStyle keyframeStyle = new TooltipStyle(e.getAsJsonObject());

                this.keyframes.add(
                    new Keyframe(
                        keyframeStyle.getAsInt("progress", 0),
                        keyframeStyle.getAsFloat("translateX", 0f),
                        keyframeStyle.getAsFloat("translateY", 0f),
                        keyframeStyle.getAsFloat("scale", 1f),
                        keyframeStyle.getAsFloat("rotate", 0f)));
            }
        }

        if (this.keyframes.isEmpty() || this.keyframes.get(this.keyframes.size() - 1).progress != 100) {
            this.keyframes.add(Keyframe.LAST_KEYFRAME);
        }
    }

    public boolean isAnimated() {
        return this.keyframes.size() > 1;
    }

    public void pushTransformMatrix(double x, double y, double width, double height, long lastFrameTime) {
        GL11.glPushAttrib(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_ENABLE_BIT);
        GL11.glPushMatrix();

        final Keyframe frame = getCurrentKeyframe(lastFrameTime);
        final double halfWidth = width / 2d;
        final double halfHeight = height / 2d;

        if (frame.scale != 1f) {
            GL11.glTranslated(
                x + frame.translateX + halfWidth - halfWidth * frame.scale,
                y + frame.translateY + halfHeight - halfHeight * frame.scale,
                0d);
            GL11.glScalef(frame.scale, frame.scale, 1f);
        } else {
            GL11.glTranslated(x + frame.translateX, y + frame.translateY, 0d);
        }

        if (frame.rotate > 0f) {
            GL11.glTranslated(halfWidth / frame.scale, halfHeight / frame.scale, 0);
            GL11.glRotated(frame.rotate, 0, 0, 1);
            GL11.glTranslated(-halfWidth / frame.scale, -halfHeight / frame.scale, 0);
        }

    }

    public void popTransformMatrix() {
        GL11.glPopMatrix();
        GL11.glPopAttrib();
    }

    public Keyframe getCurrentKeyframe(long lastFrameTime) {
        float time = System.currentTimeMillis() - lastFrameTime - this.delay;

        if (time < 0) {
            return keyframes.get(0);
        }

        time = computeTimeFraction(time);
        Keyframe a = keyframes.get(0);
        Keyframe b = keyframes.get(keyframes.size() - 1);

        for (int i = 0; i < this.keyframes.size() - 1; i++) {
            if (time >= this.keyframes.get(i).progress && time <= this.keyframes.get(i + 1).progress) {
                a = this.keyframes.get(i);
                b = this.keyframes.get(i + 1);
                break;
            }
        }

        float local = (time - a.progress) / ((float) Math.max(1, b.progress - a.progress));

        if (this.timingFunction != null) {
            local = this.timingFunction.apply(local);
        }

        return new Keyframe(
            (int) time,
            lerp(a.translateX, b.translateX, local),
            lerp(a.translateY, b.translateY, local),
            lerp(a.scale, b.scale, local),
            lerp(a.rotate, b.rotate, local));
    }

    private float computeTimeFraction(float time) {
        time = time / this.duration;

        if (this.iterationCount > 0 && time >= this.iterationCount) {
            return this.iterationCount % 2 == 1 ? 100f : 0f;
        }

        final int intTime = (int) time;

        if (this.pingPong && intTime % 2 == 1) {
            time = 1 - (time - intTime);
        } else {
            time = time - intTime;
        }

        return time * 100;
    }

    private float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

}
