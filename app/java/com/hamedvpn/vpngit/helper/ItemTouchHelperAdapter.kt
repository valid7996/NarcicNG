
package com.hamedvpn.vpngit.helper

interface ItemTouchHelperAdapter {
    
    fun onItemMove(fromPosition: Int, toPosition: Int): Boolean

    fun onItemMoveCompleted()

    
    fun onItemDismiss(position: Int)
}

