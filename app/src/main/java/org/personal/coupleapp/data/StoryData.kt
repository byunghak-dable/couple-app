package org.personal.coupleapp.data

import android.os.Parcel
import android.os.Parcelable

@Suppress("UNREACHABLE_CODE")
data class StoryData(val id: Int?, val couple_id: Int, val title: String, val description: String, val date: Long, var photo_path: ArrayList<Any>) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readValue(Int::class.java.classLoader) as? Int,
        parcel.readInt(),
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readLong(),
        parcel.readArrayList(ArrayList::class.java.classLoader) as ArrayList<Any>
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeValue(id)
        parcel.writeInt(couple_id)
        parcel.writeString(title)
        parcel.writeString(description)
        parcel.writeLong(date)
        parcel.writeList(photo_path as List<*>)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<StoryData> {
        override fun createFromParcel(parcel: Parcel): StoryData {
            return StoryData(parcel)
        }

        override fun newArray(size: Int): Array<StoryData?> {
            return arrayOfNulls(size)
        }
    }

}