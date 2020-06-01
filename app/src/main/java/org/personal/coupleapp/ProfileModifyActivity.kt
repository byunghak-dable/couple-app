package org.personal.coupleapp

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.DatePicker
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_modify_profile.*
import org.personal.coupleapp.ProfileModifyActivity.CustomHandler.Companion.UPLOAD_PROFILE
import org.personal.coupleapp.backgroundOperation.ImageDecodeHandler
import org.personal.coupleapp.backgroundOperation.ImageDecodeHandler.Companion.SINGLE_IMAGE
import org.personal.coupleapp.backgroundOperation.ImageDecodeThread
import org.personal.coupleapp.backgroundOperation.ImageDecodeThread.Companion.DECODE_INTO_BITMAP
import org.personal.coupleapp.backgroundOperation.ServerConnectionThread
import org.personal.coupleapp.backgroundOperation.ServerConnectionThread.Companion.REQUEST_PROFILE_UPLOAD
import org.personal.coupleapp.data.ProfileData
import org.personal.coupleapp.dialog.ChoiceDialog
import org.personal.coupleapp.dialog.DatePickerDialog
import org.personal.coupleapp.dialog.InformDialog
import org.personal.coupleapp.dialog.RadioButtonDialog
import org.personal.coupleapp.utils.singleton.CalendarHelper
import org.personal.coupleapp.utils.singleton.HandlerMessageHelper
import org.personal.coupleapp.utils.singleton.SharedPreferenceHelper
import java.lang.ref.WeakReference

class ProfileModifyActivity : AppCompatActivity(), View.OnClickListener, DatePickerDialog.DatePickerListener, RadioButtonDialog.DialogListener,
    InformDialog.DialogListener, ChoiceDialog.DialogListener {

    private val TAG = javaClass.name

    private val serverPage = "ModifyProfile"

    // PERMISSION_CODE는 100 단위로 설정
    private val PERMISSION_CODE_CAMERA = 100
    private val PERMISSION_CODE_GALLERY = 101

    // 암시적 인텐트는 1000단위로 설정
    private val CAMERA_REQUEST_CODE = 1000
    private val GALLERY_REQUEST_CODE = 1001

    private lateinit var serverConnectionThread: ServerConnectionThread
    private lateinit var imageDecodeThread: ImageDecodeThread
    // 프로필 이미지의 Bitmap 정보를 담고 있는 리스트
    private val imageList: ArrayList<Bitmap> =  ArrayList()
    // 생일 정보를 담고 있는 변수
    private var birthdayInMills: Int? = null

    // 카메라를 통해서 가져오는 이미지
    private lateinit var cameraImage: Uri


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_modify_profile)
        setListener()
        startWorkerThread()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i("thread-test", "onDestroy")
        stopWorkerThread()
    }

    override fun onBackPressed() {
        val warningDialog = InformDialog()
        val arguments = Bundle()

        arguments.putString("title", getText(R.string.goBackTitle).toString())
        arguments.putString("message", getText(R.string.goBackMessage).toString())
        arguments.putBoolean("needAction", true)

        warningDialog.arguments = arguments
        warningDialog.show(supportFragmentManager, "BackWarning")
    }

    private fun setListener() {
        profileImageIV.setOnClickListener(this)
        birthdayBtn.setOnClickListener(this)
        sexBtn.setOnClickListener(this)
        confirmBtn.setOnClickListener(this)
    }

    // 백그라운드 스레드 실행
    private fun startWorkerThread() {
        val serverMainHandler = CustomHandler(this)
        val decodeMainHandler = ImageDecodeHandler(profileImageIV, imageList)

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
            R.id.profileImageIV -> chooseProfileImage()
            R.id.birthdayBtn -> chooseBirthday()
            R.id.sexBtn -> chooseSex()
            R.id.confirmBtn -> confirmChange()
        }
    }

    // 프로필 이미지를 선택하는 메소드 -> 카메로, 갤러리를 선택하는 다이얼로그가 뜬다
    private fun chooseProfileImage() {
        val toolChoiceDialog = ChoiceDialog()
        val arguments = Bundle()

        arguments.putInt("arrayResource", R.array.cameraOrGallery)

        toolChoiceDialog.arguments = arguments
        toolChoiceDialog.show(supportFragmentManager, "CameraOrGalleryDialog")
    }


    // 생일 정보를 선택하는 메소드 -> DatePicker 다이얼로그가 뜬다
    private fun chooseBirthday() {
        val datePickerDialog = DatePickerDialog()
        datePickerDialog.show(supportFragmentManager, "startDateDialog")
        Log.i("이미지", imageList.toString())
    }

    // 성별을 선택하는 메소드
    private fun chooseSex() {
        val repeatPlanDialog = RadioButtonDialog()
        val arguments = Bundle()

        arguments.putInt("arrayResource", R.array.sex)

        repeatPlanDialog.arguments = arguments
        repeatPlanDialog.show(supportFragmentManager, "RepeatChoiceDialog")
    }

    // 프로필 정보를 업로드 하는 메소드
    private fun confirmChange() {
        val userColumnID = SharedPreferenceHelper.getInt(this, getText(R.string.userColumnID).toString())
        val name = nameED.text.toString()
        val stateMessage = stateED.text.toString()
        val birthDay = birthdayInMills!!
        val sex = sexBtn.text.toString()
        // 프로필 데이터 객체를 스레드로 보낸다
        val profileData = ProfileData(userColumnID, imageList[0], name, stateMessage, birthDay, sex)

        HandlerMessageHelper.serverPostRequest(serverConnectionThread, serverPage, profileData, UPLOAD_PROFILE, REQUEST_PROFILE_UPLOAD)
        Log.i(TAG, "프로필 변경 서버에 업로드 메시지 보냄")
    }

    //------------------ 다이얼로그 fragment 인터페이스 메소드 모음 ------------------

    // 카메라, 갤러리 선택 다이얼로그 결과
    override fun onChoice(whichDialog: Int, choice: String) {
        when (choice) {
            "카메라" -> cameraPermission()
            "갤러리" -> galleryPermission()
        }
    }

    // 생일 선택 결과
    override fun onDateSet(view: DatePicker?, year: Int, month: Int, dayOfMonth: Int, whichPicker: String?) {
        birthdayBtn.text = CalendarHelper.setDateFormat(year, month, dayOfMonth)
        birthdayInMills = CalendarHelper.dateToTimeInMills(year, month, dayOfMonth)
    }

    // 성별 선택 결과
    override fun onRadioBtnChoice(whichDialog: Int, choice: String) {
        sexBtn.text = choice
    }

    // 뒤로가기할 때 뜨는 다이얼로그에서 확인 선택 결과
    override fun applyConfirm() {
        finish()
    }

    // 카메라 퍼미션 받기 (저장소, 카메라 권한 받기)
    private fun cameraPermission() {

        // 사용자가 카메라 또는 외부 저장장치의 저장을 허용한 적이 없는 경우
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED
            || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {

            // permission 을 granted 할 수 있도록 요청(카메라와 외부 저장장치에 쓸 수 있는 권한)
            val permission = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)

            // 사용자에게 팝업창으로 선택할 수 있도록 보여주기
            requestPermissions(permission, PERMISSION_CODE_CAMERA)

        } else {
            openCamera()
        }
    }

    // 갤러리 퍼미션 받기 (저장소 권한 받기)
    private fun galleryPermission() {
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            val permission = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            requestPermissions(permission, PERMISSION_CODE_GALLERY)
        } else {
            openGallery()
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

    // cameraPermission()나 galleryPermission()에서 사용자가 권한 허용 POPUP 창의 결과에 따른 카메라 또는 갤러리 실행 여부 결정
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_CODE_CAMERA -> if (grantResults.isNotEmpty()) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                        openCamera()
                    } else {
                        Toast.makeText(this, "카메라 접근 권한을 취소하셨습니다.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "저장공간 접근 권한을 취소하셨습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            PERMISSION_CODE_GALLERY -> if (grantResults.isNotEmpty()) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openGallery()
                } else {
                    Toast.makeText(this, "갤러리 접근 권한을 취소하셨습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 카메라 앱과 갤러리 앱을 사용 후 사진을 가져와서 약물 이미지 View에 저장
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (data != null) {
                when (requestCode) {
                    CAMERA_REQUEST_CODE -> {HandlerMessageHelper.decodeImage(imageDecodeThread, cameraImage, DECODE_INTO_BITMAP, SINGLE_IMAGE)

                    }
                    GALLERY_REQUEST_CODE ->  {HandlerMessageHelper.decodeImage(imageDecodeThread, data.data, DECODE_INTO_BITMAP, SINGLE_IMAGE)
                    Log.i("이미지", imageList.toString())
                }
                }
            }
        }
    }

    // 프로필 사진 업로드,
    private class CustomHandler(activity: AppCompatActivity) : Handler() {

        companion object {
            const val UPLOAD_PROFILE = 1
        }

        private val activityWeak: WeakReference<AppCompatActivity> = WeakReference(activity)

        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)

            val activity = activityWeak.get()

            // 액티비티가 destroy 되지 않았을 때
            if (activity != null) {
                when (msg.what) {
                    UPLOAD_PROFILE -> {
                        Log.i("ServerConnectionThread", msg.obj.toString())
                    }
                }
                // 액티비티가 destroy 되면 바로 빠져나오도록
            } else {
                return
            }
        }
    }

}
