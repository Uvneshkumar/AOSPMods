package sh.siava.AOSPMods.myListeners.helper;

import android.graphics.Color;
import android.opengl.Matrix;

import java.util.Arrays;

// https://github.com/ProjectMatrixx/android_frameworks_base/blob/16.0/services/core/java/com/android/server/display/color/DisplayEngineController.java
public final class DisplayEngineController {

    private final float[] mMatrix = new float[16];
    private float mHue = 0.0f;
//    private float mContrast = 1.0f;
//    private float mValue = 1.0f;
//    private float mSaturation = 1.0f;

//    Mode presets {r, g, b, sat, cont, val}
//    private static final int[] X_REALITY_MODE = {252, 227, 228, 278, 260, 264};

    public float[] getMatrix() {
        return Arrays.copyOf(mMatrix, mMatrix.length);
    }

    public void setMatrix(int rgb) {
        Matrix.setIdentityM(mMatrix, 0);
        mMatrix[0] = Color.red(rgb) / 255.0f;
        mMatrix[5] = Color.green(rgb) / 255.0f;
        mMatrix[10] = Color.blue(rgb) / 255.0f;
        applyHue(mMatrix, mHue);
//        applyContrast(mMatrix, mContrast);
//        applyValue(mMatrix, mValue);
//        applySaturation(mMatrix, mSaturation);
    }

    public int getLevel() {
        return 300 + 50;
    }

    public void updateBalance() {
//        int[] mode = X_REALITY_MODE;
        mHue = 0.0f;
//        int mRed = mode[0];
//        int mGreen = mode[1];
//        int mBlue = mode[2];
        int mRed = (int) (255.0 * 0.4);
        int mGreen = (int) (255.0 * 0.4);
        int mBlue = 255;
//        mSaturation = mode[3] / 255.0f;
//        mContrast = mode[4] / 255.0f;
//        mValue = mode[5] / 255.0f;
        setMatrix(Color.rgb(mRed, mGreen, mBlue));
    }

    private void applyHue(float[] matrix, float hue) {
        float angle = hue * (float) Math.PI / 180;
        float cosA = (float) Math.cos(angle);
        float sinA = (float) Math.sin(angle);
        float[] hueMatrix = {0.213f + cosA * 0.787f - sinA * 0.213f, 0.715f - cosA * 0.715f - sinA * 0.715f, 0.072f - cosA * 0.072f + sinA * 0.928f, 0, 0.213f - cosA * 0.213f + sinA * 0.143f, 0.715f + cosA * 0.285f + sinA * 0.140f, 0.072f - cosA * 0.072f - sinA * 0.283f, 0, 0.213f - cosA * 0.213f - sinA * 0.787f, 0.715f - cosA * 0.715f + sinA * 0.715f, 0.072f + cosA * 0.928f + sinA * 0.072f, 0, 0, 0, 0, 1};
        Matrix.multiplyMM(matrix, 0, hueMatrix, 0, matrix, 0);
    }

//    private void applyContrast(float[] matrix, float contrast) {
//        float translate = (1 - contrast) / 2;
//        float[] contrastMatrix = {contrast, 0, 0, 0, 0, contrast, 0, 0, 0, 0, contrast, 0, translate, translate, translate, 1};
//        Matrix.multiplyMM(matrix, 0, contrastMatrix, 0, matrix, 0);
//    }

//    private void applyValue(float[] matrix, float value) {
//        float[] valueMatrix = {value, 0, 0, 0, 0, value, 0, 0, 0, 0, value, 0, 0, 0, 0, 1};
//        Matrix.multiplyMM(matrix, 0, valueMatrix, 0, matrix, 0);
//    }

//    private void applySaturation(float[] matrix, float saturation) {
//        float rw = 0.3086f, gw = 0.6094f, bw = 0.0820f;
//        float invSat = 1.0f - saturation;
//        float R = invSat * rw, G = invSat * gw, B = invSat * bw;
//        float[] saturationMatrix = {R + saturation, G, B, 0, R, G + saturation, B, 0, R, G, B + saturation, 0, 0, 0, 0, 1};
//        Matrix.multiplyMM(matrix, 0, saturationMatrix, 0, matrix, 0);
//    }
}