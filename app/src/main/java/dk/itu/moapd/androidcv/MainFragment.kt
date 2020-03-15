package dk.itu.moapd.androidcv

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_main.*
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc

class MainFragment : Fragment(), View.OnTouchListener, CameraBridgeViewBase.CvCameraViewListener2 {

    private var cont = 0
    private var index = 0

    private var rgbaImage: Mat? = null

    private lateinit var loaderCallback: BaseLoaderCallback

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        camera_view.visibility = SurfaceView.VISIBLE
        camera_view.setCameraIndex(index)
        camera_view.setCvCameraViewListener(this)

        camera_button.setOnClickListener {
            index = index xor 1
            camera_view.disableView()
            camera_view.setCameraIndex(index)
            camera_view.enableView()
        }

        loaderCallback = object : BaseLoaderCallback(activity) {
            override fun onManagerConnected(status: Int) {
                if (status == LoaderCallbackInterface.SUCCESS) {
                    camera_view.enableView()
                    camera_view.setOnTouchListener(this@MainFragment)
                } else
                    super.onManagerConnected(status)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!OpenCVLoader.initDebug())
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION,
                activity, loaderCallback)
        else
            loaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
    }

    override fun onPause() {
        super.onPause()
        if (camera_view != null)
            camera_view.disableView()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (camera_view != null)
            camera_view.disableView()
    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        cont++
        cont %= 4
        return v!!.performClick()
    }

    override fun onCameraViewStarted(width: Int, height: Int) {
        rgbaImage = Mat(height, width, CvType.CV_8UC4)
    }

    override fun onCameraViewStopped() {
        rgbaImage?.release()
    }

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame?): Mat {

        val image = inputFrame?.rgba()

        if (index == 1)
            Core.flip(image, image, 1)

        if (cont == 1)
            return convertToGrayscale(image)
        else if (cont == 2)
            return convertToBGRA(image)
        else if (cont == 3)
            return convertToCanny(image)

        return image!!
    }

    private fun convertToGrayscale(image: Mat?): Mat {
        val grayscale = Mat()
        Imgproc.cvtColor(image, grayscale, Imgproc.COLOR_RGBA2GRAY)
        return grayscale
    }

    private fun convertToBGRA(image: Mat?): Mat {
        val bgra = Mat()
        Imgproc.cvtColor(image, bgra, Imgproc.COLOR_RGBA2BGRA)
        return bgra
    }

    private fun convertToCanny(image: Mat?): Mat {
        val grayscale = convertToGrayscale(image)

        val thresh = Mat()
        val otsuThresh = Imgproc.threshold(grayscale, thresh,
            0.0, 255.0, Imgproc.THRESH_BINARY or Imgproc.THRESH_OTSU)

        val canny = Mat()
        Imgproc.Canny(grayscale, canny, otsuThresh * 0.5, otsuThresh)

        grayscale.release()
        thresh.release()

        return canny
    }

}
