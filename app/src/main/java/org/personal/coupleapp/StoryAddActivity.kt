package org.personal.coupleapp

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.View
import android.widget.DatePicker
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_story_add.*
import org.personal.coupleapp.StoryAddActivity.CustomHandler.Companion.UPLOAD_STORY_DATA
import org.personal.coupleapp.StoryAddActivity.MultipleImageHandler.Companion.MULTIPLE_IMAGE
import org.personal.coupleapp.StoryAddActivity.MultipleImageHandler.Companion.SINGLE_IMAGE
import org.personal.coupleapp.adapter.ImageListAdapter
import org.personal.coupleapp.backgroundOperation.ImageDecodeThread
import org.personal.coupleapp.backgroundOperation.ImageDecodeThread.Companion.DECODE_INTO_BITMAP
import org.personal.coupleapp.backgroundOperation.ImageDecodeThread.Companion.DECODE_INTO_MULTIPLE_BITMAP
import org.personal.coupleapp.backgroundOperation.ImageDecodeThread.Companion.DECODE_URL_TO_BITMAP
import org.personal.coupleapp.backgroundOperation.ServerConnectionThread
import org.personal.coupleapp.backgroundOperation.ServerConnectionThread.Companion.REQUEST_INSERT_STORY_DATA
import org.personal.coupleapp.backgroundOperation.ServerConnectionThread.Companion.REQUEST_UPDATE_STORY_DATA
import org.personal.coupleapp.data.StoryData
import org.personal.coupleapp.dialog.ChoiceDialog
import org.personal.coupleapp.dialog.DatePickerDialog
import org.personal.coupleapp.dialog.LoadingDialog
import org.personal.coupleapp.utils.singleton.CalendarHelper
import org.personal.coupleapp.utils.singleton.HandlerMessageHelper
import org.personal.coupleapp.utils.singleton.SharedPreferenceHelper
import java.lang.ref.WeakReference

class StoryAddActivity : AppCompatActivity(), View.OnClickListener, ChoiceDialog.DialogListener, DatePickerDialog.DatePickerListener {

    private val TAG = javaClass.name

    private val serverPage = "StoryAdd"

    // 암시적 인텐트는 1000단위로 설정
    private val CAMERA_REQUEST_CODE = 1000
    private val GALLERY_REQUEST_CODE = 1001

    private lateinit var serverConnectionThread: ServerConnectionThread
    private lateinit var imageDecodeThread: ImageDecodeThread
    private val loadingDialog = LoadingDialog()

    private var imageList = ArrayList<Bitmap>()
    private val imageListAdapter = ImageListAdapter(imageList)
    private var storyData: StoryData? = null

    // 스토리 날짜(밀리세컨으로 받아서 변환한다)
    private var dateTimeInMills: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_story_add)
        setListener()
        buildRecyclerView()
        startWorkerThread()
    }

    override fun onResume() {
        super.onResume()
        if (intent.hasExtra("storyData")) {
            val storyData1: StoryData? = intent.getParcelableExtra("storyData")
            storyData = storyData1
            HandlerMessageHelper.decodeImage(imageDecodeThread, storyData1?.photo_path, DECODE_URL_TO_BITMAP, MULTIPLE_IMAGE)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopWorkerThread()
    }

    // onCreate 보기 편하도록 클릭 리스너 모아두는 메소드
    private fun setListener() {
        addImageBtn.setOnClickListener(this)
        confirmBtn.setOnClickListener(this)
        dateBtn.setOnClickListener(this)
    }


    private fun buildRecyclerView() {
        val layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, true)

        storyImageRV.setHasFixedSize(true)
        storyImageRV.layoutManager = layoutManager
        storyImageRV.adapter = imageListAdapter
    }

    // 백그라운드 스레드 실행
    private fun startWorkerThread() {
        val serverMainHandler = CustomHandler(this, loadingDialog)
        val decodeMainHandler = MultipleImageHandler(imageList, imageListAdapter, loadingDialog)
        serverConnectionThread = ServerConnectionThread("ServerConnectionHelper", serverMainHandler)
        imageDecodeThread = ImageDecodeThread("ImageDecodeThread", this, decodeMainHandler)
        serverConnectionThread.start()
        imageDecodeThread.start()
    }

    // 백그라운드의 루퍼를 멈춰줌으로써 스레드 종료
    private fun stopWorkerThread() {
        Log.i(TAG, "thread 종료")
        serverConnectionThread.looper.quit()
        imageDecodeThread.looper.quit()
    }

    //------------------ 클릭 시 이벤트 관리하는 메소드 모음 ------------------
    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.addImageBtn -> chooseImageTool()
            R.id.confirmBtn -> confirmAddingStory()
            R.id.dateBtn -> chooseMemoryDate()
        }
    }

    // 스토리 이미지를 선택하는 메소드 -> 카메라, 갤러리를 선택하는 다이얼로그가 뜬다
    private fun chooseImageTool() {
        val toolChoiceDialog = ChoiceDialog()
        val arguments = Bundle()

        arguments.putInt("arrayResource", R.array.cameraOrGallery)

        toolChoiceDialog.arguments = arguments
        toolChoiceDialog.show(supportFragmentManager, "CameraOrGalleryDialog")
    }

    private fun confirmAddingStory() {
        val storyID: Int? = storyData?.id
        val coupleColumnID = SharedPreferenceHelper.getInt(this, getText(R.string.coupleColumnID).toString())
        val title = titleED.text.toString()
        val description = descriptionED.text.toString()
        val date = dateTimeInMills

        Log.i(TAG, "StoryID : $storyID")

        // 스토리 데이터 객체를 스레드로 보낸다
        storyData = StoryData(storyID, coupleColumnID, title, description, date, imageList as ArrayList<Any>)

        if (storyID == null) {
            HandlerMessageHelper.serverPostRequest(serverConnectionThread, serverPage, storyData!!, UPLOAD_STORY_DATA, REQUEST_INSERT_STORY_DATA)
        } else {
            HandlerMessageHelper.serverPutRequest(serverConnectionThread, serverPage, storyData!!, UPLOAD_STORY_DATA, REQUEST_UPDATE_STORY_DATA)
        }

        loadingDialog.show(supportFragmentManager, "Loading")
        Log.i(TAG, "프로필 변경 서버에 업로드 메시지 보냄")
    }

    private fun chooseMemoryDate() {
        val datePickerDialog = DatePickerDialog()
        datePickerDialog.show(supportFragmentManager, "startDateDialog")
        Log.i("이미지", imageList.toString())
    }


    //------------------ 다이얼로그 fragment 인터페이스 메소드 모음 ------------------
    override fun onChoice(whichDialog: Int, choice: String, itemPosition: Int?, id: Int?) {
        when (choice) {
            "카메라" -> openCamera()
            "갤러리" -> openGallery()
        }
    }

    override fun onDateSet(view: DatePicker?, year: Int, month: Int, dayOfMonth: Int, whichPicker: String?) {
        dateBtn.text = CalendarHelper.setDateFormat(year, month, dayOfMonth)
        dateTimeInMills = CalendarHelper.dateToTimeInMills(year, month, dayOfMonth)
    }

    // 카메라 앱에 접근해서 찍은 사진을 cameraImage 변수에 담는다
    private fun openCamera() {
        Log.i(TAG, imageList.toString())
        imageListAdapter.notifyDataSetChanged()
    }

    // 갤러리 앱에서 선택한 사진을 받아온다
    private fun openGallery() {
        val galleryIntent = Intent(Intent.ACTION_GET_CONTENT)
        galleryIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        galleryIntent.type = "image/*"
        startActivityForResult(galleryIntent, GALLERY_REQUEST_CODE)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (data != null) {
                when (requestCode) {
                    // TODO: 카메라 관련 이미지 해야함
                    CAMERA_REQUEST_CODE -> {
                    }

                    GALLERY_REQUEST_CODE -> {
                        val clipData = data.clipData
                        if (clipData != null) {
                            loadingDialog.show(supportFragmentManager, "Loading")
                            HandlerMessageHelper.decodeImage(imageDecodeThread, data.clipData, DECODE_INTO_MULTIPLE_BITMAP, MULTIPLE_IMAGE)
                        } else {
                            HandlerMessageHelper.decodeImage(imageDecodeThread, data.data, DECODE_INTO_BITMAP, SINGLE_IMAGE)
                        }
                    }
                }
            }
        }
    }

    // 프로필 사진 업로드,
    private class CustomHandler(activity: AppCompatActivity, loadingDialog: LoadingDialog) : Handler() {

        companion object {
            const val UPLOAD_STORY_DATA = 1
        }

        private val TAG = javaClass.name

        private val activityWeak: WeakReference<AppCompatActivity> = WeakReference(activity)
        private val loadingWeak: WeakReference<LoadingDialog> = WeakReference(loadingDialog)

        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)

            val activity = activityWeak.get()
            val loadingDialog = loadingWeak.get()

            // 액티비티가 destroy 되지 않았을 때
            if (activity != null) {
                when (msg.what) {
                    UPLOAD_STORY_DATA -> {
                        `loadingDialog`?.dismiss()
                        Log.i("되나?", msg.obj.toString())
                        activity.finish()
                    }
                }
                // 액티비티가 destroy 되면 바로 빠져나오도록
            } else {
                Log.i(TAG, "액티비티 제거로 인해 handler 종료")
                return
            }
        }
    }

    // 갤러리에서 이미지를 선택했을 때 리사이클러 뷰에 이미지 보여주는 핸들러
    class MultipleImageHandler(private var imageList: ArrayList<Bitmap>, private val adapter: ImageListAdapter, loadingDialog: LoadingDialog) : Handler() {

        companion object {
            const val SINGLE_IMAGE = 1
            const val MULTIPLE_IMAGE = 2
            const val URL_TO_BITMAP = 3
        }

        private val TAG = javaClass.name

        //        private val adapterWeak: WeakReference<ImageListAdapter> = WeakReference(adapter)
        private val loadingWeak: WeakReference<LoadingDialog> = WeakReference(loadingDialog)

        // 백그라운드에서 bitmap 으로 decoding 한 이미지를 받아 UI 에 갱신하는 작업
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)

            // 액티비티가 destroy 되지 않았을 때
            if (loadingWeak.get() != null) {
                when (msg.what) {
                    SINGLE_IMAGE -> {
                        loadingWeak.get()?.dismiss()
                        val image = msg.obj as Bitmap
                        imageList.add(image)
                        adapter.notifyDataSetChanged()
                    }
                    MULTIPLE_IMAGE -> {
                        loadingWeak.get()?.dismiss()
                        Log.i(TAG, msg.obj.toString())
                        val images = msg.obj as ArrayList<Bitmap>
                        images.forEach { imageList.add(it) }
                        adapter.notifyDataSetChanged()
                    }
                }
            } else {
                Log.i(TAG, "액티비티 제거로 인해 handler 종료")
                return
            }
        }
    }
}