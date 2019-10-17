package com.zahit.keyview

/**
 * Created by zahit on 2019-10-15
 */
interface KeyViewFilter {

    fun shouldFilter(char: Char): Boolean

}