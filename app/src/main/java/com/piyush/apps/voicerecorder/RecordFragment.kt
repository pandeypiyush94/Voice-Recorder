package com.piyush.apps.voicerecorder


import android.Manifest
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.SystemClock
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.piyush.apps.voicerecorder.Util.Companion.DATE_FORMAT
import com.piyush.apps.voicerecorder.Util.Companion.FILE_DIRECTORY
import com.piyush.apps.voicerecorder.Util.Companion.FILE_EXTENSION
import com.piyush.apps.voicerecorder.Util.Companion.FILE_PATH_FORMAT
import com.piyush.apps.voicerecorder.Util.Companion.PERMISSION_ARRAY
import com.piyush.apps.voicerecorder.Util.Companion.PERMISSION_REQ_CODE
import kotlinx.android.synthetic.main.fragment_record.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by Piyush Pandey on 07 Feb 2020
 */
class RecordFragment : Fragment() {

    private var recordingDuration = 0L //Milliseconds
    private var isRecording : Boolean = false
    private lateinit var path : String
    private lateinit var createdAt: Date

    private lateinit var timer : Chronometer
    private lateinit var btnRecord : ImageView
    private lateinit var tvRecordingText : TextView
    private lateinit var resetTxtLoader : ProgressBar

    private var loaderHandler = Handler()
    private var loaderRunnable = Runnable {
        timer.base = SystemClock.elapsedRealtime()
        timer.stop()
        tvRecordingText.text = getString(R.string.press_the_mic)
        resetTxtLoader.visibility = View.GONE
    }
    private var mediaRecorder : MediaRecorder? = null

    private lateinit var navController : NavController
    private lateinit var activityContext : VoiceRecorderActivity
    private val dateFormat = SimpleDateFormat(DATE_FORMAT, Locale.US)
    private val filePathDateFormat = SimpleDateFormat(FILE_PATH_FORMAT, Locale.US)

    override fun onStop() {
        super.onStop()
        if (isRecording) {
            stopRecording()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activityContext = context as VoiceRecorderActivity
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        timer = cm_timer
        btnRecord = btn_record
        resetTxtLoader = reset_text_loader
        tvRecordingText = tv_recording_text

        navController = Navigation.findNavController(view)
        btn_list.setOnClickListener {
            if (isRecording) {
                AlertDialog.Builder(activityContext)
                    .setMessage(getString(R.string.alert_stop_recording))
                    .setPositiveButton(getString(R.string.alert_yes)) { _, _ ->
                        navController.navigate(R.id.action_recordFragment_to_recordListFragment)
                    }
                    .setNegativeButton(getString(R.string.alert_no),null)
                    .create().show()
            } else {
                navController.navigate(R.id.action_recordFragment_to_recordListFragment)
            }
        }
        btnRecord.setOnClickListener {
            if (isRecording){
                //Stop Recording
                stopRecording()
            } else {
                //Start Recording
                if (isAllowedToRecord()) {
                    startRecording()
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_record, container, false)
    }

    private fun stopRecording(){
        isRecording = false
        timer.stop()    //Stop Timer

        val name = dateFormat.format(createdAt)
        val recordingText = getString(R.string.file_saved_as) + name
        tvRecordingText.text = recordingText

        mediaRecorder?.stop()
        mediaRecorder?.release()
        mediaRecorder = null

        recordingDuration = System.currentTimeMillis() - recordingDuration

        btnRecord.setImageDrawable(resources.getDrawable(R.drawable.record_btn_stopped,null))
        addRecordingToMediaLibrary(name)

        resetTxtLoader.visibility = View.VISIBLE
        loaderHandler.postDelayed(loaderRunnable,2000)
    }
    private fun startRecording(){
        try {
            createdAt = Date()
            val fileName = filePathDateFormat.format(createdAt)+ FILE_EXTENSION

            val directory = File(Environment.getExternalStorageDirectory(), FILE_DIRECTORY)
            if (!directory.exists()) {
                directory.mkdirs()
            }

            path = "$directory/$fileName"

            mediaRecorder = MediaRecorder()
            //Methods Should Be Called In This Order
            mediaRecorder?.setAudioSource(MediaRecorder.AudioSource.MIC)
            mediaRecorder?.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            mediaRecorder?.setOutputFile(path)
            mediaRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            mediaRecorder?.prepare()
            mediaRecorder?.start()

            recordingDuration = System.currentTimeMillis()

            if (resetTxtLoader.visibility == View.VISIBLE){
                loaderHandler.removeCallbacks(loaderRunnable)
                resetTxtLoader.visibility = View.GONE
            }

            timer.base = SystemClock.elapsedRealtime()
            timer.start()   //Start Timer

            tvRecordingText.text = getString(R.string.recording_started)
            btnRecord.setImageDrawable(resources.getDrawable(R.drawable.record_btn_recording, null))
            isRecording = true
        } catch (ex : IOException){
            ex.printStackTrace()
            Toast.makeText(activityContext,"Unable to record Voice. Please try again",Toast.LENGTH_SHORT).show()
        }
    }

    private fun isAllowedToRecord() : Boolean {
        return if (ActivityCompat.checkSelfPermission(
                activityContext,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(
                activityContext,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(
                activityContext,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            true
        } else {
            ActivityCompat.requestPermissions(
                activityContext,
                PERMISSION_ARRAY,
                PERMISSION_REQ_CODE
            )
            false
        }
    }
    private fun addRecordingToMediaLibrary(name: String) {
        val contentValues = ContentValues(4)
        contentValues.put(MediaStore.Audio.Media.TITLE, name)
        contentValues.put(MediaStore.Audio.Media.MIME_TYPE,"audio/mp3")
        contentValues.put(MediaStore.Audio.Media.DATA,path)
        contentValues.put(MediaStore.Audio.Media.DURATION, recordingDuration)

        val uri = activityContext.contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,contentValues)

        activityContext.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,uri))
    }
}
