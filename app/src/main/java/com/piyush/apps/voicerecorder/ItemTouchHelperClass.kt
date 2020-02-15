package com.piyush.apps.voicerecorder

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

/**
 * Created by Piyush Pandey on 12 Feb 2020
 */
class ItemTouchHelperClass(dragDirs: Int,
                           swipeDirs: Int,
                           context: Context?,
                           private val adapter: ItemTouchHelperAdapter?) : ItemTouchHelper.SimpleCallback(dragDirs, swipeDirs) {
    private var backGround: ColorDrawable? = null

    private var deleteIcon: Drawable? = null

    init {
        backGround = ColorDrawable()
        deleteIcon = ContextCompat.getDrawable(context!!, R.drawable.menu_delete)
    }

    interface ItemTouchHelperAdapter {
        fun onItemSwiped(position: Int, direction: Int)
    }

    override fun isLongPressDragEnabled(): Boolean {
        return true
    }

    override fun onChildDraw(
        canvas: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val item = viewHolder.itemView
        val itemHeight = item.height
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            if (dX < 0) { /*---------------Draw RED Background----------------*/
                backGround?.color = Color.RED
                backGround?.setBounds(
                    item.right + dX.toInt(),
                    item.top,
                    item.right,
                    item.bottom
                )
                backGround?.draw(canvas)
                /*---------------Set Delete Icon----------------*/
                val deleteIconMargin = (itemHeight - deleteIcon?.intrinsicHeight!!) / 2
                val deleteIconToLeft =
                    item.right - deleteIconMargin - deleteIcon?.intrinsicWidth!!
                val deleteIconToTop =
                    item.top + (itemHeight - deleteIcon?.intrinsicHeight!!) / 2
                val deleteIconToRight = item.right - deleteIconMargin
                val shareIconToBottom = deleteIconToTop + deleteIcon?.intrinsicHeight!!
                deleteIcon?.setBounds(
                    deleteIconToLeft,
                    deleteIconToTop,
                    deleteIconToRight,
                    shareIconToBottom
                )
                deleteIcon?.draw(canvas)
            }
        }
        super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }

    override fun isItemViewSwipeEnabled(): Boolean {
        return true
    }
    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        val upFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
        val swipeFlags = ItemTouchHelper.START
        return ItemTouchHelper.Callback.makeMovementFlags(
            upFlags,
            swipeFlags
        )
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        return false
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        adapter?.onItemSwiped(viewHolder.adapterPosition, direction)
    }
}