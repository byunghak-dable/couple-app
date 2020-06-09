package org.personal.coupleapp.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.DialogFragment
import org.personal.coupleapp.R
import java.lang.ClassCastException

class WarningDialog : DialogFragment(), DialogInterface.OnClickListener {

    private val TAG = javaClass.name
    private lateinit var dialogListener: DialogListener
    private var dialogID: Int? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val title = arguments!!.getString("title")
        val message = arguments!!.getString("message")
        dialogID = arguments?.getInt("dialogID")

        return AlertDialog.Builder(activity)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(R.string.confirm, this)
            .create()
    }

    override fun onClick(dialog: DialogInterface?, which: Int) {
        dialogListener.applyConfirm(dialogID)
    }

    // 다이얼로그에서 액티비티로 확인 버튼 눌렀을 때 알려줌
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
        fun applyConfirm(id: Int?)
    }
}