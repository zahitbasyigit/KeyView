package com.zahit.keyview.hidestrategy

/**
 * Created by zahit on 2019-10-16
 */

class HideAllButLastStrategy : HideStrategy {

    override fun shouldHide(charIndex: Int, currentTextLength: Int): Boolean {
        return charIndex != currentTextLength - 1
    }

}