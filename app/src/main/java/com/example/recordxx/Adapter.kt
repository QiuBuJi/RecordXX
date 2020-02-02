package com.example.recordxx

import android.app.AlertDialog.Builder
import android.content.Context
import android.support.v7.widget.RecyclerView.Adapter
import android.support.v7.widget.RecyclerView.ViewHolder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.TextView
import com.example.recordxx.DateTime.Companion.fillZero
import com.example.recordxx.MainActivity.Companion.etMemo
import com.example.recordxx.MainActivity.Companion.llm
import com.example.recordxx.MainActivity.Companion.spLevel
import com.example.recordxx.MainActivity.Companion.spWeather
import com.example.recordxx.MainActivity.Companion.tvDate
import com.example.recordxx.MainActivity.Companion.tvTime
import com.example.recordxx.MainActivity.Companion.tvTips
import java.util.*
import com.example.recordxx.Tree.*

class Adapter(private var context: Context, private val root: TimeTree<Mastur>) : Adapter<ListHolder>() {
    var adapterData: LinkedList<Tree<Mastur>> = LinkedList()

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): ListHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.sample_item_mastur, viewGroup, false)
        return ListHolder(view)
    }

    override fun onBindViewHolder(holder: ListHolder, i: Int) {
        val tree: Tree<Mastur> = adapterData[i]
        val left = (if (tree.specifyDepth > TimeField.NONE) tree.specifyDepth else tree.depth).dt - 1
        val density = context.resources.displayMetrics.density
        holder.view.scrollTo(0, 0)

        //设置左边距
        var lp = holder.circle.layoutParams as MarginLayoutParams
        lp.marginStart = (density * (8 + left * 16)).toInt()
        holder.circle.layoutParams = lp
        lp = holder.content.layoutParams as MarginLayoutParams
        lp.marginEnd = 0

        //设置显示文字
        holder.content.text = tree.toString()
        holder.line.visibility = View.GONE

        //刷新左侧图标
        refreshItemLeftIcom(tree, holder)

        //点击事件******************************************************
        //设置条目单击事件
        holder.view.setOnClickListener { tree.toggleNodeExpand(holder) }
        //被长按
        holder.view.setOnLongClickListener(onLongClick(tree))
        //删除按钮单击事件
        holder.delete.setOnClickListener(onDeleteClick(tree))
    }

    private fun onDeleteClick(tree: Tree<Mastur>): (View) -> Unit {
        return {
            val size = tree.leafCount
            if (size > 1) {
                val builder = Builder(context)
                builder.setTitle("包含" + size + "条内容，删除？")
                    .setIcon(R.mipmap.warnning_icon)
                    .setPositiveButton("是的") { _, _ -> deleteItem(tree) }
                    .setNegativeButton("放弃", null)
                    .create()
                    .show()
            } else deleteItem(tree)
        }
    }

    private fun onLongClick(tree: Tree<Mastur>): (View) -> Boolean {
        return {
            if (tree.depth > TimeField.MONTH) {
                var temp: Tree<Mastur>? = tree
                while (temp != null) {
                    if (temp.node.isEmpty()) break
                    temp = temp.node[0]
                }

                val time = temp!!.dateTime
                val resources = context.resources
                val weather = resources.getStringArray(R.array.list_weather)
                val level = resources.getStringArray(R.array.list_level)
                val weatherList = listOf(*weather)
                val levelList = listOf(*level)
                val indexWeather = weatherList.indexOf(temp.item!!.weather)
                val indexLevel = levelList.indexOf(temp.item!!.level)
                MainActivity.dateTime = time

                val month = fillZero(time.month + 1, 2)
                val day = fillZero(time.day, 2)
                tvDate.text = "${time.year}.${month}.${day}"

                val minute = fillZero(time.minute, 2)
                val hour = fillZero(time.hour, 2)
                tvTime.text = "$hour:$minute"

                spWeather.setSelection(indexWeather)
                spLevel.setSelection(indexLevel)
                etMemo.setText(temp.item!!.note)
                true
            } else false
        }
    }

    /**通知插入了数据*/
    fun notifyInsert(dateTime: DateTime): Boolean {
        val node = root.getNode(dateTime) ?: return false
        val selectDepth: TimeField = MainActivity.selectedDepth
        Tree.isDataChanged = true//提示数据已经改变了

        val visibleLeaves = root.root.allVisibleChildren
        val indexInsert = visibleLeaves.indexOf(node)
        node.specifyDepth = selectDepth
        adapterData.clear()
        adapterData.addAll(visibleLeaves)
        if (indexInsert >= 0) notifyItemInserted(indexInsert)//显示插入该节点的内容

        //刷新各节点的显示内容
        node.traverseToRoot {
            // 1.Year <== 2.Month <== 3.Day <== 4.Hour <== 5.Minute <== 6.Second

            //在没选择年月日时，设置它的显示深度
            if (node.specifyDepth == TimeField.NONE && it.node.size > 1)
                node.specifyDepth = it.depth + 1

            //更新父节点数据
            val index = adapterData.indexOf(it)
            if (index >= 0) notifyItemChanged(index)//刷新该节点的显示内容
            true
        }

        return true
    }

    /**删除条目数据*/
    fun deleteItem(tree: Tree<Mastur>) {
        val nodePare: Tree<Mastur> = tree.nodePare!!

        //删除该节点
        val position = adapterData.indexOf(tree)
        adapterData.remove(tree)
        notifyItemRemoved(position)

        //如果该节点是打开的，则要删除其子节点
        if (tree.isOpen) {
            val temp: LinkedList<*> = tree.allVisibleChildren
            adapterData.removeAll(temp)
            notifyItemRangeRemoved(position, temp.size)
        }

        //在父节点的Node中删除该节点
        nodePare.node.remove(tree)
        Tree.isDataChanged = true//提示数据已经改变了

        //父节点数量为1时，显示树不展开的样子
        if (nodePare.node.size == 1 && nodePare.depth > TimeField.MONTH) {
            nodePare.isOpen = false
            val tempTree = nodePare.node[0]
            val index = adapterData.indexOf(tempTree)
            adapterData.remove(tempTree)
            notifyItemRemoved(index)
        }

        nodePare.traverseToRoot {
            //更新父节点数据
            val index = adapterData.indexOf(it)
            notifyItemChanged(index)

            //删除父节点的子节点为空的节点
            if (it.node.isEmpty()) {
                it.nodePare?.run {
                    node.remove(it)
                    val index = adapterData.indexOf(it)
                    adapterData.remove(it)
                    if (index >= 0) notifyItemRemoved(index)
                }
            }
            true
        }

        llm.setVerticalScrollable(true)
    }

    /**
     * 节点展开&收缩
     * @param holder
     * @param expand 0 节点收缩，1 节点展开。其它数值toggle模式
     * */
    fun Tree<Mastur>.toggleNodeExpand(holder: ListHolder?, expand: Int = 3) {

        val condition =
            if (MainActivity.selectedDepth == TimeField.NONE)
                leafCount == 1 && depth > TimeField.MONTH
            else depth >= MainActivity.selectedDepth

        //节点可否点击
        if (condition) {
            tvTips.text =
                when {
                    depth == TimeField.SECOND -> item?.note ?: "---空---"
                    leaves.isNotEmpty()       -> leaves.last.item?.note ?: "---空---"
                    else                      -> "---空---"
                }
            return
        }

        //关闭其子节点属性为打开的节点
        traverse {
            if (it.isOpen) it.toggleNodeExpand(holder, 0)
            it.isOpen
        }

        isOpen = when (expand) {
            0    -> false
            1    -> true
            else -> !isOpen
        }

        //刷新左侧图标
        holder?.let { refreshItemLeftIcom(this, it) }

        val temp: List<Tree<Mastur>> = allVisibleChildren

        //删除还是添加，处理代码
        val indexOfNode: Int = adapterData.indexOf(this) + 1
        if (isOpen) {
            //增加元素
            adapterData.addAll(indexOfNode, temp)
            notifyItemRangeInserted(indexOfNode, temp.size)
        } else {
            //删除元素
            adapterData.removeAll(temp)
            notifyItemRangeRemoved(indexOfNode, temp.size)
        }
    }

    private fun refreshItemLeftIcom(tree: Tree<*>, holder: ListHolder) {
        //该节点只有1个叶节点...设置左边图标
        val condition =
            if (MainActivity.selectedDepth == TimeField.NONE)
                tree.leafCount == 1 && tree.depth > TimeField.MONTH
            else
                tree.depth >= MainActivity.selectedDepth

        if (condition) {
            holder.circle.setBackgroundResource(R.drawable.fg_click_color)
        } else {
            holder.circle.setBackgroundResource(//设置展开&收缩图标
                    if (tree.isOpen) R.drawable.triangle_top_down
                    else R.drawable.triangle_top_right)
        }
    }

    override fun getItemCount(): Int = adapterData.size
}

class ListHolder(var view: View) : ViewHolder(view) {
    var content: TextView = view.findViewById(R.id.listItem_textView_content)
    var circle: TextView = view.findViewById(R.id.listItem_textView_circle)
    var delete: TextView = view.findViewById(R.id.listItem_textView_delete)
    var line: TextView = view.findViewById(R.id.listItem_textView_line)
}