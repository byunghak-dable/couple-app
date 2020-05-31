package org.personal.coupleapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val SPLASH_SCREEN_DELAY_TIME: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        splashAnimation()
    }

    private fun splashAnimation() {
        val bottomAnimation = AnimationUtils.loadAnimation(this, R.anim.splash_from_bottom)
        logoImage.animation = bottomAnimation

        Handler().postDelayed({
            val toSignIn = Intent(this, SignInActivity::class.java)
            startActivity(toSignIn)
            finish()
        }, SPLASH_SCREEN_DELAY_TIME)
    }
}
