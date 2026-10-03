package com.appmaker.app

data class Dhikr(
    val id: Long = System.currentTimeMillis(),
    val text: String,
    val virtue: String = "",
    val category: String = "ذكر عام",
    var count: Int = 0,
    val target: Int = 33
)