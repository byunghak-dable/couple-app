package org.personal.coupleapp

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.DatePicker
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_story_add.*
import org.personal.coupleapp.adapter.ImageListAdapter
import org.personal.coupleapp.backgroundOperation.HTTPConnectionThread.Companion.REQUEST_INSERT_STORY_DATA
import org.personal.coupleapp.backgroundOperation.ImageDecodeThread.Companion.DECODE_INTO_BITMAP
import org.personal.coupleapp.backgroundOperation.ImageDecodeThread.Companion.DECODE_INTO_MULTIPLE_BITMAP
import org.personal.coupleapp.backgroundOperation.ImageDecodeThread.Companion.DECODE_URL_TO_BITMAP
import org.personal.coupleapp.data.StoryData
import org.personal.coupleapp.dialog.ChoiceDialog
import org.personal.coupleapp.dialog.DatePickerDialog
import org.personal.coupleapp.dialog.LoadingDialog
import org.personal.coupleapp.interfaces.recyclerView.ItemClickListener
import org.personal.coupleapp.interfaces.service.HTTPConnectionListener
import org.personal.coupleapp.interfaces.service.ImageHandlingListener
import org.personal.coupleapp.service.HTTPConnectionService
import org.personal.coupleapp.service.ImageHandlingService
import org.personal.coupleapp.utils.singleton.CalendarHelper
import org.personal.coupleapp.utils.singleton.SharedPreferenceHelper

class StoryAddActivity : AppCompatActivity(), View.OnClickListener, ChoiceDialog.DialogListener, DatePickerDialog.DatePickerListener,
    ImageHandlingListener,
    HTTPConnectionListener, ItemClickListener {

    private val TAG = javaClass.name

    private val serverPage = "StoryAdd"

    // 암시적 인텐트는 1000단위로 설정
    private val CAMERA_REQUEST_CODE = 1000
    private val GALLERY_REQUEST_CODE = 1001

    private lateinit var httpConnectionService: HTTPConnectionService
    private lateinit var imageHandlingService: ImageHandlingService
    private val UPLOAD_STORY_DATA = 1

    private val loadingDialog = LoadingDialog()

    private var imageList = ArrayList<Bitmap?>()
    private val imageListAdapter = ImageListAdapter(imageList, this)
    private var storyData: StoryData? = null

    // 스토리 날짜(밀리세컨으로 받아서 변환한다)
    private var dateTimeInMills: Long = 0

    private val handler by lazy { Handler(Looper.getMainLooper()) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_story_add)
        setListener()
        buildRecyclerView()
        startImageService()
    }

    override fun onStart() {
        super.onStart()
        startHttpService()
    }

    override fun onStop() {
        super.onStop()
        unbindService(httpConnection)
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(imageHandlingConnection)
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

    // 현재 액티비티와 HTTPConnectionService(Bound Service)를 연결하는 메소드
    private fun startHttpService() {
        val startHttpService = Intent(this, HTTPConnectionService::class.java)
        bindService(startHttpService, httpConnection, BIND_AUTO_CREATE)
    }

    // 현재 액티비티와 ImageHandlingService(Bound Service)를 연결하는 메소드
    private fun startImageService() {
        val startImageService = Intent(this, ImageHandlingService::class.java)
        bindService(startImageService, imageHandlingConnection, BIND_AUTO_CREATE)
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
            httpConnectionService.serverPostRequest(serverPage, storyData!!, REQUEST_INSERT_STORY_DATA, UPLOAD_STORY_DATA)
        } else {
            httpConnectionService.serverPutRequest(serverPage, storyData!!, REQUEST_INSERT_STORY_DATA, UPLOAD_STORY_DATA)
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

    //------------------ 리사이클러뷰 아이템 클릭 리스너 메소드 모음 ------------------
    override fun onItemClick(view: View?, itemPosition: Int) {
        Log.i(TAG, "onItemClick : $itemPosition")
    }

    override fun onItemLongClick(view: View?, itemPosition: Int) {
        Log.i(TAG, "onItemOnClick : $itemPosition")

    }

    //------------------ 바인드 서비스 인터페이스 메소드 모음 ------------------
    // http 서비스 인터페이스 메소드
    override fun onHttpRespond(responseData: HashMap<*, *>) {
        when (responseData["whichRespond"] as Int) {
            // 로그인을 할 경우
            UPLOAD_STORY_DATA -> {
                loadingDialog.dismiss()
                Log.i("되나?", responseData["respondData"].toString())
                finish()
            }
        }
    }

    // 이미지 디코딩 서비스 인터페이스 메소드
    override fun onSingleImage(bitmap: Bitmap) {
        loadingDialog.dismiss()
        imageList.add(bitmap)
        handler.post { imageListAdapter.notifyDataSetChanged() }
    }

    override fun onMultipleImage(bitmapList: ArrayList<Bitmap?>) {
        loadingDialog.dismiss()
        bitmapList.forEach { imageList.add(it) }
        handler.post { imageListAdapter.notifyDataSetChanged() }
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
                        loadingDialog.show(supportFragmentManager, "Loading")
                        if (clipData != null) {
                            imageHandlingService.decodeImage(data.clipData, DECODE_INTO_MULTIPLE_BITMAP)
                        } else {
                            imageHandlingService.decodeImage(data.data, DECODE_INTO_BITMAP)
                        }
                    }
                }
            }
        }
    }


    // Memo : BoundService 의 IBinder 객체를 받아와 현재 액티비티에서 서비스의 메소드를 사용하기 위한 클래스
    /*
    바운드 서비스에서는 HTTPConnectionThread(HandlerThread)가 동작하고 있으며, 이 스레드에 메시지를 통해 서버에 요청을 보낸다
    서버에서 결과를 보내주면 HTTPConnectionThread(HandlerThread)의 인터페이스 메소드 -> 바운드 서비스 -> 바운드 서비스 인터페이스 -> 액티비티 onHttpRespond 에서 handle 한다
     */
    private val httpConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder: HTTPConnectionService.LocalBinder = service as HTTPConnectionService.LocalBinder
            httpConnectionService = binder.getService()!!
            httpConnectionService.setOnHttpRespondListener(this@StoryAddActivity)
        }

        override fun onServiceDisconnected(name: ComponentName) {
            Log.i(TAG, "http 바운드 서비스 연결 종료")
        }
    }

    private val imageHandlingConnection: ServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder: ImageHandlingService.LocalBinder = service as ImageHandlingService.LocalBinder
            imageHandlingService = binder.getService()!!
            imageHandlingService.setOnImageListener(this@StoryAddActivity)

            if (intent.hasExtra("storyData")) {
                storyData = intent.getParcelableExtra("storyData")

                titleED.setText(storyData!!.title)
                descriptionED.setText(storyData!!.description)
                dateBtn.text = CalendarHelper.timeInMillsToDate(storyData!!.date)
                dateTimeInMills = storyData!!.date

                loadingDialog.show(supportFragmentManager, "LoadingDailog")
                imageHandlingService.decodeImage(storyData!!.photo_path, DECODE_URL_TO_BITMAP)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.i(TAG, "이미지 바운드 서비스 연결 종료")
        }
    }
}