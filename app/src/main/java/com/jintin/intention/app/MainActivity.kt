package com.jintin.intention.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.jintin.intention.Extra
import com.jintin.intention.Intention
import com.jintin.intention.app.data.TestParcelable
import com.jintin.intention.app.data.TestSerializable
import com.jintin.intention.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        println("string extra = " + intent.getStringExtra(EXTRA_STRING))
        println("serializable extra = " + intent.getSerializableExtra(EXTRA_SERIALIZABLE))
        println("parcelable extra = " + intent.getParcelableExtra(EXTRA_PARCELABLE))
        println("int extra = " + intent.getIntExtra(EXTRA_INT, 0))

        binding.button.setOnClickListener {
            val intent = TestRouterUtils.getIntent(
                this,
                "testvalue",
                TestParcelable("test1"),
                TestSerializable("test2"),
                456
            )
            println(intent)
            startActivity(intent)
        }

    }

    @Intention(MainActivity::class)
    interface TestRouter {
        fun getIntent(
            context: Context,
            @Extra(EXTRA_STRING) stringValue: String = "default value",
            @Extra(EXTRA_PARCELABLE) parcelable: TestParcelable,
            @Extra(EXTRA_SERIALIZABLE) serializable: TestSerializable,
            @Extra(EXTRA_INT) intValue: Int? = null,
            @Extra(EXTRA_BYTE) byteValue: Byte? = null,
            @Extra(EXTRA_CHAR) charValue: Char? = null,
            @Extra(EXTRA_SHORT) shortValue: Short? = null,
            @Extra(EXTRA_LONG) longValue: Long? = null,
            @Extra(EXTRA_DOUBLE) doubleValue: Double? = null,
            @Extra(EXTRA_FLOAT) floatValue: Float? = null,
            @Extra(EXTRA_BOOL) boolValue: Boolean? = null,
        ): Intent
    }

    companion object {
        const val EXTRA_PARCELABLE = "extra_parcelable"
        const val EXTRA_SERIALIZABLE = "extra_serializable"
        const val EXTRA_STRING = "extra_string"
        const val EXTRA_INT = "extra_int"
        const val EXTRA_BYTE = "extra_byte"
        const val EXTRA_CHAR = "extra_char"
        const val EXTRA_SHORT = "extra_short"
        const val EXTRA_LONG = "extra_long"
        const val EXTRA_DOUBLE = "extra_double"
        const val EXTRA_FLOAT = "extra_float"
        const val EXTRA_BOOL = "extra_bool"
    }
}