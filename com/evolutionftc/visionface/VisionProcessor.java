package com.evolutionftc.visionface;


import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.PixelFormat;
import android.util.Log;
import android.view.WindowManager;
import android.widget.LinearLayout;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static android.content.Context.WINDOW_SERVICE;
import static org.opencv.core.Core.FILLED;
import static org.opencv.core.Core.FONT_HERSHEY_DUPLEX;


public class VisionProcessor implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = "VisionProcessor";
    private CameraBridgeViewBase cameraView;
    private Activity activity;
    private Context appContext;

    LinearLayout oView;

    private Rect[] locations;

    private String cascadeName = "No cascade";

    private DetectionBasedTracker cascade;

    private BaseLoaderCallback loaderCallback = new BaseLoaderCallback(activity) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.d(TAG, "OpenCV loaded successfully");

                    System.loadLibrary("detection_based_tracker");

                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public VisionProcessor(Context context) {
        activity = (Activity)context;
        appContext = context;

        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization of 3.3.0");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_3_0, activity, loaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            loaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }

    }

    public void start() {

        final VisionProcessor vp = this;

        Runnable starter = new Runnable() {
            public void run() {

                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

                cameraView = new JavaCameraView(activity, 0);
                cameraView.setCvCameraViewListener(vp);
                cameraView.enableFpsMeter();

                //RelativeLayout thingy = (RelativeLayout) activity.findViewById(R.id.RelativeLayout);


                oView = new LinearLayout(appContext);
                WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                        0 | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                        PixelFormat.TRANSLUCENT);
                WindowManager wm = (WindowManager) appContext.getSystemService(WINDOW_SERVICE);
                wm.addView(oView, params);


                oView.addView(cameraView);

                cameraView.enableView();
            }
        };

        activity.runOnUiThread(starter);

    }

    public void stop() {

        final VisionProcessor vp = this;

        Runnable stopper = new Runnable() {
            public void run() {

                cameraView.disableView();

                //RelativeLayout thingy = (RelativeLayout) activity.findViewById(R.id.RelativeLayout);

                oView.removeView(cameraView);

                WindowManager wm = (WindowManager) appContext.getSystemService(WINDOW_SERVICE);
                wm.removeView(oView);

            }
        };

        activity.runOnUiThread(stopper);

    }

    public void onCameraViewStarted(int width, int height) {
    }

    public void onCameraViewStopped() {
    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat mRgba = inputFrame.rgba();

        if (cascade == null) {
        }
        else{
            MatOfRect detections = new MatOfRect();

            cascade.detect(inputFrame.gray(), detections);

            locations = detections.toArray();

            for (int i = 0; i < locations.length; i++)
                Imgproc.rectangle(mRgba, locations[i].tl(), locations[i].br(), new Scalar(0, 255, 0, 255), 2);

        }
        // rofl
        Imgproc.putText(mRgba, cascadeName, new Point(10, mRgba.height() - 10), FONT_HERSHEY_DUPLEX, 0.8, new Scalar(0, 0, 255, 255));
        Imgproc.putText(mRgba, "EVOLUTION FTC VISION", new Point(10, mRgba.height() / 2), FONT_HERSHEY_DUPLEX, 2, new Scalar(255,255,255, 255), 3, Imgproc.LINE_AA, false);
        Imgproc.putText(mRgba, "CASCADE CLASSIFIER", new Point(10, mRgba.height() / 2 + 30), FONT_HERSHEY_DUPLEX, 1, new Scalar(255,255,255, 255), 3, Imgproc.LINE_AA, false);
        return mRgba;
    }

    public Rect[] getLocations() {
        return locations;
    }

    public void loadCascade(String name) {
        cascadeName = "Tracking: " + name;

        try {

            InputStream is = appContext.getResources().openRawResource(
                    appContext.getResources().getIdentifier(name, "raw", appContext.getPackageName()));

            File cascadeDir = appContext.getDir("cascade", Context.MODE_PRIVATE);
            File cascadeFile = new File(cascadeDir, "current-cascade.xml");
            FileOutputStream os = new FileOutputStream(cascadeFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();

            cascade = new DetectionBasedTracker(cascadeFile.getAbsolutePath(), 0);

            Log.d(TAG, "Load cascade " + name + " done");

        }
        catch (IOException e) {
            throw new IllegalStateException("Problem loading cascade: " + e);
        }
    }

}
