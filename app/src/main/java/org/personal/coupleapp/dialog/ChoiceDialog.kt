package org.personal.coupleapp.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.DialogFragment

class ChoiceDialog : DialogFragment(), DialogInterface.OnClickListener {

    private val TAG = javaClass.name

    private lateinit var dialogListener: DialogListener
    private val menuList: Int by lazy { arguments!!.getInt("arrayResource") }
    private var itemPosition: Int? = null
    private var id :Int? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        itemPosition = arguments?.getInt("itemPosition")
        id = arguments?.getInt("id")

        return AlertDialog.Builder(activity)
            .setItems(menuList, this)
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
        fun onChoice(whichDialog: Int, choice: String, itemPosition: Int?, id:Int?)
    }

    override fun onClick(dialog: DialogInterface?, which: Int) {
        dialogListener.onChoice(menuList, activity!!.resources.getStringArray(menuList)[which], itemPosition, id)
    }
}