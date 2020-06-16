package org.personal.coupleapp.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import org.personal.coupleapp.R

class AddAlbumDialog : DialogFragment(), DialogInterface.OnClickListener {

    private val TAG = javaClass.name

    private lateinit var dialogListener: DialogListener
    private lateinit var albumNameED :EditText

    // 확인, 취소 버튼 구분하기 위한 변수
    private val confirmID: Int by lazy { -1 }
    private val cancelID: Int by lazy { -2 }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = requireActivity().layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_add_album, null)

        albumNameED = dialogView.findViewById(R.id.albumNameED)

        return AlertDialog.Builder(activity)
            .setTitle("앨범 폴더 추가하기")
            .setPositiveButton(R.string.confirm, this)
            .setNegativeButton(R.string.cancel, this)
            .setView(dialogView)
            .create()
    }

    override fun onClick(dialog: DialogInterface?, which: Int) {
        when (which) {
            confirmID -> dialogListener.onAddAlbumCompleted(albumNameED.text.toString())
            cancelID -> dismiss()
        }
    }

    // 다이얼로그에서 액티비티로 데이터 전송
    override fun onAttach(context: Context) {
        super.onAttach(context)

        try {

            dialogListener = context as DialogListener

        } catch (e: ClassCastException) {

            e.printStackTrace()
            Log.i(TAG, "onAttach : 인터페이스 implement 해야 함")
        }
    }

    interface DialogListener {
        fun onAddAlbumCompleted(folderName: String)
    }
}