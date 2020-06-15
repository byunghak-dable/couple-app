package org.personal.coupleapp.interfaces.recyclerView

import android.view.View

interface ItemClickListener {
    fun onItemClick(view: View?, itemPosition: Int)
    fun onItemOnClick(view:View?, itemPosition: Int)
}