package com.piyush.apps.voicerecorder

import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.piyush.apps.voicerecorder.Util.Companion.FILE_DIRECTORY
import com.piyush.apps.voicerecorder.Util.Companion.FINISHED
import com.piyush.apps.voicerecorder.Util.Companion.NOT_STARTED
import com.piyush.apps.voicerecorder.Util.Companion.PAUSED
import com.piyush.apps.voicerecorder.Util.Companion.PLAYING
import com.piyush.apps.voicerecorder.Util.Companion.STOPPED
import com.piyush.apps.voicerecorder.Util.Companion.msToString
import kotlinx.android.synthetic.main.custom_media_bottom_sheet.*
import kotlinx.android.synthetic.main.fragment_record_list.*
import kotlinx.coroutines.Runnable
import java.io.File
import java.io.IOException


/**
 * Created by Piyush Pandey on 07 Feb 2020
 */
class RecordListFragment : Fragment(), RecordingsAdapter.ItemClickListener, RecordingsAdapter.ItemDeleteListener, MediaPlayer.OnCompletionListener {

    private val recordingList = ArrayList<Recording>()
    private lateinit var rvRecordingList : P79RecyclerView
    private lateinit var activityContext : VoiceRecorderActivity

    private var totalRecordings = 0
    private var currentRecordingIndex = 0
    private var isRecordingPlaying = false

    private var currentRecording : Recording? = null
    private var playerHandler = Handler()
    private lateinit var playerRunnable : Runnable
    private lateinit var mediaPlayer : MediaPlayer
    private lateinit var adapter : RecordingsAdapter
    private lateinit var bottomSheetBehaviour : BottomSheetBehavior<View>

    private lateinit var btnPlay : ImageView
    private lateinit var btnNext : ImageView
    private lateinit var btnPrevious : ImageView

    private lateinit var tvFileName : TextView
    private lateinit var tvTotalTime : TextView
    private lateinit var tvElapsedTime : TextView
    private lateinit var sbMediaProgress : SeekBar
    private lateinit var tvPlayingStatus : TextView

    override fun onStop() {
        super.onStop()
        stopRecording(currentRecording)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activityContext = context as VoiceRecorderActivity
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        btnPlay = btn_play
        btnNext = btn_forward
        btnPrevious = btn_rewind
        tvTotalTime = tv_total_time
        tvFileName = tv_recording_file
        tvElapsedTime = tv_elapsed_time
        sbMediaProgress = media_progress
        tvPlayingStatus = bottom_sheet_header_status

        rvRecordingList = rv_recording_list
        rvRecordingList.setEmptyView(empty_view, media_bottom_sheet)
        rvRecordingList.itemAnimator = DefaultItemAnimator()
        rvRecordingList.layoutManager = LinearLayoutManager(activityContext)

        btnPlay.setOnClickListener{
            when {
                isRecordingPlaying -> {
                    pauseRecording(currentRecording)
                }
                FINISHED == currentRecording?.status -> {
                    playRecording(currentRecording)
                }
                else -> {
                    resumeRecording(currentRecording)
                }
            }
        }
        btnNext.setOnClickListener{
            if (currentRecordingIndex < totalRecordings){
                currentRecordingIndex += 1
                onItemClick(recordingList[currentRecordingIndex],currentRecordingIndex)
            }
        }
        btnPrevious.setOnClickListener{
            if (currentRecordingIndex > 0) {
                currentRecordingIndex -= 1
                onItemClick(recordingList[currentRecordingIndex],currentRecordingIndex)
            }
        }
        sbMediaProgress.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                currentRecording?.let { recording ->
                    if (FINISHED == recording.status) {
                        playRecording(recording)
                    } else {
                        seekBar?.progress?.let { mediaPlayer.seekTo(it) }
                        resumeRecording(recording)
                    }
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                if (FINISHED != currentRecording?.status) {
                    pauseRecording(currentRecording)
                }
            }
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            }
        })

        controlBottomSheet()

        getRecordedFiles()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_record_list, container, false)
    }

    private fun getRecordedFiles(){
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val directory = File(Environment.getExternalStorageDirectory(),FILE_DIRECTORY)
        val cursor = activityContext.contentResolver.query(uri,null,MediaStore.Audio.Media.DATA + " like ? ",
            arrayOf("%$directory%"),null)
        if (cursor != null && cursor.count > 0){
            while (cursor.moveToNext()) {
                val path = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA))
                val name = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE))
                val duration = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION))
                val modified = File(path).lastModified()
                if (modified != 0L) {
                    recordingList.add(Recording(name, path, modified, duration, NOT_STARTED))
                }
            }
            cursor.close()
        }
        totalRecordings = recordingList.size - 1
        recordingList.sortWith(Comparator { o1, o2 -> o2.createdAt.compareTo(o1.createdAt) })

        adapter = RecordingsAdapter(activityContext, recordingList, this, this)

        val itemTouchHelperCallback = ItemTouchHelperClass(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT,context,adapter)
        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(rvRecordingList)
        rvRecordingList.adapter = adapter

        rvRecordingList.updateLayoutParams<CoordinatorLayout.LayoutParams> { bottomMargin = bottomSheetBehaviour.peekHeight + media_bottom_sheet.height }
    }
    private fun controlBottomSheet(){
        bottomSheetBehaviour = BottomSheetBehavior.from(media_bottom_sheet)
        bottomSheetBehaviour.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                val margin = bottomSheetBehaviour.peekHeight + (bottomSheet.height - bottomSheetBehaviour.peekHeight) * slideOffset
                rvRecordingList.updateLayoutParams<CoordinatorLayout.LayoutParams> { bottomMargin = margin.toInt() }
            }
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    bottomSheetBehaviour.state = BottomSheetBehavior.STATE_COLLAPSED
                } else if (newState == BottomSheetBehavior.STATE_DRAGGING && currentRecording == null){
                    bottomSheetBehaviour.state = BottomSheetBehavior.STATE_COLLAPSED
                }
            }
        })
    }

    override fun onItemClick(recording: Recording, position : Int) {
        currentRecordingIndex = position
        if (currentRecordingIndex == totalRecordings){
            btnNext.setImageResource(R.drawable.icon_player_skip_next_disabled)
            btnNext.isEnabled = false
            btnNext.isClickable = false
        } else {
            btnNext.setImageResource(R.drawable.icon_player_skip_next)
            btnNext.isEnabled = true
            btnNext.isClickable = true
        }
        if (currentRecordingIndex == 0){
            btnPrevious.setImageResource(R.drawable.icon_player_skip_previous_disabled)
            btnPrevious.isEnabled = false
            btnPrevious.isClickable = false
        } else {
            btnPrevious.setImageResource(R.drawable.icon_player_skip_previous)
            btnPrevious.isEnabled = true
            btnPrevious.isClickable = true
        }
        val previousRecording = currentRecording
        currentRecording = recording
        if (previousRecording?.createdAt != currentRecording?.createdAt){
            if (PLAYING == previousRecording?.status || PAUSED == previousRecording?.status){
                stopRecording(previousRecording)
            }
        }
        when (currentRecording?.status){
            NOT_STARTED -> {
                playRecording(currentRecording)
            }
            PLAYING -> {
                pauseRecording(currentRecording)
            }
            PAUSED -> {
                resumeRecording(currentRecording)
            }
            STOPPED -> {
                playRecording(currentRecording)
            }
            FINISHED -> {
                playRecording(currentRecording)
            }
        }
        adapter.notifyDataSetChanged()
    }

    private fun stopRecording(recording: Recording?) {
        if (PLAYING == recording?.status || PAUSED == recording?.status){
            mediaPlayer.stop()
            mediaPlayer.release()
            isRecordingPlaying = false
            recording.status = STOPPED
            tvPlayingStatus.text = recording.status
            btnPlay.setImageDrawable(activityContext.getDrawable(R.drawable.icon_player_play))
            playerHandler.removeCallbacks(playerRunnable)
        }
    }
    private fun pauseRecording(recording: Recording?){
        mediaPlayer.pause()
        isRecordingPlaying = false
        recording?.status = PAUSED
        tvPlayingStatus.text = recording?.status
        btnPlay.setImageDrawable(activityContext.getDrawable(R.drawable.icon_player_play))
        playerHandler.removeCallbacks(playerRunnable)
        adapter.notifyDataSetChanged()
    }
    private fun resumeRecording(recording: Recording?){
        if(FINISHED == recording?.status){
            try {
                mediaPlayer.prepare()
            } catch (ex : Exception){
                ex.printStackTrace()
                Toast.makeText(activityContext,"Unable To Play Recording",Toast.LENGTH_SHORT).show()
            }
        }
        mediaPlayer.start()
        isRecordingPlaying = true
        recording?.status = PLAYING
        tvPlayingStatus.text = recording?.status
        btnPlay.setImageDrawable(activityContext.getDrawable(R.drawable.icon_player_pause))
        updateSeekbar()
        adapter.notifyDataSetChanged()
    }
    private fun playRecording(recording: Recording?) {
        try {
            mediaPlayer = MediaPlayer()
            mediaPlayer.setOnCompletionListener(this)

            //file name is unique, rename file
            mediaPlayer.setDataSource(recording?.path)
            mediaPlayer.prepare()
            mediaPlayer.start()
            isRecordingPlaying = true
            recording?.status = PLAYING
            tvPlayingStatus.text = recording?.status
            tvFileName.text = recording?.title
            btnPlay.setImageDrawable(activityContext.getDrawable(R.drawable.icon_player_pause))

            bottomSheetBehaviour.state = BottomSheetBehavior.STATE_EXPANDED

            sbMediaProgress.max = mediaPlayer.duration
            updateSeekbar()
            adapter.notifyDataSetChanged()
        } catch (ex : IOException){
            ex.printStackTrace()
            Toast.makeText(activityContext,"Unable To Play Recording",Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateSeekbar(){
        playerRunnable = object : Runnable {
            override fun run() {
                playerHandler.postDelayed(this,50)
                sbMediaProgress.progress = mediaPlayer.currentPosition
                val totalTime = mediaPlayer.duration.toLong()
                val elapsedTime = mediaPlayer.currentPosition.toLong()
                tvTotalTime.text = msToString(totalTime)
                tvElapsedTime.text = msToString(elapsedTime)
            }
        }
        playerHandler.postDelayed(playerRunnable,0)
    }

    override fun onCompletion(mp: MediaPlayer?) {
        stopRecording(currentRecording)
        currentRecording?.status = FINISHED
        tvPlayingStatus.text = currentRecording?.status
        adapter.notifyDataSetChanged()
    }
    override fun onItemDelete(recording: Recording) {
        if (PLAYING == recording.status || PAUSED == recording.status){
            currentRecording = null
            bottomSheetBehaviour.state = BottomSheetBehavior.STATE_HIDDEN
        }
        stopRecording(recording)
    }
}
