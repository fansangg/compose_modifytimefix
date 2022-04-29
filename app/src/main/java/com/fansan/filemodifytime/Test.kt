package com.fansan.filemodifytime

/**
 *@author  fansan
 *@version 2022/4/29
 */
 
fun main(){
    var abc:String? = null
    abc = "aaa"
    abc?.let {
        println("not null")
    }
}