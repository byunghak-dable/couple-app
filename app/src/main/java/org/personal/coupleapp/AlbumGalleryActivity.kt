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
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.GridLayoutManager
import kotlinx.android.synthetic.main.activity_album_gallery_acitivy.*
import kotlinx.android.synthetic.main.activity_album_gallery_acitivy.addImageBtn
import org.json.JSONObject
import org.personal.coupleapp.adapter.AlbumGalleryAdapter
import org.personal.coupleapp.backgroundOperation.HTTPConnectionThread.Companion.DELETE_FROM_SERVER
import org.personal.coupleapp.backgroundOperation.HTTPConnectionThread.Companion.REQUEST_GET_ALBUM_IMAGES
import org.personal.coupleapp.backgroundOperation.HTTPConnectionThread.Companion.REQUEST_INSERT_ALBUM_IMAGES
import org.personal.coupleapp.backgroundOperation.ImageDecodeThread.Companion.DECODE_INTO_BITMAP
import org.personal.coupleapp.backgroundOperation.ImageDecodeThread.Companion.DECODE_INTO_MULTIPLE_BITMAP
import org.personal.coupleapp.data.AlbumGalleryData
import org.personal.coupleapp.dialog.ChoiceDialog
import org.personal.coupleapp.dialog.InformDialog
import org.personal.coupleapp.dialog.LoadingDialog
import org.personal.coupleapp.interfaces.recyclerView.ItemClickListener
import org.personal.coupleapp.interfaces.service.HTTPConnectionListener
import org.personal.coupleapp.interfaces.service.ImageHandlingListener
import org.personal.coupleapp.service.HTTPConnectionService
import org.personal.coupleapp.service.ImageHandlingService
import org.personal.coupleapp.utils.singleton.ImageEncodeHelper

class AlbumGalleryActivity : AppCompatActivity(), HTTPConnectionListener, ItemClickListener, View.OnClickListener, ChoiceDialog.DialogListener, ImageHandlingListener,
    InformDialog.DialogListener {

    private val TAG = javaClass.name

    private val serverPage = "AlbumGallery"

    private lateinit var imageHandlingService: ImageHandlingService
    private lateinit var httpConnectionService: HTTPConnectionService
    private val ADD_ALBUM_IMAGES = 1
    private val GET_ALBUM_IMAGES = 2
    private val DELTE_IMAGE = 3

    private val galleryList by lazy { ArrayList<AlbumGalleryData>() }
    private val albumGalleryAdapter by lazy { AlbumGalleryAdapter(this, galleryList, this) }

    private var folderID: Int? = null
    private val addImageList by lazy { ArrayList<AlbumGalleryData>() }
    private val loadingDialog by lazy { LoadingDialog() }

    // 아이템을 길게 클릭하면 삭제할지 여부를 물어보는 다이얼로그를 보여주는데 그 때 해당 아이템의 id를 저장하는 변수
    private var targetImageID: Int? = null

    private val GALLERY_REQUEST_CODE = 1001
    private val CUSTOM_CAMERA_REQUEST_CODE = 1002

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_album_gallery_acitivy)
        setListener()
        buildRecyclerView()
        startBoundService()

        if (intent.hasExtra("folderID")) {
            folderID = intent.getIntExtra("folderID", 0)
            Log.i(TAG, "folder id : $folderID")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(httpConnection)
        unbindService(imageHandlingConnection)
    }

    private fun setListener() {
        addImageBtn.setOnClickListener(this)
    }

    private fun buildRecyclerView() {
        val layoutManager = GridLayoutManager(this, 3)

        galleryRV.setHasFixedSize(true)
        galleryRV.layoutManager = layoutManager
        galleryRV.adapter = albumGalleryAdapter
    }

    // 이미지 핸들링 서비스 http 서비스 시작하는 메소드
    private fun startBoundService() {
        val startHttpService = Intent(this, HTTPConnectionService::class.java)
        val startImageService = Intent(this, ImageHandlingService::class.java)
        bindService(startHttpService, httpConnection, BIND_AUTO_CREATE)
        bindService(startImageService, imageHandlingConnection, BIND_AUTO_CREATE)
    }

    private fun makeRequestUrl(): String {
        return "$serverPage?what=getAlbumImages&&folderID=$folderID"
    }

    //------------------ 버튼 클릭 시 이벤트 관리하는 메소드 모음 ------------------
    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.addImageBtn -> chooseImageTool()
        }
    }

    private fun chooseImageTool() {
        val toolChoiceDialog = ChoiceDialog()
        val arguments = Bundle()

        arguments.putInt("arrayResource", R.array.cameraOrGallery)

        toolChoiceDialog.arguments = arguments
        toolChoiceDialog.show(supportFragmentManager, "chooseImageToolDialog")
    }

    //------------------ 버튼 클릭 시 이벤트 관리하는 메소드 모음 ------------------
    override fun onChoice(whichDialog: Int, choice: String, itemPosition: Int?, id: Int?) {
        when (choice) {
            "카메라" -> openCustomCamera()
            "갤러리" -> openGallery()
        }
    }

    private fun openCustomCamera() {
        val toCustomCamera = Intent(this, CustomCameraActivity::class.java)
        startActivityForResult(toCustomCamera, CUSTOM_CAMERA_REQUEST_CODE)
    }

    private fun openGallery() {
        val galleryIntent = Intent(Intent.ACTION_GET_CONTENT)
        galleryIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        galleryIntent.type = "image/*"
        startActivityForResult(galleryIntent, GALLERY_REQUEST_CODE)
    }

    //------------------ 리사이클러 뷰 아이템 클릭 시 이벤트 관리하는 메소드 모음 ------------------
    override fun onItemClick(view: View?, itemPosition: Int) {
    }

    override fun onItemLongClick(view: View?, itemPosition: Int) {
        val deleteDialog = InformDialog()
        val arguments = Bundle()

        arguments.putString("title", "사진 삭제")
        arguments.putString("message", "선택하신 사진을 삭제하시겠습니까?")
        arguments.putBoolean("needAction", true)

        deleteDialog.arguments = arguments
        deleteDialog.show(supportFragmentManager, "BackWarning")
        targetImageID = galleryList[itemPosition].id
    }

    //------------------ 다이얼로그 리스너 관리하는 메소드 모음 ------------------
    // 사진 삭제할지를 물어보는 다이얼로그 결과
    override fun applyConfirm() {
        val deleteJsonObject = JSONObject()
        deleteJsonObject.put("what", "deleteImage")
        deleteJsonObject.put("folderID", folderID)
        deleteJsonObject.put("id", targetImageID)
        Log.i(TAG, deleteJsonObject.toString())
        httpConnectionService.serverDeleteRequest(serverPage, deleteJsonObject.toString(), DELETE_FROM_SERVER, DELTE_IMAGE)
    }

    //------------------ 바인드 서비스 인터페이스 리스너 관리하는 메소드 모음 ------------------
    // 이미지 디코딩 서비스 인터페이스 메소드
    override fun onSingleImage(bitmap: Bitmap) {
        loadingDialog.dismiss()
        val albumGalleryData = AlbumGalleryData(null, folderID!!, bitmap)
        addImageList.add(albumGalleryData)

        httpConnectionService.serverPostRequest(serverPage, addImageList, REQUEST_INSERT_ALBUM_IMAGES, ADD_ALBUM_IMAGES)
    }

    override fun onMultipleImage(bitmapList: ArrayList<Bitmap?>) {
        loadingDialog.dismiss()
        bitmapList.forEach {
            val albumGalleryData = AlbumGalleryData(null, folderID!!, it)
            addImageList.add(albumGalleryData)
        }
        httpConnectionService.serverPostRequest(serverPage, addImageList, REQUEST_INSERT_ALBUM_IMAGES, ADD_ALBUM_IMAGES)
    }

    override fun onHttpRespond(responseData: HashMap<*, *>) {
        val handler = Handler(Looper.getMainLooper())
        loadingDialog.dismiss()
        when (responseData["whichRespond"] as Int) {

            ADD_ALBUM_IMAGES -> {
                Log.i(TAG, "http test : ${responseData["respondData"]}")
                if (responseData["respondData"] == 200) {
                    galleryList.clear()
                    loadingDialog.show(supportFragmentManager, "LoadingDialog")
                    httpConnectionService.serverGetRequest(makeRequestUrl(), REQUEST_GET_ALBUM_IMAGES, GET_ALBUM_IMAGES)
                } else {
                    Log.i(TAG, "서버 연결 실패")
                }
            }

            GET_ALBUM_IMAGES -> {
                if (responseData["respondData"] != null) {
                    val fetchedAlbumList = responseData["respondData"] as ArrayList<AlbumGalleryData>
                    fetchedAlbumList.forEach { galleryList.add(it) }
                    handler.post { albumGalleryAdapter.notifyDataSetChanged() }
                } else {
                    handler.post { albumGalleryAdapter.notifyDataSetChanged() }
                }
            }

            DELTE_IMAGE -> {
                Log.i(TAG, responseData["respondData"].toString())
                if (responseData["respondData"] == "true") {
                    galleryList.clear()
                    loadingDialog.show(supportFragmentManager, "LoadingDialog")
                    httpConnectionService.serverGetRequest(makeRequestUrl(), REQUEST_GET_ALBUM_IMAGES, GET_ALBUM_IMAGES)
                }
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.P)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (data != null) {
                when (requestCode) {
                    GALLERY_REQUEST_CODE -> {
                        val clipData = data.clipData
                        loadingDialog.show(supportFragmentManager, "Loading")
                        if (clipData != null) {
                            addImageList.clear()
                            imageHandlingService.decodeImage(data.clipData, DECODE_INTO_MULTIPLE_BITMAP)
                        } else {
                            addImageList.clear()
                            imageHandlingService.decodeImage(data.data, DECODE_INTO_BITMAP)
                        }
                    }
                    CUSTOM_CAMERA_REQUEST_CODE -> {
                        addImageList.clear()
                        val albumGalleryData = AlbumGalleryData(null, folderID!!, ImageEncodeHelper.bitmapList[0])
                        addImageList.add(albumGalleryData)
                        loadingDialog.show(supportFragmentManager, "LoadingDialog")
                        httpConnectionService.serverPostRequest(serverPage, addImageList, REQUEST_INSERT_ALBUM_IMAGES, ADD_ALBUM_IMAGES)
                    }
                }
            }
        }
    }

    private val imageHandlingConnection: ServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder: ImageHandlingService.LocalBinder = service as ImageHandlingService.LocalBinder
            imageHandlingService = binder.getService()!!
            imageHandlingService.setOnImageListener(this@AlbumGalleryActivity)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.i(TAG, "이미지 바운드 서비스 연결 종료")
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
            httpConnectionService.setOnHttpRespondListener(this@AlbumGalleryActivity)

            loadingDialog.show(supportFragmentManager, "LoadingDialog")
            httpConnectionService.serverGetRequest(makeRequestUrl(), REQUEST_GET_ALBUM_IMAGES, GET_ALBUM_IMAGES)
        }

        override fun onServiceDisconnected(name: ComponentName) {
            Log.i(TAG, "바운드 서비스 연결 종료")
        }
    }
}