package org.personal.coupleapp.dialog

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.DialogFragment
import kotlinx.android.synthetic.main.dialog_plan_type.view.*
import org.personal.coupleapp.R

class PlanTypeDialog : DialogFragment(), View.OnClickListener {

    private val TAG = javaClass.name

    private lateinit var dialogListener: DialogListener
    private lateinit var planType: String

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder: AlertDialog.Builder
        val inflater = requireActivity().layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_plan_type, null)

        dialogView.dateBtn.setOnClickListener(this)
        dialogView.travelBtn.setOnClickListener(this)
        dialogView.cultureLifeBtn.setOnClickListener(this)
        dialogView.schoolBtn.setOnClickListener(this)
        dialogView.otherBtn.setOnClickListener(this)

        builder = AlertDialog.Builder(activity).setView(dialogView)
        return builder.create()
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.dateBtn -> applyChoice("데이트", R.drawable.ic_favorite_black_24dp)
            R.id.travelBtn -> applyChoice("여행", R.drawable.ic_airplanemode_active_black_24dp)
            R.id.cultureLifeBtn -> applyChoice("문화생활", R.drawable.ic_music_note_black_24dp)
            R.id.schoolBtn -> applyChoice("학교", R.drawable.ic_school_black_24dp)
            R.id.otherBtn -> applyChoice("기타", R.drawable.ic_menu_black_24dp)
        }
    }

    // 사용자의 선택을 액티비티로 보내는 메소드
    private fun applyChoice(choice: String, imageSource:Int) {
        dialogListener.applyPlanType(choice, imageSource)
        dismiss()
    }

    // 다이얼로그에서 액티비티로 데이터 전송
    override fun onAttach(context: Context) {
        super.onAttach(context)

        try {

            dialogListener = context as DialogListener

        } catch (e: ClassCastException) {

            e.printStackTrace()
            Log.i(TAG, "onAttach : 인터페이스 implement 안함")
        }
    }

    interface DialogListener {
        fun applyPlanType(planType: String, imageSource:Int)
    }
}