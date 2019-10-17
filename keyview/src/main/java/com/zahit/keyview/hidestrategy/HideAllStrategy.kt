package com.zahit.keyview.hidestrategy

/**
 * Created by zahit on 2019-10-16
 */
class HideAllStrategy : HideStrategy {

    override fun shouldHide(charIndex: Int, currentTextLength: Int): Boolean {
        return true
    }

}