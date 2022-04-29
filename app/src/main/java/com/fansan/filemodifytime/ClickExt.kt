package com.fansan.filemodifytime

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed

/**
 *@author  fansan
 *@version 2021/10/19
 */

fun Modifier.noRippleClick(click: () -> Unit): Modifier = composed {
    clickable(
        indication = null,
        interactionSource = remember { MutableInteractionSource() },
        onClick = NoFastClick(click))
}


open class NoFastClick(val block: () -> Unit, val delay: Long = 500) : () -> Unit {

    var clickTime: Long = System.currentTimeMillis()

    override fun invoke() {
        if (System.currentTimeMillis() - clickTime < delay) {
            return
        }
        clickTime = System.currentTimeMillis()
        block()
    }

}