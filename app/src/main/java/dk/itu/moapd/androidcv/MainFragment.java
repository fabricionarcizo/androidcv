package dk.itu.moapd.androidcv;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

public class MainFragment extends Fragment
        implements View.OnTouchListener,
        CameraBridgeViewBase.CvCameraViewListener2 {

    private byte cont;
    private int index;

    private JavaCameraView mOpenCvCameraView;
    private Mat mRgbaImage;

    private BaseLoaderCallback mLoaderCallback;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        cont = 0;
        mOpenCvCameraView = view.findViewById(R.id.camera_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCameraIndex(index);
        mOpenCvCameraView.setCvCameraViewListener(this);

        index = 0;
        Button cameraButton = view.findViewById(R.id.camera_button);
        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                index = index^1;
                mOpenCvCameraView.disableView();
                mOpenCvCameraView.setCameraIndex(index);
                mOpenCvCameraView.enableView();
            }
        });

        mLoaderCallback = new BaseLoaderCallback(getActivity()) {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public void onManagerConnected(int status) {
                if (status == LoaderCallbackInterface.SUCCESS) {
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.setOnTouchListener(MainFragment.this);
                } else {
                    super.onManagerConnected(status);
                }
            }
        };

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug())
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION,
                    getActivity(), mLoaderCallback);
        else
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        cont++;
        cont %= 4;
        return v.performClick();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgbaImage = new Mat(height, width, CvType.CV_8UC4);
    }

    @Override
    public void onCameraViewStopped() {
        mRgbaImage.release();
    }

    @Override
    public Mat onCameraFrame(
            CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        Mat image = inputFrame.rgba();

        if (index == 1)
            Core.flip(image, image, 1);

        if (cont == 1)
            return convertToGrayscale(image);
        else if (cont == 2)
            return convertToBGRA(image);
        else if (cont == 3)
            return convertToCanny(image);

        return image;
    }

    private Mat convertToGrayscale(Mat image) {
        Mat grayscale = new Mat();
        Imgproc.cvtColor(image, grayscale, Imgproc.COLOR_RGBA2GRAY);
        return grayscale;
    }

    private Mat convertToBGRA(Mat image) {
        Mat bgra = new Mat();
        Imgproc.cvtColor(image, bgra, Imgproc.COLOR_RGBA2BGRA);
        return bgra;
    }

    private Mat convertToCanny(Mat image) {
        Mat grayscale = convertToGrayscale(image);

        Mat thresh = new Mat();
        double otsuThresh = Imgproc.threshold(grayscale, thresh,
                0, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);

        Mat canny = new Mat();
        Imgproc.Canny(grayscale, canny, otsuThresh * 0.5, otsuThresh);

        grayscale.release();
        thresh.release();

        return canny;
    }

}
