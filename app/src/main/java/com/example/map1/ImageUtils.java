package com.example.map1;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.widget.Toast;

public class ImageUtils {
    public static Bitmap getCircularBitmapFromDrawable(Drawable drawable, Context context, int dpSize, int borderDpWidth,String username) {
        if (drawable == null) {
            System.err.println("Исходный Drawable null.");
            return null;
        }
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        int imageDiameterPx = (int) (dpSize * metrics.density + 0.5f);
        int borderPxWidth = (int) (borderDpWidth * metrics.density + 0.5f);
        if (imageDiameterPx <= 0) {
            System.err.println("Целевой диаметр изображения в пикселях должен быть больше 0.");
            return null;
        }
        int outputSizePx = imageDiameterPx + borderPxWidth;

        if (outputSizePx <= 0) {
            System.err.println("Целевой размер выходного Bitmap должен быть больше 0.");
            return null;
        }
        Bitmap scaledSourceBitmap;
        try {
            scaledSourceBitmap = Bitmap.createBitmap(imageDiameterPx, imageDiameterPx, Bitmap.Config.ARGB_8888);
        } catch (IllegalArgumentException e) {
            System.err.println("Не удалось создать промежуточный Bitmap (" + imageDiameterPx + "x" + imageDiameterPx + "): " + e.getMessage());
            return null;
        } catch (OutOfMemoryError e) {
            System.err.println("Ошибка нехватки памяти при создании промежуточного Bitmap: " + e.getMessage());
            return null;
        }
        Canvas scaleCanvas = new Canvas(scaledSourceBitmap);
        drawable.setBounds(0, 0, imageDiameterPx, imageDiameterPx);
        drawable.draw(scaleCanvas);
        Bitmap outputBitmap;
        try {
            outputBitmap = Bitmap.createBitmap(outputSizePx, outputSizePx, Bitmap.Config.ARGB_8888);
        } catch (IllegalArgumentException e) {
            System.err.println("Не удалось создать конечный Bitmap (" + outputSizePx + "x" + outputSizePx + "): " + e.getMessage());
            if (scaledSourceBitmap != null && !scaledSourceBitmap.isRecycled()) {
                scaledSourceBitmap.recycle();
            }
            return null;
        } catch (OutOfMemoryError e) {
            System.err.println("Ошибка нехватки памяти при создании конечного Bitmap (" + outputSizePx + "x" + outputSizePx + "): " + e.getMessage());
            if (scaledSourceBitmap != null && !scaledSourceBitmap.isRecycled()) {
                scaledSourceBitmap.recycle();
            }
            return null;
        }
        Canvas canvas = new Canvas(outputBitmap);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(0xff424242);
        float center = outputSizePx / 2f;
        float imageRadius = imageDiameterPx / 2f;
        canvas.drawCircle(center, center, imageRadius, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        int left = (outputSizePx - imageDiameterPx) / 2;
        int top = (outputSizePx - imageDiameterPx) / 2;
        Rect destRect = new Rect(left, top, left + imageDiameterPx, top + imageDiameterPx);
        Rect srcRect = new Rect(0, 0, imageDiameterPx, imageDiameterPx);
        canvas.drawBitmap(scaledSourceBitmap, srcRect, destRect, paint);
        if (borderPxWidth > 0) {
            paint.setXfermode(null);
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(Color.BLACK);
            paint.setStrokeWidth(borderPxWidth);
            float borderCircleRadius = imageDiameterPx / 2f;
            canvas.drawCircle(center, center, borderCircleRadius, paint);
        }
        if (username!=null) {
            Paint textPaint = new Paint();
            textPaint.setAntiAlias(true);
            textPaint.setTextAlign(Paint.Align.CENTER);
            if (username.length()==11){
                textPaint.setTextSize(23);
            } else if (username.length()==10) {
                textPaint.setTextSize(25);
            } else if (username.length()==9){
                textPaint.setTextSize(25);
            } else if (username.length()==8){
                textPaint.setTextSize(28);
            } else if (username.length()==7){
                textPaint.setTextSize(33);
            } else if (username.length()==6){
                textPaint.setTextSize(40);
            } else if (username.length()==5){
                textPaint.setTextSize(45);
            } else if (username.length()==4){
                textPaint.setTextSize(50);
            } else if (username.length()<4) {
                textPaint.setTextSize(55);
            }
            Paint.FontMetricsInt fm = textPaint.getFontMetricsInt();
            float textBaselineY = center - ((fm.ascent + fm.descent) / 2f);
            canvas.drawText(username, center, textBaselineY, textPaint);
        }
        if (scaledSourceBitmap != null && !scaledSourceBitmap.isRecycled()) {
            scaledSourceBitmap.recycle();
        }
        return outputBitmap;
    }
}