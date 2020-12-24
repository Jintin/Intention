# Intention
Intention is a tool to help you materialize your intent easily for Android.

All you need to do is to provide an interface contract of how to generate your Intent with context and your data.

Contract example:
```kotlin
@Intention(MainActivity::class)
interface MainActivityRouter {

    fun getIntent(
        context: Context,
        @Extra("MyKey") value: String?,
        @Extra("MyKey2") value2: Int = 345
    ): Intent

}
```
After compiler the generate class will name as the interface with suffix `Util` and you can use it directly!

Usage example:
```kotlin
// generate intent
val intent = MainActivityRouterUtil.getIntent(this, "myData")

// get data from intent
intent.getStringExtra("MyKey") // "myData"
intent.getIntExtra("MyKey2", 0) // 345
```