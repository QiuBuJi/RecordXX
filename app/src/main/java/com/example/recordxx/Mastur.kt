package com.example.recordxx

data class Mastur(var weather: String? = null,
                  var level: String? = null,
                  var note: String? = null) {

    override fun toString(): String = "$weather $level $note"
}