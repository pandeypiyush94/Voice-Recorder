package com.piyush.apps.voicerecorder

import android.os.Parcel
import android.os.Parcelable

/**
 * Created by Piyush Pandey on 07 Feb 2020
 */
data class Recording(val title: String?, val path: String?, val createdAt:Long, val duration:Long, var status: String?) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readLong(),
        parcel.readLong(),
        parcel.readString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(title)
        parcel.writeString(path)
        parcel.writeLong(createdAt)
        parcel.writeLong(duration)
        parcel.writeString(status)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Recording> {
        override fun createFromParcel(parcel: Parcel): Recording {
            return Recording(parcel)
        }

        override fun newArray(size: Int): Array<Recording?> {
            return arrayOfNulls(size)
        }
    }
}