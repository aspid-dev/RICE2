package ru.rice.agent

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("rice_local", MODE_PRIVATE) }
    private lateinit var status: TextView
    private lateinit var tasksBox: LinearLayout
    private lateinit var memoryCount: TextView
    private val permissionCode = 702
    private val exportCode = 703

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RiceLog.info("ACTIVITY", "MainActivity onCreate")
        try {
            if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                showPermissionScreen()
            } else {
                RiceLog.enablePublicLog(this)
                showMainScreen()
            }
        } catch (t: Throwable) {
            RiceLog.error("STARTUP", "Activity startup failed", t)
            showRecovery(t)
        }
    }

    private fun showPermissionScreen() {
        val page = newColumn()
        page.addView(text("RICE · подготовка", 24f, 0xFF173B35.toInt(), true))
        page.addView(text("Разрешение нужно только для сохранения журнала в Documents/RICE. Без него приложение тоже работает, а журнал можно экспортировать вручную.", 16f, 0xFF263A32.toInt()))
        addButton(page, "Разрешить сохранение журнала") {
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), permissionCode)
        }
        addButton(page, "Продолжить без разрешения") { showMainScreen() }
        setContentView(page)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, results)
        if (requestCode == permissionCode) {
            val granted = results.isNotEmpty() && results[0] == PackageManager.PERMISSION_GRANTED
            RiceLog.info("PERMISSION", "WRITE_EXTERNAL_STORAGE granted=$granted")
            if (granted) RiceLog.enablePublicLog(this)
            showMainScreen()
        }
    }

    private fun showMainScreen() {
        try {
            val page = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(0xFFF3F5F2.toInt())
            }
            val header = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(0xFF173B35.toInt())
                setPadding(dp(20), dp(18), dp(20), dp(14))
            }
            header.addView(text("RICE", 30f, 0xFFFFFFFF.toInt(), true))
            header.addView(text("Локальная основа · 0.2.0", 14f, 0xFFD5E5DE.toInt()))
            status = text("Запущен. Сеть не используется.", 13f, 0xFFD5E5DE.toInt())
            header.addView(status)
            page.addView(header)

            val scroll = ScrollView(this)
            val body = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(18), dp(10), dp(18), dp(26))
            }
            scroll.addView(body)
            page.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

            section(body, "ДИАГНОСТИКА")
            body.addView(text("Журнал: ${RiceLog.publicPath()}", 12f, 0xFF68766F.toInt()))
            addButton(body, "Экспортировать журнал") { exportLog() }

            section(body, "ПАМЯТЬ")
            val note = EditText(this).apply {
                hint = "Записать мысль или наблюдение…"
                minLines = 2
                gravity = Gravity.TOP
            }
            body.addView(note, match())
            addButton(body, "Сохранить в локальную память") {
                val value = note.text.toString().trim()
                if (value.isNotEmpty()) {
                    val entries = readArray("memory")
                    entries.put(JSONObject().put("text", value).put("time", System.currentTimeMillis()))
                    saveArray("memory", entries)
                    RiceLog.info("MEMORY", "Saved memory entry; count=${entries.length()}")
                    note.setText("")
                    memoryCount.text = "Записей в памяти: ${entries.length()}"
                    setStatus("Запись сохранена на устройстве.")
                }
            }
            memoryCount = text("Записей в памяти: ${readArray("memory").length()}", 12f, 0xFF68766F.toInt())
            body.addView(memoryCount)

            section(body, "ЗАДАЧИ")
            val taskInput = EditText(this).apply { hint = "Новая задача…"; setSingleLine(true) }
            body.addView(taskInput, match())
            addButton(body, "Добавить задачу") {
                val value = taskInput.text.toString().trim()
                if (value.isNotEmpty()) {
                    val tasks = readArray("tasks")
                    tasks.put(JSONObject().put("text", value).put("done", false).put("origin", "user"))
                    saveArray("tasks", tasks)
                    RiceLog.info("TASK", "User added task")
                    taskInput.setText("")
                    renderTasks()
                }
            }
            addButton(body, "Предложить исследовательскую задачу") { proposeTask(); renderTasks() }
            tasksBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            body.addView(tasksBox, match())
            renderTasks()

            section(body, "ПРИНЦИПЫ РАБОТЫ")
            body.addView(text("• Работа без интернета\n• Память хранится на этом устройстве\n• Цели и задачи пока выбираются вручную или предлагаются простыми локальными правилами\n• Облачных сервисов и сетевых запросов в проекте нет", 14f, 0xFF33443D.toInt()))
            setContentView(page)
            RiceLog.info("UI", "Main screen rendered")
        } catch (t: Throwable) {
            RiceLog.error("UI", "Main screen failed", t)
            showRecovery(t)
        }
    }

    private fun showRecovery(error: Throwable) {
        try {
            val page = newColumn()
            page.addView(text("RICE · восстановление", 24f, 0xFF173B35.toInt(), true))
            page.addView(text("Основной экран не открылся. Ошибка записана. Можно сохранить журнал и отправить его для диагностики.", 15f, 0xFF263A32.toInt()))
            page.addView(text("${error.javaClass.simpleName}: ${error.message ?: "без описания"}", 12f, 0xFF68766F.toInt()))
            addButton(page, "Сохранить журнал в файл") { exportLog() }
            addButton(page, "Попробовать открыть Риса снова") { showMainScreen() }
            setContentView(page)
        } catch (fatal: Throwable) {
            RiceLog.error("RECOVERY", "Recovery screen failed", fatal)
            finish()
        }
    }

    private fun exportLog() {
        RiceLog.info("EXPORT", "User requested log export")
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/plain"
            putExtra(Intent.EXTRA_TITLE, "RICE_LOG.txt")
        }
        try {
            startActivityForResult(intent, exportCode)
        } catch (t: Throwable) {
            RiceLog.error("EXPORT", "Cannot open file picker", t)
            setStatus("Не удалось открыть выбор файла: ${t.message}")
        }
    }

    @Deprecated("Android 8.1 compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != exportCode || resultCode != RESULT_OK) return
        val uri: Uri = data?.data ?: return
        try {
            val output = contentResolver.openOutputStream(uri) ?: throw IllegalStateException("Не удалось открыть файл")
            output.use { out -> RiceLog.logFile(this).inputStream().use { it.copyTo(out) } }
            setStatus("Журнал сохранён. Отправь RICE_LOG.txt в чат.")
            RiceLog.info("EXPORT", "Log export completed")
        } catch (t: Throwable) {
            RiceLog.error("EXPORT", "Log export failed", t)
            setStatus("Ошибка экспорта: ${t.message}")
        }
    }

    private fun renderTasks() {
        if (!::tasksBox.isInitialized) return
        tasksBox.removeAllViews()
        val tasks = readArray("tasks")
        if (tasks.length() == 0) tasksBox.addView(text("Пока задач нет.", 13f, 0xFF68766F.toInt()))
        for (i in 0 until tasks.length()) {
            val item = tasks.optJSONObject(i) ?: continue
            val line = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            line.addView(text((if (item.optBoolean("done")) "✓ " else "• ") + item.optString("text"), 14f, 0xFF263A32.toInt()))
            addButton(line, if (item.optBoolean("done")) "Вернуть в работу" else "Отметить выполненной") {
                item.put("done", !item.optBoolean("done"))
                saveArray("tasks", tasks)
                RiceLog.info("TASK", "Task state changed")
                renderTasks()
            }
            tasksBox.addView(line, match())
        }
    }

    private fun proposeTask() {
        val ideas = listOf(
            "Изучить датчики телефона и описать доступные измерения.",
            "Проверить способы резервного копирования локальной памяти.",
            "Составить перечень возможностей и ограничений устройства.",
            "Разработать план изучения локальных текстовых знаний."
        )
        val tasks = readArray("tasks")
        val used = (0 until tasks.length()).mapNotNull { tasks.optJSONObject(it)?.optString("text") }
        val idea = ideas.firstOrNull { it !in used } ?: "Пересмотреть ранее предложенные исследовательские задачи."
        tasks.put(JSONObject().put("text", idea).put("done", false).put("origin", "rice"))
        saveArray("tasks", tasks)
        RiceLog.info("CURIOSITY", "Suggested local research task: $idea")
        setStatus("Рис предложил новую задачу.")
    }

    private fun readArray(key: String): JSONArray = try {
        JSONArray(prefs.getString(key, "[]"))
    } catch (t: Throwable) {
        RiceLog.error("DATA", "Could not read $key", t)
        JSONArray()
    }
    private fun saveArray(key: String, value: JSONArray) { prefs.edit().putString(key, value.toString()).apply() }
    private fun newColumn() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(18), dp(18), dp(18), dp(18))
        setBackgroundColor(0xFFF3F5F2.toInt())
    }
    private fun text(value: String, size: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
        setPadding(0, dp(5), 0, dp(5))
    }
    private fun section(parent: LinearLayout, title: String) {
        parent.addView(text(title, 12f, 0xFF356859.toInt(), true).apply { setPadding(0, dp(18), 0, dp(8)) })
    }
    private fun addButton(parent: LinearLayout, title: String, action: () -> Unit) {
        parent.addView(Button(this).apply {
            text = title
            isAllCaps = false
            setOnClickListener {
                try { action() }
                catch (t: Throwable) { RiceLog.error("BUTTON", title, t); setStatus("Ошибка: ${t.message}") }
            }
        }, match())
    }
    private fun match() = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(6) }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun setStatus(message: String) {
        if (::status.isInitialized) status.text = message
        RiceLog.info("STATUS", message)
    }
}
