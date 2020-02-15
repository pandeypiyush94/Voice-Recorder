package com.piyush.apps.voicerecorder

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView

/**
 * Created by Piyush Pandey on 14 Feb 2020
 */
class P79RecyclerView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : RecyclerView(context, attrs, defStyleAttr) {

    private var emptyView : View? = null
    private var bottomSheet : ConstraintLayout? = null
    private var observer: AdapterDataObserver = object : AdapterDataObserver() {
        override fun onChanged() {
            checkIfEmpty()
        }
        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
            checkIfEmpty()
        }
        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            checkIfEmpty()
        }
        override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
            checkIfEmpty()
        }
        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            checkIfEmpty()
        }
    }

    private fun checkIfEmpty() {
        val rvAdapter = adapter
        if (rvAdapter?.itemCount == 0) {
            this.visibility = View.GONE
            bottomSheet?.visibility = View.GONE
            emptyView?.visibility = View.VISIBLE
        } else {
            this.visibility = View.VISIBLE
            bottomSheet?.visibility = View.VISIBLE
            emptyView?.visibility = View.GONE
        }
    }
    fun setEmptyView(view: View, bottomSheet : ConstraintLayout) {
        emptyView = view
        this.bottomSheet = bottomSheet
    }

    override fun setAdapter(adapter: Adapter<*>?) {
        super.setAdapter(adapter)
        adapter?.registerAdapterDataObserver(observer)
        checkIfEmpty()
    }
}