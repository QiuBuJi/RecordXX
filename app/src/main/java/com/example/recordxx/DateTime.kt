package com.example.recordxx

import android.annotation.SuppressLint
import com.example.review.New.StoreData
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.util.*
import kotlin.math.abs

class DateTime : StoreData {
    var year = 0
    var month = 0
    var day = 0
    var hour = 0
    var minute = 0
    var second = 0

    var mastur = Items()

    constructor() {}
    constructor(rawBytes: ByteArray) {
        loadWith(rawBytes)
//        try {
//        } catch (e: IOException) {
//            e.printStackTrace()
//        }
    }

    constructor(years: Int, months: Int, days: Int, hours: Int, minuts: Int, seconds: Int) {
        year = years
        month = months
        day = days
        hour = hours
        minute = minuts
        second = seconds
    }

    constructor(years: Int, months: Int, days: Int) {
        year = years
        month = months
        day = days
    }

    constructor(dateTime: DateTime) {
        year = dateTime.year
        month = dateTime.month
        day = dateTime.day
        hour = dateTime.hour
        minute = dateTime.minute
        second = dateTime.second
    }

    constructor(calendar: Calendar) {
        year = calendar[Calendar.YEAR]
        month = calendar[Calendar.MONTH]
        day = calendar[Calendar.DAY_OF_MONTH] //- 1
        hour = calendar[Calendar.HOUR_OF_DAY]
        minute = calendar[Calendar.MINUTE]
        second = calendar[Calendar.SECOND]
    }

    private fun getInt(src: String, objStr: String): Int {
        var index1: Int
        val index2 = src.indexOf(objStr)
        if (index2 == -1) return 0
        index1 = index2
        var c: Char
        do {
            if (index1 == 0) {
                index1 = -1
                break
            }
            c = src[--index1]
        } while (c.toInt() == 0x2d || c.toInt() >= 0x30 && c.toInt() <= 0x39)
        val substring = src.substring(++index1, index2)
        return Integer.valueOf(substring)
    }

    constructor(str: String) {
        year = getInt(str, "年")
        month = getInt(str, "月")
        day = getInt(str, "日")
        hour = getInt(str, "时")
        minute = getInt(str, "分")
        second = getInt(str, "秒")
    }

    //设置该时间域及以下域为0
    fun setZeroSegment(timeFieldEnum: TimeFieldEnum?) {
        when (timeFieldEnum) {
            TimeFieldEnum.YEAR   -> {
                year = 0
                month = 0
                day = 0
                hour = 0
                minute = 0
                second = 0
            }
            TimeFieldEnum.MONTH  -> {
                month = 0
                day = 0
                hour = 0
                minute = 0
                second = 0
            }
            TimeFieldEnum.DAY    -> {
                day = 0
                hour = 0
                minute = 0
                second = 0
            }
            TimeFieldEnum.HOUR   -> {
                hour = 0
                minute = 0
                second = 0
            }
            TimeFieldEnum.MINUTE -> {
                minute = 0
                second = 0
            }
            TimeFieldEnum.SECOND -> second = 0
        }
    }

    //设置该时间域及以下域为0
    fun setZeroSegment(field: Int) {
        require(!(field > 5 || field < 0))
        when (field) {
            0 -> {
                year = 0
                month = 0
                day = 0
                hour = 0
                minute = 0
                second = 0
            }
            1 -> {
                month = 0
                day = 0
                hour = 0
                minute = 0
                second = 0
            }
            2 -> {
                day = 0
                hour = 0
                minute = 0
                second = 0
            }
            3 -> {
                hour = 0
                minute = 0
                second = 0
            }
            4 -> {
                minute = 0
                second = 0
            }
            5 -> second = 0
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val dateTime = other as DateTime
        return year == dateTime.year && month == dateTime.month && day == dateTime.day && hour == dateTime.hour && minute == dateTime.minute && second == dateTime.second
    }

    override fun hashCode(): Int {
        return Objects.hash(year, month, day, hour, minute, second)
    }

    /**
     * 判断本时间，是否比参数的时间大
     *
     * @param dateTime 参数时间
     * @return true 比参数的时间要大；false 比参数的时间小或者相等
     */
    fun biggerThan(dateTime: DateTime): Boolean {
        val value = compareTo(dateTime)
        return value > 0
    }

    /**
     * 比较两个数据，谁大一些
     *
     * @param dt 用于比较的另一个参数
     * @return 返回值：0一样大，1本数据大，-1本数据小
     */
    operator fun compareTo(dt: DateTime): Int {
        return if (dt.year == year) {
            if (dt.month == month) {
                if (dt.day == day) {
                    if (dt.hour == hour) {
                        if (dt.minute == minute) {
                            if (dt.second == second) return 0 else second - dt.second
                        } else minute - dt.minute
                    } else hour - dt.hour
                } else day - dt.day
            } else month - dt.month
        } else year - dt.year
    }

    fun addTo(calendar: Calendar) {
        calendar.add(Calendar.YEAR, year)
        calendar.add(Calendar.MONTH, month)
        calendar.add(Calendar.DAY_OF_MONTH, day)
        calendar.add(Calendar.HOUR_OF_DAY, hour)
        calendar.add(Calendar.MINUTE, minute)
        calendar.add(Calendar.SECOND, second)
    }

    /**
     * 取两个数据之差
     *
     * @param subtrahend 用于减去的时间
     * @return 2个时间的差值
     */
    fun subtract(subtrahend: DateTime): DateTime {
        var subtrahend = DateTime(subtrahend)
        var minuend = DateTime(this)

        return if (minuend > subtrahend) minuend.subtractOf(subtrahend) else {
            subtrahend.subtractOf(minuend)
//            subtrahend.year = -subtrahend.year
//            subtrahend.month = -subtrahend.month
//            subtrahend.day = -subtrahend.day
//            subtrahend.hour = -subtrahend.hour
//            subtrahend.minute = -subtrahend.minute
            subtrahend.second = -subtrahend.second
            subtrahend
        }
    }

    operator fun minus(divisor: DateTime): DateTime {
        return subtract(divisor)
    }

    /**
     * 取两个数据之差，保存于本数据中
     *
     * @param dateTime 用于减去的时间
     */
    fun subtractOf(dateTime: DateTime): DateTime {
        var borrow = subtractOf(BorrowStru(second, minute), dateTime.second, 60)
        second = borrow.lowOrder
        minute = borrow.highOrder
        borrow = subtractOf(BorrowStru(minute, hour), dateTime.minute, 60)
        minute = borrow.lowOrder
        hour = borrow.highOrder
        borrow = subtractOf(BorrowStru(hour, day), dateTime.hour, 24)
        hour = borrow.lowOrder
        day = borrow.highOrder

        var multiple = 30
        if (month == 1) { //闰年判断
            multiple = if (year % 4 == 0 && year % 100 > 0) 29 else 28
        } else if (month == 0 || month == 2 || month == 4 || month == 6 || month == 7 || month == 9 || month == 11) multiple = 31

        borrow = subtractOf(BorrowStru(day, month), dateTime.day, multiple)
        day = borrow.lowOrder
        month = borrow.highOrder
        borrow = subtractOf(BorrowStru(month, year), dateTime.month, 12)
        month = borrow.lowOrder
        year = borrow.highOrder
        year -= dateTime.year
        return this
    }

    /**
     * 把两个时间相加，保存于本时间中
     *
     * @param dateTime 用于相加的时间，可以为负值
     */
    fun add(dateTime: DateTime) {
        val dt = DateTime(dateTime)
        dt.year = -dt.year
        dt.month = -dt.month
        dt.day = -dt.day
        dt.hour = -dt.hour
        dt.minute = -dt.minute
        dt.second = -dt.second
        subtractOf(dt)
    }

    internal inner class BorrowStru(var lowOrder: Int, var highOrder: Int)

    /**
     * 两个数据相加和进位
     *
     * @param `val`        保存高位&低位的数据结构
     * @param subtractor 减数，用于减去的数值
     * @param multiple   进位标志数，如满10进1的10、满60进1的60
     * @return 返回已经被相加&进位了的数据
     */
    private fun subtractOf(value: BorrowStru, subtractor: Int, multiple: Int): BorrowStru {
        value.lowOrder -= subtractor
        if (value.lowOrder < 0) { //得出要借的数量
            var times = Math.abs(value.lowOrder / multiple)
            //val.lowOrder还有余数的话，要多借1
            value.lowOrder %= multiple
            times += if (value.lowOrder == 0) 0 else 1
            //向高位借数
            value.highOrder -= times
            //todo ??
            value.lowOrder = (if (value.lowOrder == 0) 0 else multiple) + value.lowOrder
        } else if (value.lowOrder >= multiple) { //低位数大于进制数，则向高位进位
            val times = value.lowOrder / multiple
            value.lowOrder %= multiple
            value.highOrder += times
        }
        return value
    }

    override fun toString(): String {
        val buffer = StringBuilder()
        buffer.append(fillZero(year, 4)).append("年")
            .append(fillZero(month + 1)).append("月")
            .append(fillZero(day)).append("日 ")
            .append(fillZero(hour)).append("时")
            .append(fillZero(minute)).append("分")
            .append(fillZero(abs(second))).append('秒')
            .append(if (second < 0) " ✘" else " ") //✔
            .append(" [ $year.$month.$day $hour:$minute:$second ]")

        return buffer.toString()
    }

    fun toString(simplify: Boolean = false): String {
        val buffer = StringBuilder()
        if (simplify) {
            buffer.append(fillZero(year, 4)).append(".")
                .append(fillZero(month + 1)).append(".")
                .append(fillZero(day)).append(" ")
                .append(fillZero(hour)).append(":")
                .append(fillZero(minute)).append(":")
                .append(fillZero(Math.abs(second)))
        } else return toString()
        return buffer.toString()
    }

    fun toStringTime(): String {
        val time = StringBuilder()
        if (year > 0) time.append(String.format("%d年", year))
        if (month >= 0) time.append(String.format("%d月", month + 1))
        if (day > 0) time.append(String.format("%d日", day + 1))
        if (hour > 0) time.append(String.format("%d时", hour))
        if (minute > 0) time.append(String.format("%d分", minute))
        if (second > 0) time.append(String.format("%d秒", second))
        return time.toString()
    }

    @SuppressLint("DefaultLocale")
    fun toAboutValue(): String {
        val strTime = StringBuilder()
        val rate: Int
        val value = 0
        if (year > 0) {
            rate = (month / 12f * 10).toInt()
            strTime.append(year)
            if (rate > value) strTime.append('.').append(rate)
            strTime.append("年")
        } else if (month > 0) {
            rate = (day / 31f * 10).toInt()
            strTime.append(month + 1)
            if (rate > value) strTime.append('.').append(rate)
            strTime.append("月")
        } else if (day > 0) {
            rate = (hour / 24f * 10).toInt()
            strTime.append(day)
            if (rate > value) strTime.append('.').append(rate)
            strTime.append("日")
        } else if (hour > 0) {
            rate = (minute / 60f * 10).toInt()
            strTime.append(hour)
            if (rate > value) strTime.append('.').append(rate)
            strTime.append("时")
        } else if (minute > 0) {
            rate = (second / 60f * 10).toInt()
            strTime.append(minute)
            if (rate > value) strTime.append('.').append(rate)
            strTime.append("分")
        } else if (second > 0) strTime.append(second).append("秒")
        return strTime.toString()
    }

    fun toAboutValue1(): String {
        val strTime = StringBuilder()
        when {
            year > 0   -> strTime.append(year).append("年")
            month > 0  -> strTime.append(month).append("月")
            day > 0    -> strTime.append(day).append("日")
            hour > 0   -> strTime.append(hour).append("时")
            minute > 0 -> strTime.append(minute).append("分")
            second > 0 -> strTime.append(second).append("秒")
        }
        return strTime.toString()
    }

    fun toAboutValueNoDot(): String {
        val strTime = StringBuilder()
        if (year > 0) strTime.append(String.format("%d年", year))
        else if (month > 0) strTime.append(String.format("%d月", month + 1))
        else if (day > 0) strTime.append(String.format("%d日", day))
        else if (hour > 0) strTime.append(String.format("%d时", hour))
        else if (minute > 0) strTime.append(String.format("%d分", minute))
        else if (second > 0) strTime.append(String.format("%d秒", second))
        return strTime.toString()
    }

    fun toNoneZeroString(): StringBuffer {
        val time = StringBuffer()
        if (year > 0) time.append(String.format("%d年", year))
        if (month > 0) time.append(String.format("%d月", month + 1))
        if (day > 0) time.append(String.format("%d日 ", day))
        if (hour > 0) time.append(String.format("%s:", fillZero(hour, 2)))
        time.append(String.format("%s:%s", fillZero(minute, 2), fillZero(second, 2)))
        return time
    }

    @Throws(IOException::class)
    override fun toBytes(dos: DataOutputStream) {
        dos.writeShort(year) //2
        dos.writeByte(month) //1
        dos.writeByte(day) //1
        dos.writeByte(hour) //1
        dos.writeByte(minute) //1
        dos.writeByte(second) //1
    }

    @Throws(IOException::class)
    override fun loadWith(dis: DataInputStream) {
        year = dis.readShort().toInt()
        month = dis.readByte().toInt()
        day = dis.readByte().toInt()
        hour = dis.readByte().toInt()
        minute = dis.readByte().toInt()
        second = dis.readByte().toInt()
    }

    enum class TimeFieldEnum {
        YEAR, MONTH, DAY, HOUR, MINUTE, SECOND
    }

    companion object {
        var YEAR = 0
        var MONTH = 1
        var DAY = 2
        var HOUR = 3
        var MINUTE = 4
        var SECOND = 5

        fun getCurrentTime() = DateTime(Calendar.getInstance())

        /**
         * 数字不够位数，则在前面填充0
         *
         * @param value 要填充的数字
         * @param width 填充好的字符，总的位数
         * @param ch    要填充的字符
         * @return 填充好的字符串
         */
        fun fillChar(value: Int, width: Int, ch: Char): String {
            var width = width
            var temp = Math.abs(value)
            val sb = StringBuilder()
            var count = 0
            do {
                temp /= 10
                count++
            } while (temp > 0)
            if (count > width) width = count
            width -= count
            if (value < 0) sb.append('-')
            for (i in 0 until width) sb.append(ch)
            sb.append(Math.abs(value))
            return sb.toString()
        }

        /**
         * 数字不够位数，则在前面填充0
         *
         * @param value 要填充的数字
         * @param width 填充好的字符，总的位数
         * @return 填充好的字符串
         */
        fun fillZero(value: Int, width: Int = 2): String {
            return fillChar(value, width, '0')
        }

        var MAX_BYTES = 7
    }
}