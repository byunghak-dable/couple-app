package org.personal.coupleapp.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.ImageReader
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.AttributeSet
import android.util.Log
import android.util.SparseIntArray
import android.view.Surface
import androidx.annotation.RequiresApi
import org.opencv.android.JavaCamera2View
import java.io.FileOutputStream
import java.util.*

class CustomCameraView(context: Context?, attrs: AttributeSet?) : JavaCamera2View(context, attrs) {

    private val TAG = "MyCameraView"
    private val ORIENTATIONS = SparseIntArray()

    init {

        ORIENTATIONS.append(Surface.ROTATION_0, 90)
        ORIENTATIONS.append(Surface.ROTATION_90, 0)
        ORIENTATIONS.append(Surface.ROTATION_180, 270)
        ORIENTATIONS.append(Surface.ROTATION_270, 180)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun takePicture() {

        if (mCameraDevice == null) {

            return
        }

        val activity = context as Activity
        val manager = activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val characteristics: CameraCharacteristics =
            manager.getCameraCharacteristics(mCameraDevice.id)
        val jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            ?.getOutputSizes(ImageFormat.JPEG)
        var width = 640
        var height = 480

        if (jpegSizes != null) {

            if (jpegSizes.isNotEmpty()) {

                width = jpegSizes[0].width
                height = jpegSizes[0].height
            }
        }

        val imageReader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1)

        val captureBuilder: CaptureRequest.Builder =
            mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
        captureBuilder.addTarget(imageReader.surface)
        captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)

        val rotation = activity.windowManager.defaultDisplay.rotation
        captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation))

        val readerListener = object : ImageReader.OnImageAvailableListener {
            override fun onImageAvailable(reader: ImageReader?) {

                val image = reader!!.acquireLatestImage()
                val buffer = image.planes[0].buffer
                val bytes = ByteArray(buffer.capacity())
                buffer.get(bytes)
                save(bytes)
            }

            private fun save(bytes: ByteArray) {

                val values = ContentValues().apply {

                    put(MediaStore.Images.Media.TITLE, "MyCamera")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }

                val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val imageUri: Uri = context.contentResolver.insert(collection, values)!!

                context.contentResolver.openFileDescriptor(imageUri, "w", null).use {
                    FileOutputStream(it!!.fileDescriptor).use { outputStream ->
                        outputStream.write(bytes)
                        outputStream.close()
                    }
                }

                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(imageUri, values, null, null)
            }
        }

        imageReader.setOnImageAvailableListener(readerListener, mBackgroundHandler)

        val captureListener = object : CameraCaptureSession.CaptureCallback() {
            @SuppressLint("ShowToast")
            override fun onCaptureCompleted(
                session: CameraCaptureSession,
                request: CaptureRequest,
                result: TotalCaptureResult
            ) {
                super.onCaptureCompleted(session, request, result)
                Log.i(TAG, "save")
            }
        }
        mCameraDevice.createCaptureSession(
            Arrays.asList(imageReader.surface), object : CameraCaptureSession.StateCallback() {

                override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {

                    try {
                        cameraCaptureSession.capture(
                            captureBuilder.build(),
                            captureListener,
                            mBackgroundHandler
                        )
                    } catch (e: CameraAccessException) {
                        e.printStackTrace()
                    }
                }

                override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {


                }
            }, mBackgroundHandler
        )
    }
}

