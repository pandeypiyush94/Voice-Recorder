package com.piyush.apps.voicerecorder

import android.Manifest
import android.content.Context
import android.text.SpannableStringBuilder
import android.text.style.ImageSpan
import android.view.MenuItem
import android.widget.PopupMenu

/**
 * Created by Piyush Pandey on 12 Feb 2020
 */
class Util {
    companion object {
        const val PERMISSION_REQ_CODE = 794
        val PERMISSION_ARRAY = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE)

        const val FILE_EXTENSION = ".mp3"
        const val FILE_DIRECTORY = "VoiceRecorder"

        const val PAUSED = "Paused"
        const val PLAYING = "Playing"
        const val STOPPED = "Stopped"
        const val FINISHED = "Finished"
        const val NOT_STARTED = "Not Started"

        const val FILE_PATH_FORMAT : String = "MMM_dd_yyyy_hh_mm_ss"
        const val DATE_FORMAT : String = "MMM dd, yyyy @ hh:mm:ss aa"

        fun msToString(ms: Long): String? {
            val totalSecs = ms / 1000
            val hours = totalSecs / 3600
            val minutes = totalSecs / 60 % 60
            val secs = totalSecs % 60
            val minutesString = if (minutes == 0L) "00" else if (minutes < 10) "0$minutes" else "" + minutes
            val secsString = if (secs == 0L) "00" else if (secs < 10) "0$secs" else "" + secs
            return if (hours > 0) "$hours : $minutesString : $secsString" else if (minutes > 0) "$minutes : $secsString" else "00 : $secsString"
        }

        fun addIconToMenuItems(context: Context, popUpMenu: PopupMenu){
            val menu = popUpMenu.menu
            for (i in 0 until menu.size()) {
                addIconToMenuItem(context, menu.getItem(i))
            }
        }
        private fun addIconToMenuItem(context: Context, menuItem: MenuItem){
            val icon = menuItem.icon
            val iconSize: Int = context.resources.getDimensionPixelSize(R.dimen.menu_item_icon_size)
            icon.setBounds(0,0,iconSize,iconSize)

            val imgSpan = ImageSpan(icon)

            val strBuilder = SpannableStringBuilder("     ${menuItem.title}")
            strBuilder.setSpan(imgSpan,1,2,0)

            menuItem.title = strBuilder
            menuItem.icon = null
        }
    }

}