package org.personal.coupleapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.activity_profile.*

class ProfileActivity : AppCompatActivity(), View.OnClickListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        setListener()
    }

    private fun setListener() {
        modifyProfileIV.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {

            R.id.modifyProfileIV -> toModifyProfile()
        }
    }

    private fun toModifyProfile() {
        val toModifyProfile = Intent(this, ProfileModifyActivity::class.java)
        startActivity(toModifyProfile)
    }
}
