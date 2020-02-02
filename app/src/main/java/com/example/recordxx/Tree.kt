package com.example.recordxx

import java.util.*
import kotlin.collections.ArrayList

class Tree<E> {
    /**时间下标*/
    var index = 0
    /**该节点当前的深度*/
    var depth = TimeField.NONE
    /**用于指定显示数据，左边空格的数量*/
    var specifyDepth = TimeField.NONE
    /**保存的数据*/
    var item: E? = null
    /**盖节点的子节点*/
    var node = ArrayList<Tree<E>>()
    /**该节点的父节点*/
    var nodePare: Tree<E>? = null
    /**该节点是否为打开状态*/
    var isOpen = false

    companion object {
        /**判断数据是否改变，改变了则需要保存数据，否则不需要保存数据*/
        var isDataChanged = false
    }

    constructor(index: Int) {
        this.index = index
    }

    constructor(index: Int, item: E) {
        this.index = index
        this.item = item
    }

    constructor(index: Int, item: E?, nodePre: Tree<E>?) {
        this.index = index
        this.item = item
        nodePare = nodePre
    }

    fun add(iterator: Iterator<Int>, item: E): Tree<E>? {
        //有下一个数据
        if (iterator.hasNext()) {
            val index = iterator.next()
            var nodeTemp: Tree<E>? = null

            //如果节点数一致
            for (tree in node) {
                if (tree.index == index) {
                    nodeTemp = tree;
                    break
                }
            }

            //如果上面未匹配到数据
            if (nodeTemp == null) {
                var indexInsert = node.size//默认最大值，在末尾追加数据

                //找到合适的排序位置
                for ((indexTemp, tree) in node.withIndex()) {
                    if (tree.index > index) {
                        indexInsert = indexTemp;
                        break
                    }
                }

                //没有节点，则创建新节点
                nodeTemp = Tree(index, null, this)
                nodeTemp.depth = depth + 1
                node.add(indexInsert, nodeTemp)
            }
            return nodeTemp.add(iterator, item)
        }
        //没有下一个数据
        else {
            this.item?.let { return null }
            this.item = item
        }
        return this
    }

    operator fun get(iterator: Iterator<Int>): E? {
        var item: E? = null
        if (iterator.hasNext()) {
            val index = iterator.next()
            for (tree in node) {
                if (tree.index == index) {
                    item = tree[iterator];
                    break
                }
            }
        } else {
            item = this.item
        }
        return item
    }

    fun getNode(iterator: Iterator<Int>): Tree<E>? {
        var node: Tree<E>? = null
        if (iterator.hasNext()) {
            val index = iterator.next()

            for (tree in this.node) {
                if (tree.index == index) {
                    node = tree.getNode(iterator);
                    break
                }
            }
        } else node = this
        return node
    }

    fun contain(item: Tree<E>): Boolean {
        for (node in node) {
            if (node === item) return true
            else {
                val contain = node.contain(item)
                if (contain) return contain
            }
        }
        return false
    }

    /**
     * 取该节点下有效元素item的总数量
     * */
    val leafCount: Int
        get() {
            var elements = if (item == null) 0 else 1
            for (tree in node) elements += tree.leafCount
            return elements
        }

    val dateTime: DateTime
        get() {
            val dateTime = DateTime()
            this.traverseToRoot {
                when (it.depth) {
                    TimeField.YEAR   -> dateTime.year = it.index
                    TimeField.MONTH  -> dateTime.month = it.index
                    TimeField.DAY    -> dateTime.day = it.index
                    TimeField.HOUR   -> dateTime.hour = it.index
                    TimeField.MINUTE -> dateTime.minute = it.index
                    TimeField.SECOND -> dateTime.second = it.index
                    else             -> dateTime
                }
                true
            }
            return dateTime
        }

    fun subNodeCount(): Int {
        var elements = node.size
        for (tree in node) elements += tree.leafCount
        return elements
    }

    //没有子节点，就是叶节点
    val isLeafNode: Boolean
        get() = node.isNotEmpty()

    fun hasData(): Boolean = item != null

    /**
     * 遍历该节点下的所有节点
     * @return 返回true，遍历子节点。返回false，不遍历子节点
     * */
    fun traverse(action: (item: Tree<E>) -> Boolean) {
        for (tree in node) {
            val bValue = action(tree)
            if (bValue) tree.traverse(action)
        }
    }

    fun replace(iterator: Iterator<Int>, item: E): Boolean {
        return if (iterator.hasNext()) {
            val index = iterator.next()
            var nodeTemp: Tree<E>? = null

            for (tree in node) {
                if (tree.index == index) {
                    nodeTemp = tree; break
                }
            }
            nodeTemp?.replace(iterator, item) ?: false
        } else {
            this.item = item
            true
        }
    }

    val allChildren: LinkedList<Tree<E>>
        get() {
            val root = this
            val listTreeOut = LinkedList<Tree<E>>()
            root.traverse {
                if (it.depth > TimeField.DAY) {
                    if (it.node.size < 2) return@traverse false
                }
                listTreeOut.add(it)
                true
            }
            return listTreeOut
        }

    /**返回最大部分节点*/
    val maxOne: Tree<E>
        get() {
            if (node.isNotEmpty()) return node.last().maxOne
            return this
        }

    /**遍历每个节点的最大部分*/
    fun tillMaxOne(action: (tree: Tree<E>) -> Boolean): Unit {
        if (!action(this)) return
        if (node.isNotEmpty()) node.last().tillMaxOne(action)
    }

    /**
     * 遍历到根节点, 例如：
     * 1.Year <== 2.Month <== 3.Day <== 4.Hour <== 5.Minute <== 6.Second
     * @param action 遍历时要做的程序
     * */
    fun traverseToRoot(action: (tree: Tree<E>) -> Boolean) {
        if (depth > TimeField.NONE) {
            if (action(this)) this.nodePare?.traverseToRoot(action)
        }
    }

    /**叶节点*/
    val leaves: LinkedList<Tree<E>>
        get() {
            val listTreeOut = LinkedList<Tree<E>>()
            this.traverse {
                if (it.depth == TimeField.SECOND) listTreeOut.add(it)
                true
            }
            return listTreeOut
        }//没数据，结束循环//装载父节点数据

    /**到结尾，装载父节点数据*/
    val allVisibleChildren: LinkedList<Tree<E>>
        get() {
            val out = LinkedList<Tree<E>>()
            traverse {
                if (MainActivity.selectedDepth == TimeField.NONE) {
                    if (it.depth > TimeField.MONTH && it.leafCount == 1) {
                        val leaves = it.leaves
                        //分配显示深度
                        for (leaf in leaves) leaf.specifyDepth = it.depth
                        //添加所有叶节点元素
                        out.addAll(leaves)
                    } else out.add(it)
                } else {
                    if (it.depth == MainActivity.selectedDepth) {
                        val leaves = it.leaves
                        //分配显示深度
                        for (leaf in leaves) leaf.specifyDepth = MainActivity.selectedDepth
                        //添加所有叶节点元素
                        out.addAll(leaves)
                    } else out.add(it)
                }

                it.isOpen
            }
            return out
        }

    /**从秒向前记录，默认到月为止。内部已经设定好了格式*/
    fun getString(untilDepth: TimeField = TimeField.MONTH): String {
        var temp: Tree<E>? = this
        val sb = StringBuffer()

        loop@ while (temp != null) {
            //到了这个深度就停止
            if (temp.depth == untilDepth) break
            //把数字格式为2位的字符
            var term = temp.index.make2Bit()

            //单位为各种情况的处理：
            when (temp.unite) {
                "秒" -> {
                    val toString = temp.item.toString()
                    sb.append(" ").append(toString)
                    temp = temp.nodePare
                    continue@loop
                }
                "时" -> term += ":"
                "日" -> term += temp.unite + " "
            }
            sb.insert(0, term)//插入到最前
            temp = temp.nodePare//转到父节点
        }
        return sb.toString()
    }

    fun Int.make2Bit(): String = if (this < 10) "0$this" else this.toString()
    val strIndex: String
        get() = (if (this.unite == "月") this.index + 1 else this.index).make2Bit()//“月”类型数据，要把值+1

    override fun toString(): String {
        //如果是尾节点，执行其特定的特性
        secondToString()?.let { return it }

        var strLeafCount = " ($leafCount)"//把数量转换为字符串
        var note = ""

        //除去“年”&“月”的其它数据&该节点下的有效数据只有1个
        //该日如果只有1个叶节点，则显示该field下的全部数据
        if (MainActivity.selectedDepth == TimeField.NONE && depth > TimeField.MONTH && leafCount < 2 ||
            MainActivity.selectedDepth == depth) {
            strLeafCount = ""//不要括号及数字，例如: (127)
            var temp: Tree<*> = this
            val sbTime = StringBuffer()

            loop@ while (temp.node.isNotEmpty()) {
                temp = temp.node[0]
                val unite = when (temp.depth) {
                    TimeField.HOUR   -> ":"
                    TimeField.MINUTE -> ""
                    TimeField.SECOND -> continue@loop//不要“秒”的部分
                    else             -> temp.unite//其它情况，正常处理
                }
                sbTime.append(temp.strIndex).append(unite)
            }
            val space = " "
            note = space + sbTime.toString() + space + temp.item.toString() + space
        }

        return strIndex + unite + strLeafCount + note
    }

    /**生成节点是秒的字符串数据*/
    fun secondToString(): String? {
        if (depth == TimeField.SECOND) {
            var temp = nodePare
            val strTemp = StringBuffer()

            while (temp != null && temp.isOpen.not()) {
                val strIndex = temp.strIndex
                val str = when (temp.unite) {
                    "日", "月" -> "$strIndex${temp.unite}"
                    "时"      -> " $strIndex:"
                    "分"      -> strIndex
                    else     -> ""
                }
                strTemp.insert(0, str)
                temp = temp.nodePare
            }
            strTemp.append(" ${item.toString()}")
            if (strTemp.isNotEmpty()) return strTemp.toString()
        }
        return null
    }

    val unite: String
        get() = depth.toString()

    enum class TimeField(val dt: Int) {
        NONE(0),
        YEAR(1),
        MONTH(2),
        DAY(3),
        HOUR(4),
        MINUTE(5),
        SECOND(6);

        var units = arrayOf("root", "年", "月", "日", "时", "分", "秒")
        override fun toString(): String = units[dt]

        operator fun plus(i: Int): TimeField {
            return when (dt + i) {
                0    -> NONE
                1    -> YEAR
                2    -> MONTH
                3    -> DAY
                4    -> HOUR
                5    -> MINUTE
                6    -> SECOND
                else -> NONE
            }
        }

        operator fun minus(i: Int): TimeField {
            return when (dt - i) {
                0    -> NONE
                1    -> YEAR
                2    -> MONTH
                3    -> DAY
                4    -> HOUR
                5    -> MINUTE
                6    -> SECOND
                else -> NONE
            }
        }
    }
}