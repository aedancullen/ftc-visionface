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

import static android.content.Context.WINDOW_SERVICE;
import static org.opencv.core.Core.FONT_HERSHEY_DUPLEX;


public class VisionProcessor implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = "VisionProcessor";
    private CameraBridgeViewBase cameraView;
    private Activity activity;
    private Context appContext;

    LinearLayout oView;

    private double x;
    private double y;
    private double z;
    private boolean isSeen;
    private Rect[] rawDetections;

    public int capWidth;
    public int capHeight;

    public Mat lastFrame;

    private String diagText = "No ObjectSpec loaded";

    private ObjectSpec object;

    private CameraSpec camera;


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

    public VisionProcessor() {};

    public VisionProcessor(Context context, CameraSpec camera) {
        activity = (Activity)context;
        appContext = context;
        this.camera = camera;

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
                try {
                    cameraView.disableView();

                    //RelativeLayout thingy = (RelativeLayout) activity.findViewById(R.id.RelativeLayout);

                    oView.removeView(cameraView);

                    WindowManager wm = (WindowManager) appContext.getSystemService(WINDOW_SERVICE);
                    wm.removeView(oView);
                }
                catch (Exception e) {
                }

            }
        };

        activity.runOnUiThread(stopper);

    }

    public void onCameraViewStarted(int width, int height) {
        this.capWidth = width;
        this.capHeight = height;
    }

    public void onCameraViewStopped() {
    }

    private String dp(double in) {
        return String.format("%.2f", in);
    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat mRgba = inputFrame.rgba();
        lastFrame = mRgba.clone();

        if (object == null) {
            isSeen = false;
        }
        else{
            MatOfRect detections = new MatOfRect();

            object.cascade.detect(inputFrame.gray(), detections);

            Rect[] locations = detections.toArray();
            rawDetections = locations;

            if (locations.length == 0) {
                isSeen = false;
            }
            else {

                Imgproc.rectangle(mRgba, locations[0].tl(), locations[0].br(), new Scalar(0, 255, 0, 255), 2);

                double cotFov = Math.cos(camera.horizontalFovRadians / 2.0)/Math.sin(camera.horizontalFovRadians / 2.0);
                double distanceAtFullFrame = cotFov * (object.actualWidth / 2.0);

                double widthRatio = capWidth / (double)locations[0].width;
                double objectDistance = distanceAtFullFrame * widthRatio;

                double objectAnglePerPxHoriz = camera.horizontalFovRadians / capWidth;
                double objectAnglePerPxVert = camera.verticalFovRadians / capHeight;

                double objectAngleHoriz = objectAnglePerPxHoriz * (((locations[0].br().x + locations[0].tl().x) / 2.0) - (capWidth / 2.0));
                double objectAngleVert = objectAnglePerPxVert * (((locations[0].br().y + locations[0].tl().y) / 2.0) - (capHeight / 2.0));

                double objectPosX = Math.sin(objectAngleHoriz) * objectDistance;
                double objectPosY = Math.sin(objectAngleVert) * objectDistance;

                this.x = objectPosX;
                this.y = objectPosY;
                this.z = objectDistance;
                this.isSeen = true;

            }
        }
        diagText = "(x:" + dp(x) + " y:" + dp(y) + " z:" + dp(z) + " seen:" + String.valueOf(isSeen) + ")";
        // rofl
        Imgproc.putText(mRgba, diagText, new Point(10, mRgba.height() - 10), FONT_HERSHEY_DUPLEX, 0.8, new Scalar(0, 0, 255, 255));
        Imgproc.putText(mRgba, "EVOLUTION ADVANCED VISION", new Point(10, mRgba.height() / 2), FONT_HERSHEY_DUPLEX, 1.5, new Scalar(255,255,255, 255), 3, Imgproc.LINE_AA, false);
        Imgproc.putText(mRgba, "better than Vuforia", new Point(10, mRgba.height() / 2 + 30), FONT_HERSHEY_DUPLEX, 1, new Scalar(255,255,255, 255), 3, Imgproc.LINE_AA, false);
        return mRgba;
    }

    public double[] getObjectPosition() {
        return new double[]{x,y,z};
    }

    public boolean getIsSeen() {
        return isSeen;
    }

    public Rect[] getRawDetections() {
        return rawDetections;
    }

    public void loadObject(ObjectSpec object) {
        this.object = object;

        if (object == null) {
            diagText = "No ObjectSpec loaded";
        }

    }

}
