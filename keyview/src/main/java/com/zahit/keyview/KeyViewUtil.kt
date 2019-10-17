package com.zahit.keyview

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager

/**
 * Created by zahit on 2019-10-14
 */
class KeyViewUtil {

    companion object {
        fun showKeyboard(view: View) {
            val inputMethodManager =
                view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.showSoftInput(view, 0)
        }

        fun hideKeyboard(view: View) {
            val inputMethodManager =
                view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }
}