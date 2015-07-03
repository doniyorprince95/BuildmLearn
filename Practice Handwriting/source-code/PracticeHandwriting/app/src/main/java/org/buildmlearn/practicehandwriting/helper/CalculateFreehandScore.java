package org.buildmlearn.practicehandwriting.helper;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.os.AsyncTask;
import android.widget.TextView;

import com.software.shell.fab.ActionButton;

import org.buildmlearn.practicehandwriting.R;
import org.buildmlearn.practicehandwriting.activities.SplashActivity;

import java.util.ArrayList;

public class CalculateFreehandScore extends AsyncTask<Void,Void,float[]> {
    private Bitmap mTouchImg, mSavedImg;
    private ArrayList<ArrayList<Point>> mTouches;
    private ProgressDialog mProgressDialog;
    private DrawingView mDrawView;
    private Context mContext;
    private String mPracticeString;
    private int[] mTouchBounds;

    public CalculateFreehandScore(Context context, DrawingView drawView, String practiceString) {
        mContext = context;
        mDrawView = drawView;
        mPracticeString = practiceString;
    }

    @Override
    protected void onPreExecute() {
        mDrawView.canDraw(false);
        mDrawView.canVibrate(false);
        mTouchImg = mDrawView.getTouchesBitmap();
        mTouches = mDrawView.getTouchesList();
        mTouchBounds = mDrawView.getTouchBounds();

        mDrawView.clearCanvas();
        mDrawView.init();
        mDrawView.setBitmapFromText(mPracticeString);

        mDrawView.setDrawingCacheEnabled(true);
        mSavedImg = Bitmap.createBitmap(mDrawView.getDrawingCache());
        mDrawView.destroyDrawingCache();

        mProgressDialog = new ProgressDialog(mContext);
        mProgressDialog.setTitle("Please wait ...");
        mProgressDialog.setMessage("Calculating...");
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();
    }

    @Override
    protected float[] doInBackground(Void... voids) {
        return scoreBitmapForFreehand(mTouches, mSavedImg, (mSavedImg.getWidth() - mTouchImg.getWidth())/2, (mSavedImg.getHeight() - mTouchImg.getHeight())/2);
    }

    @Override
    protected void onPostExecute(float[] result) {
        mProgressDialog.dismiss();
        float best = SplashActivity.mDbHelper.getScore(mPracticeString);
        if (best < result[0]) {
            best = result[0];
            SplashActivity.mDbHelper.writeScore(mPracticeString,best);
        }
        mDrawView.clearCanvas();
        mDrawView.init();
        mDrawView.setBitmap(bitmapOverlay(mSavedImg, scaleBitmap(mTouchImg, result[1], result[2]), (int) result[3], (int) result[4]));
        mDrawView.startAnimation(Animator.createScaleDownAnimation());

        ((TextView) ((Activity) mContext).findViewById(R.id.score_and_timer_View)).setText("Score: " + String.valueOf(result[0]));
        ((Activity) mContext).findViewById(R.id.score_and_timer_View).setAnimation(Animator.createFadeInAnimation());
        ((TextView) ((Activity) mContext).findViewById(R.id.best_score_View)).setText("Best: " + String.valueOf(best));
        ((Activity) mContext).findViewById(R.id.best_score_View).setAnimation(Animator.createFadeInAnimation());

        Animator.createYFlipForwardAnimation(((Activity) mContext).findViewById(R.id.done_save_button));
        ((ActionButton) ((Activity) mContext).findViewById(R.id.done_save_button)).setImageResource(R.drawable.ic_save);
        Animator.createYFlipBackwardAnimation(((Activity) mContext).findViewById(R.id.done_save_button));
    }

    private float[] scoreBitmapForFreehand(ArrayList<ArrayList<Point>> touches, Bitmap canvasBitmap, int centerX, int centerY) {
        ArrayList<Point> points = new ArrayList<>();
        for(int i = 0; i < touches.size() ; i++)
            for (int j = 0; j < touches.get(i).size(); j++)
                points.add(touches.get(i).get(j));
        float size = points.size();
        if(size!=0) {
            float correctTouches;
            int i, cx, cy;
            float scaleX, scaleY;
            int bgColour = mContext.getResources().getColor(R.color.AppBg);
            float score, maxScore = 0, scaleXForMaxScore = 1, scaleYForMaxScore = 1, cxForMaxScore = centerX, cyForMaxScore = centerY;
            int[] xTouches = new int[(int)size];
            int[] yTouches = new int[(int)size];

            for (i = 0; i < size; i++) {
                xTouches[i] = (points.get(i).x * mDrawView.getBitmapWidth() / mDrawView.mWidth) - mTouchBounds[0];
                yTouches[i] = (points.get(i).y * mDrawView.getBitmapHeight() / mDrawView.mHeight) - mTouchBounds[1];
            }

            outerLoop:
            for (scaleX = 0.8f; scaleX < 1.4f; scaleX += 0.1f)
                for (scaleY = 0.8f; scaleY < 1.4f; scaleY += 0.1f) {
                    for (cx = centerX - 20; cx <= centerX + 20; cx += 2)
                        for (cy = centerY - 20; cy <= centerY + 20; cy += 2) {
                            correctTouches = 0;
                            for (i = 0; i < size; i++) {
                                int x = (int) (xTouches[i] * scaleX) + cx;
                                int y = (int) (yTouches[i] * scaleY) + cy;

                                if (x >= 0 && x < canvasBitmap.getWidth() && y >= 0 && y < canvasBitmap.getHeight() && canvasBitmap.getPixel(x, y) != bgColour)
                                    correctTouches += 1;
                            }
                            score = correctTouches / size;
                            if (score > maxScore) {
                                maxScore = score;
                                scaleXForMaxScore = scaleX;
                                scaleYForMaxScore = scaleY;
                                cxForMaxScore = cx;
                                cyForMaxScore = cy;
                                if (score == 1)
                                    break outerLoop;
                            }
                        }
                }
            return new float[]{100 * maxScore, scaleXForMaxScore, scaleYForMaxScore, cxForMaxScore, cyForMaxScore};
        } else {
            return new float[]{0, 1, 1, centerX, centerY};
        }
    }

    private Bitmap scaleBitmap(Bitmap originalImage, float scaleX, float scaleY) {
        float width = originalImage.getWidth() * scaleX;
        float height = originalImage.getHeight() * scaleY;

        Bitmap background = Bitmap.createBitmap((int) width, (int) height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(background);

        float xTranslation = 0.0f, yTranslation = (height - originalImage.getHeight() * scaleY)/2.0f;
        Matrix transformation = new Matrix();
        transformation.postTranslate(xTranslation, yTranslation);
        transformation.preScale(scaleX, scaleY);

        Paint paint = new Paint();
        paint.setFilterBitmap(true);

        canvas.drawBitmap(originalImage, transformation, paint);
        return background;
    }

    private Bitmap bitmapOverlay(Bitmap bitmap1, Bitmap bitmap2, int xOffset, int yOffset) {
        Bitmap resultBitmap = Bitmap.createBitmap(bitmap1.getWidth(), bitmap1.getHeight(), Bitmap.Config.ARGB_8888);

        Canvas c = new Canvas(resultBitmap);
        c.drawBitmap(bitmap1, 0, 0, null);

        Paint p = new Paint();
        p.setAlpha(255);

        c.drawBitmap(bitmap2, xOffset, yOffset, p);
        return resultBitmap;
    }
}