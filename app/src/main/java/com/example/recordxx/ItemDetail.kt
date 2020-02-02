package com.example.recordxx

class ItemDetail {
    internal constructor(txt: String) {
        this.txt = txt
        type = ItemType.none
    }

    internal constructor(txt: String, type: ItemType) {
        this.txt = txt
        this.type = type
    }

    internal constructor(txt: String, type: ItemType, dateTime: DateTime?) {
        this.txt = txt
        this.type = type
        this.dateTime = dateTime
    }

    internal var txt: String
    internal var dateTime: DateTime? = null

    enum class ItemType {
        none, year, month, day
    }

    internal var type: ItemType
}