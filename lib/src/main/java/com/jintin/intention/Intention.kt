package com.jintin.intention

import kotlin.reflect.KClass

annotation class Intention(val value: KClass<*>)

annotation class Extra(val value: String)

annotation class IntegerArrayListExtra(val value: String)

annotation class ParcelableArrayListExtra(val value: String)

annotation class StringArrayListExtra(val value: String)

annotation class CharSequenceArrayListExtra(val value: String)