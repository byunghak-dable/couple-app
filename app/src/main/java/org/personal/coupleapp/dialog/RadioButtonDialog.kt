package org.personal.coupleapp.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.DialogFragment
import org.personal.coupleapp.R

class RadioButtonDialog : DialogFragment(), DialogInterface.OnClickListener {

    private val TAG = javaClass.name

    private lateinit var dialogListener: DialogListener
    private var position = 0
    private val menuList: Int by lazy { arguments!!.getInt("arrayResource") }
    private val confirmID: Int by lazy { -1 }
    private val cancelID: Int by lazy { -2 }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        return AlertDialog.Builder(activity)
            .setSingleChoiceItems(menuList, position, this)
            .setPositiveButton(R.string.confirm, this)
            .setNegativeButton(R.string.cancel, this)
            .create()
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
        fun onRadioBtnChoice(whichDialog: Int, choice: String)
    }

    override fun onClick(dialog: DialogInterface?, which: Int) {

        when (which) {
            confirmID -> dialogListener.onRadioBtnChoice(menuList, activity!!.resources.getStringArray(menuList)[position])
            cancelID -> dismiss()
            else -> position = which
        }

    }
}