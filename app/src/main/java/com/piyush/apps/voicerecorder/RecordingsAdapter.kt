package com.piyush.apps.voicerecorder

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.piyush.apps.voicerecorder.RecordListFragment.Companion.PLAYING
import com.piyush.apps.voicerecorder.RecordListFragment.Companion.msToString
import com.piyush.apps.voicerecorder.Util.Companion.addIconToMenuItems
import kotlinx.android.synthetic.main.item_recording.view.*
import java.io.File

/**
 * Created by Piyush Pandey on 30 Jan 2020
 */
class RecordingsAdapter(private val context : Context, private val recordingList : ArrayList<Recording>, private val itemClickListener : ItemClickListener, private val itemDeleteListener: ItemDeleteListener) : RecyclerView.Adapter<RecordingsAdapter.Holder>(), ItemTouchHelperClass.ItemTouchHelperAdapter {

    private var swipedRecording: Recording? = null
    private var swipedRecordingPosition = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(LayoutInflater.from(context).inflate(R.layout.item_recording,parent,false))
    }

    override fun getItemCount(): Int {
        return recordingList.size
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val recording = recordingList[position]
        holder.recordingName.text = recording.title
        holder.recordingTime.text = DateUtils.getRelativeTimeSpanString(recording.createdAt, System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS).toString()
        holder.recordingDuration.text = msToString(recording.duration)

        if (PLAYING == recording.status){
            holder.recordingImg.setImageResource(R.drawable.list_pause_btn)
        } else {
            holder.recordingImg.setImageResource(R.drawable.list_play_btn)
        }

        holder.moreImg.setOnClickListener{
            showMoreList(it,holder.adapterPosition)
        }
        holder.itemView.setOnClickListener {
            if (PLAYING != recording.status){
                holder.recordingImg.setImageResource(R.drawable.list_pause_btn)
            } else {
                holder.recordingImg.setImageResource(R.drawable.list_play_btn)
            }
            itemClickListener.onItemClick(recording,position)
        }
    }

    class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val moreImg : ImageView = itemView.item_more
        val recordingName: TextView = itemView.item_recording_name
        val recordingTime: TextView = itemView.item_recording_time
        val recordingImg : ImageView = itemView.item_recording_image
        val recordingDuration : TextView = itemView.item_recording_duration
    }

    interface ItemClickListener {
        fun onItemClick(recording : Recording, position: Int)
    }
    interface ItemDeleteListener {
        fun onItemDelete(recording : Recording)
    }

    override fun onItemSwiped(position: Int, direction: Int) {
       if (direction == ItemTouchHelper.START) {
            deleteRecording(position)
       }
    }

    private fun shareRecording(position: Int){
        try {
            val file = File(recordingList[position].path!!)
            val uri = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                Uri.fromFile(file)
            } else {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            }
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "audio/*"
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val chooser = Intent.createChooser(shareIntent, "Share Recording Via")
            chooser.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(chooser)
        } catch (ex : Exception){
            Toast.makeText(context,"Unable to share file",Toast.LENGTH_SHORT).show()
            ex.printStackTrace()
        }
    }
    private fun deleteRecording(position: Int){
        swipedRecording = recordingList.removeAt(position)
        swipedRecordingPosition = position
        notifyItemRemoved(position)
        itemDeleteListener.onItemDelete(swipedRecording!!)
        if (!File(swipedRecording?.path!!).delete()){
            recordingList.add(swipedRecordingPosition, swipedRecording!!)
            notifyItemInserted(swipedRecordingPosition)
            Toast.makeText(context, "Unable To Delete Recording", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Recording Deleted", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showMoreList(view : View, position: Int){
        val popUpMenu = PopupMenu(context, view)
        popUpMenu.setOnMenuItemClickListener {
            shareRecording(position)
            true
        }
        popUpMenu.menuInflater.inflate(R.menu.menu_more,popUpMenu.menu)
        addIconToMenuItems(context, popUpMenu)
        popUpMenu.show()
    }
}