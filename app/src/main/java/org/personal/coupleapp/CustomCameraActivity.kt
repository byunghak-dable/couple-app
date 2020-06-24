package org.personal.coupleapp

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_custom_camera.*
import org.opencv.android.*
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfRect
import org.opencv.core.Size
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.CascadeClassifier
import org.personal.coupleapp.adapter.FilterAdapter
import org.personal.coupleapp.data.FilterData
import org.personal.coupleapp.utils.singleton.ImageEncodeHelper
import java.io.File
import java.io.FileOutputStream
import java.util.*
import kotlin.collections.ArrayList

class CustomCameraActivity : AppCompatActivity(), CameraBridgeViewBase.CvCameraViewListener2,
    View.OnClickListener, FilterAdapter.ItemClickListener, SeekBar.OnSeekBarChangeListener {

    private val TAG: String = "CameraActivity"

    private val PERMISSION_REQUEST_CODE: Int = 1000

    // 추후에 사용할 변수들을 kotlin 에서는 다음과 같이 정의
    private lateinit var matInput: Mat
    private lateinit var matResult: Mat
    private lateinit var sunglasses: Mat
    private var handler: Handler = Handler()
    private var thresholds: Int = 50

    // 카메라로 캡처한 이미지를 저장하기 전 볼 수 있도록 bitmap
    private lateinit var capturedImage: Bitmap

    // 얼굴인식을 위한 OpenCV 파일
    private lateinit var faceDetector: CascadeClassifier
    private lateinit var eyesDetector: CascadeClassifier

    // 필터의 번호를 지정하여 지정된 번호에 따른 필터를 native-lib 메소드에서 바꿔주도록 한다.
    private var filter = 0

    // 자바에서 native 와 같이 외부 메소드를 사용하기 위해 kotlin 에서는 external 을 사용한다.
    private external fun convertRGB(matAddrInput: Long, matAddrResult: Long, filter: Int, thresholds: Int)

    private external fun overlayImage(matAddrInput: Long, matAddrImage: Long, positionX: Double, positionY: Double)

    init {

        System.loadLibrary("opencv_java4")
        System.loadLibrary("native-lib")
    }

    // Memo : 코틀린은 추상클래스를 객체화할 때 다음과 같은 방법으로 객체화 시킨다.
    private val loaderCallback = object : BaseLoaderCallback(this) {

        override fun onManagerConnected(status: Int) {
            super.onManagerConnected(status)

            when (status) {

                LoaderCallbackInterface.SUCCESS -> {

                    readCascadeFile()
                    cameraPreview.enableView()
                }

                else -> super.onManagerConnected(status)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_custom_camera)

        cameraPreview.setCameraIndex(0)
        setListener()
        buildRecyclerView()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onStart() {
        super.onStart()

        permissionCheck()
    }

    override fun onResume() {
        super.onResume()

        loadOpenCV()
    }

    override fun onPause() {
        super.onPause()

        if (cameraPreview != null) {

            cameraPreview.disableView()
        }
    }

    override fun onBackPressed() {

        when {

            photoPreview.visibility == View.VISIBLE -> photoPreview.visibility = View.GONE

            filterRV.visibility == View.VISIBLE -> filterRV.visibility = View.GONE

            else -> super.onBackPressed()
        }
    }

    // Memo : CameraViewListener2 인터페이스의 메소드들로 카메라가 찍은 프레임을 Mat 객체에 담아 프리뷰를 보여준다.
    // -------------------------------------------------------------------------------------------------
    override fun onCameraViewStarted(width: Int, height: Int) {

        matInput = Mat(width, height, CvType.CV_8UC4)
        matResult = Mat(width, height, CvType.CV_8SC4)
    }

    override fun onCameraViewStopped() {

        matInput.release()
        matResult.release()
    }

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame?): Mat {

        matInput = inputFrame!!.rgba()

        if (filter < 7) {

            controlVisibility(thresholdSB, View.GONE)
            convertRGB(matInput.nativeObjAddr, matResult.nativeObjAddr, filter, 0)


        } else if (filter == 7) {

            controlVisibility(thresholdSB, View.VISIBLE)
            thresholdSB.max = 100
            thresholdSB.setOnSeekBarChangeListener(this)
            thresholdSB.progress = thresholds
            convertRGB(matInput.nativeObjAddr, matResult.nativeObjAddr, filter, thresholds)

        } else {

            controlVisibility(thresholdSB, View.GONE)
            detectFace()
        }

        return matResult
    }

    private fun controlVisibility(view: View, visibility: Int) {

        handler.post {

            view.visibility = visibility
        }
    }

    private fun detectFace() {

        val faceDetection = MatOfRect()

        faceDetector.detectMultiScale(matInput, faceDetection, 1.3, 2, 0, Size(40.0, 40.0))

        faceDetection.toArray().forEach { faceRect ->

            val resizedSunglasses = Mat()
            val imageScale: Double = faceRect.width.toDouble() / sunglasses.width()
            val resizedWidth = sunglasses.cols() * imageScale
            val resizedHeight = sunglasses.rows() * imageScale

            val positionX = faceRect.x.toDouble()
            val positionY = faceRect.y + faceRect.height.toDouble() / 3

            Imgproc.resize(sunglasses, resizedSunglasses, Size(resizedWidth, resizedHeight))


            overlayImage(matInput.nativeObjAddr, resizedSunglasses.nativeObjAddr, positionX, positionY)
        }

        matInput.copyTo(matResult)
    }

    // -------------------------------------------------------------------------------------------------

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onClick(v: View?) {
        when (v?.id) {

            R.id.takePictureBtn -> takePicture()
            R.id.filterBtn -> showFilterList()
            R.id.saveBtn -> saveImage()
        }
    }

    override fun onItemClick(itemPosition: Int) {

        filter = itemPosition
    }

    private fun takePicture() {

        val bitmapOutput =
            Bitmap.createBitmap(matResult.cols(), matResult.rows(), Bitmap.Config.ARGB_8888)
        val matrix = Matrix()

        Utils.matToBitmap(matResult, bitmapOutput)
        matrix.postRotate(90f)
        capturedImage = Bitmap.createBitmap(bitmapOutput, 0, 0, bitmapOutput.width, bitmapOutput.height, matrix, true)

        showPhotoPreview()
    }

    private fun showFilterList() {

        filterRV.visibility = View.VISIBLE
    }

    private fun saveImage() {
        val toAlbumGallery = Intent()
        ImageEncodeHelper.bitmapList.clear()
        ImageEncodeHelper.bitmapList.add(capturedImage)
        setResult(RESULT_OK, toAlbumGallery)
        finish()
    }

    private fun showPhotoPreview() {

        photoPreview.visibility = View.VISIBLE
        photoPreviewIV.setImageBitmap(capturedImage)
    }

    // 캡처한 이미지 저장하는 메소드

    private fun loadOpenCV() {

        if (!OpenCVLoader.initDebug()) {

            Log.i(TAG, "onResume : Internal OpenCV library not found")
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, loaderCallback)

        } else {

            Log.i(TAG, "onResume : OpenCV library found inside package. and using it")
            loaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun permissionCheck() {

        var isPermission = true

        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {

            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {

                if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {

                    val permissionArray = arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    requestPermissions(permissionArray, PERMISSION_REQUEST_CODE)
                    isPermission = false
                }
            }
        }

        if (isPermission) {

            onPermissionGranted()
        }
    }

    // 카메라 및 저장소 권한 요청 결과 콜백 메소드
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (grantResults.isNotEmpty()) {

            if (requestCode == PERMISSION_REQUEST_CODE) {

                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    if (grantResults[1] == PackageManager.PERMISSION_GRANTED) {

                        if (grantResults[2] == PackageManager.PERMISSION_GRANTED) {

                            onPermissionGranted()

                        } else {

                            Toast.makeText(this, "파일 저장소 쓰기 권한을 거부하셨습니다.", Toast.LENGTH_SHORT).show()
                        }
                    } else {

                        Toast.makeText(this, "파일 저장소 일기 권한을 거부하셨습니다.", Toast.LENGTH_SHORT).show()
                    }
                } else {

                    Toast.makeText(this, "카메라 권한을 거부하셨습니다", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun onPermissionGranted() {

        val cameraViews: List<CameraBridgeViewBase?> = getCameraViewList()

        for (cameraBridgeViewBase in cameraViews) {

            cameraBridgeViewBase?.setCameraPermissionGranted()
        }
    }

    private fun getCameraViewList(): List<CameraBridgeViewBase?> {

        return Collections.singletonList(cameraPreview)
    }

    private fun setListener() {

        cameraPreview.setCvCameraViewListener(this)
        takePictureBtn.setOnClickListener(this)
        filterBtn.setOnClickListener(this)
        saveBtn.setOnClickListener(this)
    }

    // 필터 리스트를 보여주는 리사이클러 뷰
    private fun buildRecyclerView() {

        val filterList = ArrayList<FilterData>()
        filterList.add(FilterData(R.drawable.filter_normal, "Normal"))
        filterList.add(FilterData(R.drawable.filter_gray, "Gray"))
        filterList.add(FilterData(R.drawable.filter_luv, "LUV"))
        filterList.add(FilterData(R.drawable.filter_bgr, "BGR"))
        filterList.add(FilterData(R.drawable.filter_hls, "HLS"))
        filterList.add(FilterData(R.drawable.filter_hsv, "HSV"))
        filterList.add(FilterData(R.drawable.filter_blur, "Blur"))
        filterList.add(FilterData(R.drawable.filter_canny, "Canny"))
        filterList.add(FilterData(R.raw.sunglasses, "sunglasses"))

        val filterAdapter = FilterAdapter(filterList, this)
        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        filterRV.setHasFixedSize(true)
        filterRV.layoutManager = layoutManager
        filterRV.adapter = filterAdapter
    }

    // raw 폴더에 있는 파일(얼굴인식 관련)들을 내부 저장소에 옮기는 메소드
    private fun writeFiles(rawFileID: Int, fileNameID: Int, directoryName: String): File {

        val inputStream = resources.openRawResource(rawFileID)
        val fileName = resources.getString(fileNameID)

        val directory: File = getDir(directoryName, Context.MODE_PRIVATE)
        val file = File(directory, fileName)
        val fileOutputStream = FileOutputStream(file)

        fileOutputStream.write(inputStream.readBytes())
        inputStream.close()
        fileOutputStream.close()

        return file
    }

    // 내부 저장소에 저장된 파일들을 통해 face, eyes 관련 CascadeClassifier 객체 생성
    private fun readCascadeFile() {

        // TODO: 처음 앱을 install 할 때만 다운 받도록 바꿔도 됨 -> 시간나면 바꾸자
        val faceCascadeFile: File = writeFiles(R.raw.haarcascade_frontalface_alt2, R.string.faceFileName, "Cascade")
        val eyesCascadeFile: File = writeFiles(R.raw.haarcascade_eye_tree_eyeglasses, R.string.eyeTreeFileName, "Cascade")
        val imageFile = writeFiles(R.raw.sunglasses, R.string.sunglassesImageFileName, "OverlayImage")


        faceDetector = CascadeClassifier(faceCascadeFile.absolutePath)
        eyesDetector = CascadeClassifier(eyesCascadeFile.absolutePath)
        sunglasses = Imgcodecs.imread(imageFile.absolutePath, Imgcodecs.IMREAD_UNCHANGED)
    }

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {

        thresholds = progress
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {
    }

    override fun onStopTrackingTouch(seekBar: SeekBar?) {
    }
}


//private fun saveImage() {
//
//    val values = ContentValues().apply {
//
//        put(MediaStore.Images.Media.TITLE, "MyCamera")
//        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
//        put(MediaStore.Images.Media.IS_PENDING, 1)
//    }
//
//    val collection: Uri =
//        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
//    val imageUri: Uri = contentResolver.insert(collection, values)!!
//
//    contentResolver.openFileDescriptor(imageUri, "w", null).use {
//        FileOutputStream(it!!.fileDescriptor).use { outputStream ->
//
//            val byteArray: ByteArray
//            val byteArrayStream = ByteArrayOutputStream()
//
//            capturedImage.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayStream)
//            byteArray = byteArrayStream.toByteArray()
//            outputStream.write(byteArray)
//            outputStream.close()
//        }
//    }
//
//    values.clear()
//    values.put(MediaStore.Images.Media.IS_PENDING, 0)
//    contentResolver.update(imageUri, values, null, null)
//
//    photoPreview.visibility = View.GONE
//}
