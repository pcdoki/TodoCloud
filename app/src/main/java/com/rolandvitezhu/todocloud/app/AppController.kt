package com.rolandvitezhu.todocloud.app

import android.app.Application
import android.graphics.Color
import android.widget.TextView
import androidx.appcompat.view.ActionMode
import com.google.android.material.R
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.jakewharton.threetenabp.AndroidThreeTen
import com.rolandvitezhu.todocloud.di.component.AppComponent
import com.rolandvitezhu.todocloud.di.component.DaggerAppComponent

open class AppController : Application() {

    val appComponent: AppComponent by lazy {
        initializeComponent()
    }

    private fun initializeComponent(): AppComponent {
        appContext = applicationContext as Application
        instance = this
        AndroidThreeTen.init(this)
        return DaggerAppComponent.factory().create(applicationContext)
    }

    companion object {
        @get:Synchronized
        var instance: AppController? = null
            private set
        var isActionModeEnabled = false
        private var actionMode: ActionMode? = null

        private var lastShownSnackbar: Snackbar? = null

        fun setActionMode(actionMode: ActionMode?) {
            Companion.actionMode = actionMode
        }

        fun isActionMode(): Boolean {
            return actionMode != null
        }

        var appContext: Application? = null

        fun showWhiteTextSnackbar(snackbar: Snackbar) {
            val snackbarTextView = snackbar.view
                    .findViewById<TextView>(R.id.snackbar_text)
            val lastShownSnackbarTextView: TextView
            val snackbarText = snackbarTextView.text
            var lastShownSnackbarText: CharSequence = ""
            if (lastShownSnackbar != null) {
                lastShownSnackbarTextView = lastShownSnackbar!!.view
                        .findViewById(R.id.snackbar_text)
                lastShownSnackbarText = lastShownSnackbarTextView.text
            }
            val shouldShowSnackbar = !(lastShownSnackbar != null && lastShownSnackbar!!.isShown
                    && snackbarText == lastShownSnackbarText)
            if (shouldShowSnackbar) {
                snackbarTextView.setTextColor(Color.WHITE)
                snackbar.show()
                lastShownSnackbar = snackbar
            }
        }

        /**
         * Fix unnecessary TextInputEditText animation on set text.
         * @param text The text to set.
         * @param textInputEditText TextInputEditText to set text.
         * @param relatedTextInputLayout The TextInputLayout related to the textInputEditText.
         */
        fun setText(
                text: String?, textInputEditText: TextInputEditText, relatedTextInputLayout: TextInputLayout
        ) {
            // Disable animation before set text. The unnecessary animation won't play in this case.
            relatedTextInputLayout.isHintAnimationEnabled = false
            textInputEditText.setText(text)

            // Enable animation, because it should work on user interaction.
            relatedTextInputLayout.isHintAnimationEnabled = true
        }
    }
}