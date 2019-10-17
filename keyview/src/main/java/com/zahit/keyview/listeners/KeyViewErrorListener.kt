package com.zahit.keyview.listeners

/**
 * Created by zahit on 2019-10-15
 */
interface KeyViewErrorListener {

    fun hasError(code: String): Boolean

    fun onErrorStatusChanged(hasError: Boolean, code: String)

}