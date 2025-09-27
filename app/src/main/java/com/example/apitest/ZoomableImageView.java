package com.example.apitest;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import androidx.appcompat.widget.AppCompatImageView;

/**
 * Custom ImageView that allows the user to pan (move) and pinch-to-zoom (scale) the image.
 */
public class ZoomableImageView extends AppCompatImageView implements View.OnTouchListener {

    // Matrix to handle the transformation (scaling, translating) of the image
    private Matrix matrix = new Matrix();

    // Modes of operation: NONE (waiting), DRAG (moving), ZOOM (scaling)
    private static final int NONE = 0;
    private static final int DRAG = 1;
    private static final int ZOOM = 2;
    private int mode = NONE;

    // Tracking for dragging
    private PointF lastPoint = new PointF();

    // Gesture detectors
    private ScaleGestureDetector mScaleDetector;
    private GestureDetector mGestureDetector;

    public ZoomableImageView(Context context) {
        super(context);
        setup(context);
    }

    public ZoomableImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup(context);
    }

    public ZoomableImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setup(context);
    }

    private void setup(Context context) {
        // Set the scale type to MATRIX so we can control the image transformations
        super.setScaleType(ScaleType.MATRIX);

        // Ensure the view listens to touch events
        this.setOnTouchListener(this);

        // Initialize the Scale Gesture Detector for pinch-to-zoom
        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());

        // Initialize the Simple Gesture Detector for single-touch drag
        mGestureDetector = new GestureDetector(context, new GestureListener());
    }

    // --- Core Touch Listener ---
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        // 1. Process scaling gestures (pinch-to-zoom)
        mScaleDetector.onTouchEvent(event);

        // 2. Process dragging and other single-touch gestures
        mGestureDetector.onTouchEvent(event);

        // 3. Handle primary drag events manually (to avoid conflicts with single tap)
        PointF currentPoint = new PointF(event.getX(), event.getY());

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                // Start drag mode
                matrix.set(getImageMatrix());
                lastPoint.set(currentPoint);
                mode = DRAG;
                break;

            case MotionEvent.ACTION_MOVE:
                if (mode == DRAG) {
                    // Calculate displacement and translate the matrix
                    float dx = currentPoint.x - lastPoint.x;
                    float dy = currentPoint.y - lastPoint.y;
                    matrix.postTranslate(dx, dy);
                    setImageMatrix(matrix);
                    lastPoint.set(currentPoint); // Update last point
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                // Exit drag/zoom mode
                mode = NONE;
                break;
        }

        // Return true to indicate we have consumed the event
        return true;
    }

    // --- Scale Listener (Pinch-to-Zoom Logic) ---
    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        private float currentScale = 1.0f;

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            mode = ZOOM;
            // Get the current scale from the matrix
            float[] values = new float[9];
            matrix.getValues(values);
            currentScale = values[Matrix.MSCALE_X];
            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scaleFactor = detector.getScaleFactor();
            float newScale = currentScale * scaleFactor;

            // Optional: Limit zoom levels (e.g., between 0.5x and 5.0x)
            newScale = Math.max(0.5f, Math.min(newScale, 5.0f));

            float pivotX = detector.getFocusX();
            float pivotY = detector.getFocusY();

            // Apply scale transformation centered at the pinch point
            matrix.postScale(scaleFactor, scaleFactor, pivotX, pivotY);
            setImageMatrix(matrix);
            return true;
        }
    }

    // --- Gesture Listener (Handling Double Tap) ---
    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            // Optional: Implement double-tap to reset or toggle zoom
            matrix.reset();
            setImageMatrix(matrix);
            return true;
        }
    }
}
