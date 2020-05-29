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

class AlertDialog : DialogFragment(), DialogInterface.OnClickListener {

    private val TAG = javaClass.name

    private lateinit var dialogListener: DialogListener
    private val confirmID: Int by lazy { -1 }
    private val cancelID: Int by lazy { -2 }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val title = arguments!!.getString("title")
        val message = arguments!!.getString("message")
        val builder = AlertDialog.Builder(activity)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(R.string.confirm, this)
            .setNegativeButton(R.string.cancel, this)

        return builder.create()
    }

    override fun onClick(dialog: DialogInterface?, which: Int) {
        when (which) {
            confirmID -> dialogListener.applyConfirm()
            cancelID -> dismiss()
        }
    }

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
        fun applyConfirm()
    }
}