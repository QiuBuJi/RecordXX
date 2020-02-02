package com.example.recordxx

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.support.constraint.ConstraintLayout
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.example.recordxx.Tree.TimeField
import com.example.recordxx.util.SpanUtil
import kotlinx.android.synthetic.main.activity_main.*
import java.io.*
import java.util.*

class MainActivity : AppCompatActivity(), View.OnClickListener, View.OnLongClickListener {
    private val dataName = "masturbation log.txt"
    private val pathExternal = Environment.getExternalStorageDirectory()
    private val pathRoot = File(pathExternal, "RecordXX")
    private val pathData = File(pathRoot, dataName)
    lateinit var setting: SharedPreferences

    private lateinit var clTop: ConstraintLayout
    private lateinit var vsSwitch: ViewSwitcher
    private lateinit var rcList: RecyclerView
    private lateinit var ivCursor: ImageView
    private lateinit var tvDisplay: TextView
    private lateinit var tvTxt: TextView
    private lateinit var buSubmit: Button
    private lateinit var buSwitch: Button
    private lateinit var tvNoData: View
    private lateinit var button: Button
    private lateinit var rlMain: RelativeLayout
    private var adapter: Adapter? = null
    private var tree: TimeTree<Items> = TimeTree()
    private val handler = Handler() {
        when (it.what) {
            0 -> showLastTime()//显示距离上次的时间间隔
        }
        return@Handler true
    }
    private var timer = Timer()

    companion object {
        private val TAG = "msg_"
        lateinit var llm: LLManager

        lateinit var tvDate: TextView
        lateinit var tvTime: TextView
        lateinit var spWeather: Spinner
        lateinit var spSpinner: Spinner
        lateinit var spLevel: Spinner
        lateinit var etMemo: EditText
        lateinit var tvTips: TextView

        var dateTime: DateTime = DateTime()
        var selectedDepth: TimeField = TimeField.NONE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()

        main_tvDate.setOnClickListener(this::onClick)
        main_tvTime.setOnClickListener(this::onClick)
        main_tvDate.setOnLongClickListener(this::onLongClick)
        main_tvTime.setOnLongClickListener(this::onLongClick)
        main_etMemo.setOnLongClickListener(this::onLongClick)
        buSubmit.setOnClickListener(this::onClick)

        requestPermission()

        //从读存储器上读数据
        readData()

        //设置适配器
        adapter = Adapter(this, tree)
        rcList.adapter = adapter
        llm = LLManager(this)
        rcList.layoutManager = llm

        //设置可显示部分
        setting = getSharedPreferences("setting", 0)
        val field = setting.getInt("field", 0)
        main_spinner.setSelection(field)
        main_spinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(p0: AdapterView<*>?) = Unit
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) = select()
            }
    }

    private fun readData() {
        val list = pathRoot.list { _, str -> str.endsWith(".txt", true) }// FIXME: 2020.2.3 multiple file choose



        try {
            val fis = FileInputStream(pathData)
            val log = fis.readBytes()
            tree = TimeTree.makeTimeTree(String(log))//转换为内部数据

        } catch (e: IOException) {
            //发生异常执行的程序
            tvNoData.visibility = View.VISIBLE
            when (e) {
                is FileNotFoundException -> Unit
                else                     -> {
                    Toast.makeText(this, "数据错误！", Toast.LENGTH_LONG).show()
                }
            }
            e.printStackTrace()
        }
    }

    /**指纹识别*/
    private fun fingerprintInit() {
        val fgm = FingerprintManagerCompat.from(this)
        val callback = object : FingerprintManagerCompat.AuthenticationCallback() {
            override fun onAuthenticationError(errMsgId: Int, errString: CharSequence?) {
                super.onAuthenticationError(errMsgId, errString)
            }

            override fun onAuthenticationSucceeded(result: FingerprintManagerCompat.AuthenticationResult?) {
                super.onAuthenticationSucceeded(result)
                readData()
                select()
            }
        }

        if (fgm.isHardwareDetected) {
            val cancellationSignal = android.support.v4.os.CancellationSignal()
            fgm.authenticate(null, 0, cancellationSignal, callback, null)
        }
    }

    private fun select() {
        //恢复默认值
        tree.traverse { it.isOpen = false;true }

        //对应位置设置enable为false
        selectedDepth = when (main_spinner.selectedItem) {
            "年"  -> TimeField.MONTH
            "月"  -> TimeField.DAY
            "日"  -> TimeField.HOUR
            else -> TimeField.NONE
        }

        //展开最近离当前时间最近的数据
        adapter?.let { it ->
            val data = it.adapterData
            val node = tree.root.node
            data.clear()

            if (node.isNotEmpty()) {
                //展开最近离当前时间最近的数据
                val last = node.last()
                last.tillMaxOne {
                    it.isOpen = !((selectedDepth == TimeField.NONE && it.depth > TimeField.MONTH && it.leafCount < 2) ||
                                  it.depth == selectedDepth)
                    it.isOpen
                }
            }
            data.addAll(tree.root.allVisibleChildren)
            it.notifyDataSetChanged()
        }
    }

    private fun initViews() {
        rcList = findViewById(R.id.main_list)
        tvNoData = findViewById(R.id.main_tvNoData)
        clTop = findViewById(R.id.main_clTop)
        tvTxt = findViewById(R.id.main_tvArrive)

        tvDate = findViewById(R.id.main_tvDate)
        tvTime = findViewById(R.id.main_tvTime)
        spWeather = findViewById(R.id.main_spWeather)
        spLevel = findViewById(R.id.main_spLevel)
        buSubmit = findViewById(R.id.main_buSubmit)
        etMemo = findViewById(R.id.main_etMemo)
        tvTips = findViewById(R.id.main_tvTips)
        spSpinner = findViewById(R.id.main_spinner)

    }

    override fun onStart() {
        super.onStart()

        //滚动到底部
        adapter?.let {
            rcList.scrollToPosition(it.adapterData.size - 1)
        }

        timer.cancel()
        timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                handler.sendEmptyMessage(0)
            }
        }, 0, 1000)

        // TODO: 2020.1.30 设置指纹识别
        //设置指纹识别
//        fingerprintInit()
    }

    /**显示距离上次的时间间隔*/
    private fun showLastTime() {
        var root: Tree<Items>? = tree.root

        while (root != null) {
            if (root.depth == TimeField.DAY) {
                //取时间间隔（上次距今的时间）
                val leaves = root.leaves
                val lastLeaf: Tree<Items> = if (leaves.size > 1) leaves.last else leaves.first
                val currentTime = DateTime.getCurrentTime()
                val subtract = currentTime - lastLeaf.dateTime

                //设置个性化显示样式
                var strArriveTime = subtract.toAboutValue1()
                val unit: String
                if (strArriveTime.isNotEmpty()) {
                    unit = strArriveTime.substring(strArriveTime.length - 1, strArriveTime.length)
                    strArriveTime = strArriveTime.substring(0, strArriveTime.length - 1)

                    if (subtract.second < 0) strArriveTime = "-$strArriveTime"//负号的显示
                    SpanUtil.create()
                        .addForeColorSection(strArriveTime, 0xFF8E24AA.toInt())
                        .addForeColorSection(unit, Color.LTGRAY).setRelSize(unit, 0.4f)
                        .showIn(main_tvArrive)
                }
                break
            }

            root = try {
                root.node.last()
            } catch (e: Exception) {
                null
            }
        }
    }

    /***日期框 单击*/
    private fun dataOnClick(noChange: Boolean = false) {
        val split = main_tvDate.text.split(".")
        val currentTime =
            //使用显示的时间
            if (noChange) DateTime(
                    split[0].toInt(),
                    split[1].toInt() - 1,
                    split[2].toInt())
            //使用当前的时间
            else DateTime.getCurrentTime()

        //弹窗 选择日期
        DatePickerDialog(this, DatePickerDialog.OnDateSetListener { _, year, month, day ->
            dateTime.year = year
            dateTime.month = month
            dateTime.day = day

            refreshDataTimeShowing()
        }, currentTime.year, currentTime.month, currentTime.day).show() //初始化 弹窗 显示当前日期

        //日期 天 少1天
    }

    @SuppressLint("SetTextI18n")
    private fun refreshDataTimeShowing() {
        //取 弹窗选择 的日期，显示。
        main_tvDate.text = "${dateTime.year}.${fillChar(dateTime.month + 1)}.${fillChar(dateTime.day)}"
        //取 弹窗选择 的时间，显示。
        main_tvTime.text = "${fillChar(dateTime.hour)}:${fillChar(dateTime.minute)}"
    }

    /**
     * 格式化为 指定位数的字符，
     * @param num 要格式的数字
     * @param char 用于填充的字符
     * @param width 格式后字符的宽度（正常情况下）
     */
    private fun fillChar(num: Int, char: Char = '0', width: Int = 2): String {
        val strNum = num.toString()
        val fillNum = width - strNum.length
        val sb = StringBuffer()
        for (index in 0 until fillNum) sb.append(char)
        sb.append(strNum)
        return sb.toString()
    }

    /**时间框 单击*/
    private fun timeOnClick(noChange: Boolean = false) {
        val hour: Int
        val minute: Int

        if (noChange) {
            //使用显示的时间
            val split = main_tvTime.text.split(":")
            hour = split[0].toInt()
            minute = split[1].toInt()
        } else {
            //使用当前的时间
            val currentTime = DateTime.getCurrentTime()
            hour = currentTime.hour
            minute = currentTime.minute
        }

        //弹窗 选择时间
        TimePickerDialog(this, TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
            dateTime.hour = hourOfDay
            dateTime.minute = minute
            dateTime.second = 0

            refreshDataTimeShowing()
        }, hour, minute, true).show() //初始化 弹窗 显示当前时间，弹窗 显示。

    }

    /**日期 时间 初始化*/
    @SuppressLint("SetTextI18n")
    private fun initDateTime() {
        dateTime = DateTime.getCurrentTime()
        dateTime.second = 0

        //取 弹窗选择 的日期，显示。
        main_tvDate.text = "${dateTime.year}.${fillChar(dateTime.month + 1)}.${fillChar(dateTime.day)}"
        //取 弹窗选择 的时间，显示。
        main_tvTime.text = "${fillChar(dateTime.hour)}:${fillChar(dateTime.minute)}"
    }

    override fun onResume() {
        super.onResume()
        initDateTime()
    }

    override fun onStop() {
        super.onStop()
        setting.edit().putInt("field", main_spinner.selectedItemPosition).apply()
        timer.cancel()
        if (Tree.isDataChanged) {
            Tree.isDataChanged = false
            dataStore()
        }
    }

    /**保存数据*/
    private fun dataStore() {
        val sb = StringBuffer()
        tree.traverse {
            //深度为DEPTH_DAY时，处理1日1条或不止1条数据的情况
            if (it.depth == TimeField.DAY) {

                for (leaf in it.leaves) {
                    var leaf = leaf
                    val strData = leaf.item?.toString() ?: ""
                    val strTime = "${leaf.nodePare?.nodePare?.strIndex}:${leaf.nodePare?.strIndex}"

                    leaf = leaf.nodePare?.nodePare?.nodePare!!
                    sb.append("\n${leaf.strIndex}${leaf.unite} $strTime $strData")
                }
                false
            } else {
                //刚开始，不要插入空行
                if (sb.isNotEmpty()) {
                    when (it.depth) {
                        TimeField.YEAR  -> sb.append("\n\n")//插入2行空行
                        TimeField.MONTH -> sb.append("\n") //插入1行空行
                    }
                }

                sb.append("\n${it.strIndex}${it.unite}(${it.leafCount})")
                true
            }
        }
        if (sb.isNotEmpty()) sb.deleteCharAt(0)

        //备份数据
        val backupFile = File(pathRoot, "${dataName}.backup.txt")
        if (!pathRoot.exists()) pathRoot.mkdirs()
        if (!backupFile.exists()) backupFile.createNewFile()//没有文件，则创建文件
        if (!pathData.exists()) pathData.createNewFile()

        val fileIn = FileInputStream(pathData)
        var fos = FileOutputStream(backupFile)
        val readBytes = fileIn.readBytes()

        fos.write(readBytes)
        fos.close()

        //保存起来
        fos = FileOutputStream(pathData)
        fos.write(sb.toString().toByteArray())
        fos.close()
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.main_tvDate   -> dataOnClick()
            R.id.main_tvTime   -> timeOnClick()
            R.id.main_buSubmit -> submitOnClick()
        }
    }

    private fun memoOnClick() {
        val notes = getTreeNotes()
        val pop = ListPopupWindow(this)

        notes.reverse()

        pop.setAdapter(ArrayAdapter<String>(this, R.layout.sample_text, notes))
        pop.width = ViewGroup.LayoutParams.WRAP_CONTENT
        pop.height = pop.width
        pop.anchorView = main_etMemo
        pop.isModal = true
        pop.setOnItemClickListener { _, _, i, _ ->
            main_etMemo.setText(notes[i])
            pop.dismiss()
        }
        pop.show()
    }

    private fun getTreeNotes(): LinkedList<String> {
        val notes = LinkedList<String>()
        val temp = LinkedList<String>()

        tree.traverse { it ->
            val note = it.item?.item3
            note?.let {
                if (it.matches(Regex("\\s+")).not() && it.isNotEmpty()) temp.add(it)
            }
            true
        }

        //去重复
        while (temp.size > 0) {
            val pop = temp.pop()
            notes.add(pop)
            val iterator = temp.iterator()

            while (iterator.hasNext()) {
                if (iterator.next() == pop) iterator.remove()
            }
        }
        return notes
    }

    override fun onLongClick(view: View): Boolean {
        when (view.id) {
            R.id.main_tvDate -> dataOnClick(true)
            R.id.main_tvTime -> timeOnClick(true)
            R.id.main_etMemo -> memoOnClick()
        }
        return true
    }

    /**提交按钮被单击*/
    private fun submitOnClick() {
        val adapter = adapter ?: return
        tvNoData.visibility = View.INVISIBLE
        val item = Items(main_spWeather.selectedItem.toString() + " " +
                         main_spLevel.selectedItem.toString() + " " +
                         main_etMemo.text.toString())
        val addedValue = tree.add(dateTime, item)

        if (addedValue != null) adapter.notifyInsert(dateTime)
        else {
            AlertDialog.Builder(this)
                .setTitle("数据已经存在！确定要替换吗？")
                .setPositiveButton("确定") { _, _ ->
                    val node: Tree<Items>? = tree.getNode(dateTime)
                    tree.replace(dateTime, item)
                    Tree.isDataChanged = true//提示数据已经改变了

                    //转到显示的部分的节点
                    node?.let { it ->
                        val node1 = tree.getNode(dateTime)!!
                        val indexOf = adapter.adapterData.indexOf(it)
                        adapter.adapterData.remove(it)
                        adapter.adapterData.add(indexOf, node1)
                        adapter.notifyItemChanged(indexOf)
                    }
                }
                .setNegativeButton("取消", null)
                .show()

        }

        //显示距离上次的时间间隔
        showLastTime()
    }

    /**请求权限*/
    private fun requestPermission() {
        val strPermissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val listPermissions = LinkedList<String>()

        //检查软件是否有权限
        for (strPermission in strPermissions) {
            val state = checkSelfPermission(strPermission)
            if (state == PackageManager.PERMISSION_DENIED) listPermissions.add(strPermission)
        }

        //没有权限才则请求
        if (listPermissions.isNotEmpty()) {
            val strPermissionTemp = arrayOfNulls<String>(listPermissions.size)
            for (i in listPermissions.indices) strPermissionTemp[i] = listPermissions[i]
            requestPermissions(strPermissionTemp, 1)
        }
    }

    /**权限请求结果反馈*/
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        //显示权限请求失败结果
        val iterator = permissions.iterator()
        val denied = StringBuffer()
        for (grantResult in grantResults) {
            if (grantResult == PackageManager.PERMISSION_DENIED) denied.append(iterator.next() + "\n")
        }
        if (denied.isNotEmpty())
            Toast.makeText(this, "权限\"${denied}\"请求失败...", Toast.LENGTH_LONG).show()
    }

}