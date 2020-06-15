package org.personal.coupleapp

import android.app.Activity
import android.content.ComponentName
import android.content.ContentValues
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.View
import kotlinx.android.synthetic.main.activity_open_chat_add_room.*
import org.personal.coupleapp.backgroundOperation.HTTPConnectionThread.Companion.REQUEST_INSERT_OPEN_CHAT_ROOM
import org.personal.coupleapp.backgroundOperation.ImageDecodeThread
import org.personal.coupleapp.data.OpenChatUserData
import org.personal.coupleapp.data.OpenChatRoomData
import org.personal.coupleapp.dialog.ChoiceDialog
import org.personal.coupleapp.interfaces.service.HTTPConnectionListener
import org.personal.coupleapp.interfaces.service.ImageHandlingListener
import org.personal.coupleapp.service.HTTPConnectionService
import org.personal.coupleapp.service.ImageHandlingService
import org.personal.coupleapp.utils.singleton.SharedPreferenceHelper
import java.lang.Integer.parseInt

class OpenChatAddRoomActivity : AppCompatActivity(), View.OnClickListener, ChoiceDialog.DialogListener, HTTPConnectionListener, ImageHandlingListener {

    private val TAG = javaClass.name

    private val serverPage = "OpenChatAddRoom"

    // 카메라를 통해서 가져오는 이미지
    private lateinit var cameraImage: Uri
    private val imageList by lazy { ArrayList<Bitmap>() }

    private lateinit var httpConnectionService: HTTPConnectionService
    private lateinit var imageHandlingService: ImageHandlingService

    private val ADD_OPEN_CHAT_ROOM = 1

    // 오픈 채팅방을 만드는 방장 ID -> userColumnID 를 서버로 보내 저장
    private val userColumnID by lazy { SharedPreferenceHelper.getInt(this, getText(R.string.userColumnID).toString()) }
    private val userName by lazy { SharedPreferenceHelper.getString(this, getText(R.string.userName).toString()) }
    private val profileImageUrl by lazy { SharedPreferenceHelper.getString(this, getText(R.string.profileImageUrl).toString()) }

    // 암시적 인텐트는 1000단위로 설정
    private val CAMERA_REQUEST_CODE = 1000
    private val GALLERY_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_open_chat_add_room)
        setListener()
        startImageService()
    }

    override fun onStart() {
        super.onStart()
        startBoundService()
    }

    override fun onStop() {
        super.onStop()
        unbindService(httpConnection)
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(imageHandleConnection)
    }

    private fun setListener() {
        coverImageIV.setOnClickListener(this)
        confirmBtn.setOnClickListener(this)
    }

    // 현재 액티비티와 HTTPConnectionService(Bound Service)를 연결하는 메소드
    private fun startBoundService() {
        val startService = Intent(this, HTTPConnectionService::class.java)
        bindService(startService, httpConnection, BIND_AUTO_CREATE)
    }

    private fun startImageService() {
        val startImageService = Intent(this, ImageHandlingService::class.java)
        bindService(startImageService, imageHandleConnection, BIND_AUTO_CREATE)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.coverImageIV -> chooseCoverImage()
            R.id.confirmBtn -> createOpenChatRoom()
        }
    }

    // 커버 이미지를 설정하는 메소드 -> 카메라 or 갤러리를 통해 이미지를 선택할지를 고를 수 있는 다이얼로그 생성
    private fun chooseCoverImage() {
        val toolChoiceDialog = ChoiceDialog()
        val arguments = Bundle()

        arguments.putInt("arrayResource", R.array.cameraOrGallery)

        toolChoiceDialog.arguments = arguments
        toolChoiceDialog.show(supportFragmentManager, "CameraOrGalleryDialog")
    }

    // 오픈 채팅방을 생성했다는 것을 서버에 알리고 오픈 채팅방 정보를 서버에 저장
    private fun createOpenChatRoom() {
        val postData = HashMap<String, Any>()
        val openChatRoomData = OpenChatRoomData(null, userColumnID, roomNameED.text.toString(), roomDefinitionED.text.toString(), imageList[0],1)
        val openChatParticipantsData = OpenChatUserData(null, userColumnID, userName, profileImageUrl)

        postData["openChatRoomData"] = openChatRoomData
        postData["headerData"] = openChatParticipantsData

        httpConnectionService.serverPostRequest(serverPage, postData, REQUEST_INSERT_OPEN_CHAT_ROOM, ADD_OPEN_CHAT_ROOM)
    }

    //------------------ 다이얼로그 리스너 메소드 ------------------
    override fun onChoice(whichDialog: Int, choice: String, itemPosition: Int?, id: Int?) {
        when (choice) {
            "카메라" -> openCamera()
            "갤러리" -> openGallery()
        }
    }

    // 카메라 앱에 접근해서 찍은 사진을 cameraImage 변수에 담는다
    private fun openCamera() {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "pillPicture")
        values.put(MediaStore.Images.Media.DESCRIPTION, "From Camera")
        cameraImage = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)!!

        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImage)
        startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE)
    }

    // 갤러리 앱에서 선택한 사진을 받아온다
    private fun openGallery() {
        val galleryIntent = Intent(Intent.ACTION_PICK)
        galleryIntent.type = "image/*"
        startActivityForResult(galleryIntent, GALLERY_REQUEST_CODE)
    }

    // 카메라 앱과 갤러리 앱을 사용 후 사진을 가져와서 약물 이미지 View에 저장
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (data != null) {
                when (requestCode) {
                    CAMERA_REQUEST_CODE -> {
                        imageHandlingService.decodeImage(cameraImage, ImageDecodeThread.DECODE_INTO_BITMAP)

                    }
                    GALLERY_REQUEST_CODE -> {
                        imageHandlingService.decodeImage(data.data, ImageDecodeThread.DECODE_INTO_BITMAP)
                    }
                }
            }
        }
    }

    override fun onHttpRespond(responseData: HashMap<*, *>) {
        when (responseData["whichRespond"] as Int) {

            ADD_OPEN_CHAT_ROOM -> {
                val roomId = parseInt(responseData["respondData"].toString())
                Log.i(TAG, "http 테스트 : $roomId")
                val toOpenChat = Intent(this, OpenChattingActivity::class.java).apply {
                    putExtra("roomID", roomId)
                }
                startActivity(toOpenChat)
                finish()
            }
        }
    }

    override fun onSingleImage(bitmap: Bitmap) {
        val handler = Handler(Looper.getMainLooper())

        imageList.clear()
        imageList.add(bitmap)

        handler.post { coverImageIV.setImageBitmap(bitmap) }
    }

    override fun onMultipleImage(bitmapList: ArrayList<Bitmap?>) {
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
            httpConnectionService.setOnHttpRespondListener(this@OpenChatAddRoomActivity)
        }

        override fun onServiceDisconnected(name: ComponentName) {
            Log.i(TAG, "http 바운드 서비스 연결 종료")
        }
    }

    private val imageHandleConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder: ImageHandlingService.LocalBinder = service as ImageHandlingService.LocalBinder
            imageHandlingService = binder.getService()!!
            imageHandlingService.setOnImageListener(this@OpenChatAddRoomActivity)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.i(TAG, "이미지 바운드 서비스 연결 종료")
        }
    }
}