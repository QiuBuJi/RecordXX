package com.example.recordxx

class TimeTree<E> {
    val root = Tree<E>(666)

    internal fun add(time: DateTime, item: E): Tree<E>? {
        val list = getList(time)
        val iterator = list.iterator()
        return root.add(iterator, item)
    }

    internal fun replace(time: DateTime, item: E): Boolean {
        val list = getList(time)
        val iterator = list.iterator()
        return root.replace(iterator, item)
    }

    internal operator fun get(time: DateTime): E? = root[getList(time).iterator()]

    internal fun getNode(dateTime: DateTime): Tree<E>? {
        val integers = getList(dateTime)
        return root.getNode(integers.iterator())
    }

    private var count = 0
    private var indexOfShowed = 0
    fun indexOfVisible(tree: Tree<Items>): Int {
        count = 0
        indexOfShowed = count

        root.traverse {
            count++
            if (it == tree) indexOfShowed = count
            it.isOpen
        }
        return --indexOfShowed
    }

    fun traverse(action: (item: Tree<E>) -> Boolean) = root.traverse(action)

    override fun toString(): String = root.leafCount.toString() + "个节点"

    private fun getList(time: DateTime): List<Int> = arrayListOf(time.year, time.month, time.day, time.hour, time.minute, time.second)

    companion object {
        fun makeTimeTree(log: String): TimeTree<Items> {
            val result: TimeTree<Items> = TimeTree()
            val strLineSet = log.split("\n").toTypedArray()
            val dateTime = DateTime()

            for (str in strLineSet) {
                when {
                    str.contains("年") -> dateTime.year = makeNumber(str, "年")
                    str.contains("月") -> dateTime.month = makeNumber(str, "月") - 1
                    str.contains("日") -> {
                        dateTime.day = makeNumber(str, "日")

                        val segs = str.split("\\s".toRegex()).toTypedArray()
                        val strTimeBothSet = segs[1].split(":").toTypedArray()

                        dateTime.hour = strTimeBothSet[0].toInt()
                        dateTime.minute = strTimeBothSet[1].toInt()
                        dateTime.second = 0

                        val value: Items = when {
                            segs.size > 5  -> {
                                val sb = StringBuilder()
                                for (i in 4 until segs.size) sb.append(segs[i]).append(" ")
                                val strTrimmed = sb.trim().toString()

                                Items(segs[2] + " " + segs[3] + " " + strTrimmed)
                            }
                            segs.size == 5 -> Items(segs[2] + " " + segs[3] + " " + segs[4])
                            else           -> Items(segs[2] + " " + segs[3] + " " + "")
                        }
                        result.add(dateTime, value)
                        val mastur = value as Mastur

                    }
                }
            }
            return result
        }

        internal fun makeNumber(str: String, match: String): Int {
            val index = str.indexOf(match)
            val strNumber = str.substring(0, index)
            return strNumber.toInt()
        }
    }
}