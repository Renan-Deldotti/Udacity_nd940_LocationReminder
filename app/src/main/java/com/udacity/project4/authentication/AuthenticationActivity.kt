package com.udacity.project4.authentication

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.firebase.ui.auth.AuthUI
import com.udacity.project4.R
import com.udacity.project4.databinding.ActivityAuthenticationBinding
import com.udacity.project4.locationreminders.RemindersActivity

/**
 * This class should be the starting point of the app, It asks the users to sign in / register, and redirects the
 * signed in users to the RemindersActivity.
 */
class AuthenticationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding: ActivityAuthenticationBinding = DataBindingUtil.setContentView(this, R.layout.activity_authentication)

        binding.authLoginButton.setOnClickListener {
            launchSignInFlow()
        }
    }

    private fun launchSignInFlow() {
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(), AuthUI.IdpConfig.GoogleBuilder().build()
        )

        startActivityForResult(
            AuthUI.getInstance().createSignInIntentBuilder().setAvailableProviders(providers).build(),
            SIGN_IN_REQUEST_CODE
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (SIGN_IN_REQUEST_CODE == requestCode) {
            if (Activity.RESULT_OK == resultCode) {
                // Success on sign in
                startActivity(Intent(this, RemindersActivity::class.java))
            } else {
                // Error on sign in
                Toast.makeText(this,"Error on sign-in", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val SIGN_IN_REQUEST_CODE = 1001
    }
}
