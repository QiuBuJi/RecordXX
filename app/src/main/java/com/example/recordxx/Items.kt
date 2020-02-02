package com.example.recordxx

open class Items(var item1: String? = null,
                 var item2: String? = null,
                 var item3: String? = null) {

    constructor(src: String) : this() {
        val split = src.trim().split("\\s".toRegex())
        val sb = StringBuffer()

        for (index in split.indices) {
            when (index) {
                0    -> item1 = split[index]
                1    -> item2 = split[index]
                else -> sb.append(" ").append(split[index])
            }
        }
        item3 = sb.replace(0, 1, "").toString()
    }

    override fun toString(): String = "$item1 $item2 $item3"
}


class Mastur(var weather: String? = null,
             var level: String? = null,
             var note: String? = null) : Items(weather, level, note)// FIXME: 2020.2.3 data convert to another same data?

data class Gymnastic(var times: String,
                     var costs: String,
                     var note: String?) : Items(times, costs, note)