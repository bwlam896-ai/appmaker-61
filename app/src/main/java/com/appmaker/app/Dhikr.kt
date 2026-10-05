package com.appmaker.app

data class Dhikr(
    val id: Long = System.currentTimeMillis(),
    var text: String,
    var virtue: String = "",
    var category: String = "ذكر عام",
    var count: Int = 0,
    val target: Int = 33
)