package com.jintin.intention.app.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.io.Serializable

@Parcelize
data class TestParcelable(val data: String) : Parcelable

data class TestSerializable(val data: String) : Serializable