package com.zahit.keyview.hidestrategy

/**
 * Created by zahit on 2019-10-16
  */
interface HideStrategy {

    fun shouldHide(charIndex: Int, currentTextLength: Int): Boolean

}