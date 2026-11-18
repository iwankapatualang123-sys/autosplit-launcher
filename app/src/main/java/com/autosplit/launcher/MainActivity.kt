package com.autosplit.launcher

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Point
import android.graphics.Typeface
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextClock
import android.widget.TextView
import android.widget.Toast
import java.util.Locale
import kotlin.math.roundToInt

class MainActivity : Activity() {

    private lateinit var prefs: Prefs
    private lateinit var hub: SensorHub
    private lateinit var engine: PanelEngine

    private val handler = Handler(Looper.getMainLooper())

    private lateinit var root: LinearLayout
    private lateinit var holder: FrameLayout
    private lateinit var gpsText: TextView
    private lateinit var ramText: TextView
    private lateinit var batText: TextView
    private lateinit var btnA: TextView
    private lateinit var btnB: TextView
    private lateinit var btnStyle: TextView
    private lateinit var btnStart: TextView
    private lateinit var overlay: SlotOverlay

    private var gauge: GaugeView? = null
    private val tileValues = HashMap<String, TextView>()
    private val tileBoxes = HashMap<String, View>()

    private var panelsActive: Boolean
        get() = prefs.panelsActive
        set(v) {
            prefs.panelsActive = v
        }
    private var tick = 0
    private var lastBrightness = -2f

    private val poll = object : Runnable {
        override fun run() {
            refresh()
            handler.postDelayed(this, 250L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        hub = SensorHub(this)
        engine = PanelEngine(this, prefs)
        if (prefs.keepScreenOn) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        buildUi()
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQ_LOCATION)
        }
    }

    override fun onStart() {
        super.onStart()
        hub.start()
        updateSlotButtons()
        applyInsets()
        handler.post(poll)
    }

    override fun onStop() {
        handler.removeCallbacks(poll)
        hub.stop()
        super.onStop()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_LOCATION) {
            hub.stop()
            hub.start()
        }
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        // Launcher: tombol kembali tidak menutup layar utama.
    }

    // ---------- UI ----------

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density + 0.5f).toInt()

    private fun glass(accent: Boolean = false) = GlassDrawable(resources.displayMetrics.density, 12f, accent)

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

    private fun f(fmt: String, vararg a: Any): String = String.format(Locale.US, fmt, *a)

    private fun buildUi() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = HudBackground(resources.displayMetrics.density)
        }

        val status = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = glass()
            setPadding(dp(14), dp(8), dp(14), dp(8))
        }
        val clock = TextClock(this).apply {
            format24Hour = "HH:mm"
            format12Hour = "HH:mm"
            textSize = 20f
            setTextColor(Pal.CYAN)
            typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
        }
        gpsText = statusText(Gravity.CENTER)
        ramText = statusText(Gravity.CENTER)
        batText = statusText(Gravity.END)
        status.addView(clock, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        status.addView(gpsText, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.6f))
        status.addView(ramText, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.8f))
        status.addView(batText, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        holder = FrameLayout(this).apply {
            background = glass()
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }

        val grid = GridLayout(this).apply { columnCount = 4 }
        addTile(grid, T_HEAD, "ARAH")
        addTile(grid, T_ALT, "KETINGGIAN")
        addTile(grid, T_G, "GAYA G")
        addTile(grid, T_BAT, "BATERAI")
        addTile(grid, T_TRIP, "JARAK TRIP")
        addTile(grid, T_MAX, "KEC. MAKS")
        addTile(grid, T_AVG, "RATA-RATA")
        addTile(grid, T_TIME, "WAKTU JALAN")
        addTile(grid, T_PRESS, "TEKANAN")
        addTile(grid, T_LUX, "CAHAYA")
        tileBoxes[T_TRIP]?.setOnLongClickListener {
            hub.resetTrip()
            toast("Trip direset")
            true
        }

        val dockRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        btnA = chip("A") { launchSlot(true) }.also {
            it.setOnLongClickListener { pickApp(true); true }
        }
        btnB = chip("B") { launchSlot(false) }.also {
            it.setOnLongClickListener { pickApp(false); true }
        }
        btnStyle = chip("GAYA") { cycleStyle() }
        btnStart = chip("MULAI", true) { startPanels() }
        addDock(dockRow, btnStart)
        addDock(dockRow, btnA)
        addDock(dockRow, btnB)
        addDock(dockRow, btnStyle)
        addDock(dockRow, chip("SEMUA APP") { drawer() })
        addDock(dockRow, chip("SENSOR") { showSensors() })
        addDock(dockRow, chip("ATUR") { showSettings() })
        addDock(dockRow, chip("LAYAR PENUH") { panelsActive = false; applyInsets() })
        val dock = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(dockRow)
        }

        val full = ViewGroup.LayoutParams.MATCH_PARENT
        val wrap = ViewGroup.LayoutParams.WRAP_CONTENT
        root.addView(status, LinearLayout.LayoutParams(full, wrap).apply { setMargins(dp(3), dp(3), dp(3), dp(3)) })
        root.addView(holder, LinearLayout.LayoutParams(full, 0, 1f).apply { setMargins(dp(3), dp(3), dp(3), dp(3)) })
        root.addView(grid, LinearLayout.LayoutParams(full, wrap))
        root.addView(dock, LinearLayout.LayoutParams(full, wrap))
        root.setPadding(dp(8), dp(6), dp(8), dp(6))
        overlay = SlotOverlay(this)
        val frame = FrameLayout(this)
        frame.addView(root, FrameLayout.LayoutParams(full, full))
        frame.addView(overlay, FrameLayout.LayoutParams(full, full))
        setContentView(frame)

        setGauge()
    }

    private fun statusText(g: Int) = TextView(this).apply {
        textSize = 13f
        setTextColor(Pal.MUTED)
        gravity = g
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
    }

    private fun addTile(grid: GridLayout, key: String, label: String) {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = glass()
            setPadding(dp(8), dp(6), dp(8), dp(6))
        }
        val l = TextView(this).apply {
            text = label
            setTextColor(Pal.MUTED)
            textSize = 10.5f
            gravity = Gravity.CENTER
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
        val v = TextView(this).apply {
            text = "—"
            setTextColor(Pal.TEXT)
            textSize = 17f
            gravity = Gravity.CENTER
            maxLines = 1
            typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
        }
        box.addView(l)
        box.addView(v)
        val lp = GridLayout.LayoutParams().apply {
            width = 0
            height = ViewGroup.LayoutParams.WRAP_CONTENT
            columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            rowSpec = GridLayout.spec(GridLayout.UNDEFINED)
            setMargins(dp(3), dp(3), dp(3), dp(3))
        }
        grid.addView(box, lp)
        tileValues[key] = v
        tileBoxes[key] = box
    }

    private fun chip(label: String, accent: Boolean = false, onClick: () -> Unit): TextView =
        TextView(this).apply {
            text = label
            setTextColor(if (accent) Pal.CYAN else Pal.TEXT)
            textSize = 14f
            gravity = Gravity.CENTER
            maxLines = 1
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            minHeight = dp(52)
            setPadding(dp(16), 0, dp(16), 0)
            background = glass(accent)
            setOnClickListener { onClick() }
        }

    private fun addDock(row: LinearLayout, v: View) {
        row.addView(v, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            setMargins(dp(3), dp(3), dp(3), dp(3))
        })
    }

    private fun setGauge() {
        holder.removeAllViews()
        val g: GaugeView = when (prefs.gaugeStyle) {
            1 -> HondaGaugeView(this)
            2 -> LexusGaugeView(this)
            else -> BmwGaugeView(this)
        }
        g.maxSpeed = if (prefs.useMph) 160f else 240f
        g.unit = if (prefs.useMph) "mph" else "km/h"
        holder.addView(g, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        gauge = g
        btnStyle.text = "GAYA: ${STYLE_NAMES[prefs.gaugeStyle.coerceIn(0, 2)]}"
    }

    private fun cycleStyle() {
        prefs.gaugeStyle = (prefs.gaugeStyle + 1) % 3
        setGauge()
    }

    private fun updateSlotButtons() {
        btnStart.text = "MULAI ${prefs.panelCount} JENDELA"
        btnA.text = "A: ${engine.labelOf(prefs.panelA)}"
        btnB.text = "B: ${engine.labelOf(prefs.panelB)}"
    }

    @Suppress("DEPRECATION")
    private fun applyInsets() {
        val real = Point()
        windowManager.defaultDisplay.getRealSize(real)
        val reserve = panelsActive && !isInMultiWindowMode
        val right = if (reserve) real.x * (100 - prefs.leftPercent) / 100 else 0
        root.setPadding(dp(8), dp(6), dp(8) + right, dp(6))
        updateOverlay()
    }

    private fun updateOverlay() {
        if (!(panelsActive && !isInMultiWindowMode)) {
            overlay.setSlots(emptyList())
            return
        }
        val list = ArrayList<SlotOverlay.Slot>()
        list.add(SlotOverlay.Slot(engine.boundsA(), "A  " + engine.labelOf(prefs.panelA).uppercase()))
        if (prefs.panelCount >= 3) {
            list.add(SlotOverlay.Slot(engine.boundsB(), "B  " + engine.labelOf(prefs.panelB).uppercase()))
        }
        overlay.setSlots(list)
    }

    @Suppress("DEPRECATION")
    private fun immersive() {
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) immersive()
    }

    // ---------- Pembaruan data ----------

    private fun moving(): Boolean = hub.currentSpeedMps() > 1.4f

    private fun cardinal(d: Float): String {
        val n = arrayOf("U", "TL", "T", "TG", "S", "BD", "B", "BL")
        return n[((d + 22.5f) / 45f).toInt() % 8]
    }

    private fun refresh() {
        val mph = prefs.useMph
        val k = if (mph) 2.23694f else 3.6f
        val unitName = if (mph) "mph" else "km/j"
        gauge?.setSpeed(hub.currentSpeedMps() * k)

        tick++
        if (tick % 8 == 1) {
            hub.readBattery()
            applyInsets()
            ramText.text = "RAM ${hub.ramFreeMb()} MB"
            val bt = if (hub.batteryTempC.isNaN()) "" else f(" %.0f°C", hub.batteryTempC)
            batText.text = if (hub.batteryPct >= 0) "${hub.batteryPct}%$bt${if (hub.charging) " CAS" else ""}" else ""
        }
        gpsText.text = hub.gpsStatus()

        val h = if (!hub.headingDeg.isNaN()) hub.headingDeg else hub.gpsBearing
        tileValues[T_HEAD]?.text = if (h.isNaN()) "—" else "${h.roundToInt() % 360}° ${cardinal(h)}"

        val alt = when {
            !hub.altitudeM.isNaN() -> hub.altitudeM
            !hub.pressureHpa.isNaN() ->
                SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, hub.pressureHpa).toDouble()
            else -> Double.NaN
        }
        tileValues[T_ALT]?.text = when {
            alt.isNaN() -> "—"
            mph -> f("%.0f ft", alt * 3.28084)
            else -> f("%.0f m", alt)
        }

        tileValues[T_G]?.text = if (hub.hasLinear || hub.hasAccel) f("%.2f G", hub.gForce) else "—"
        tileValues[T_BAT]?.text = if (hub.batteryPct >= 0) "${hub.batteryPct}%" else "—"

        val dist = hub.tripMeters / 1000.0 * (if (mph) 0.621371 else 1.0)
        tileValues[T_TRIP]?.text = f("%.1f %s", dist, if (mph) "mi" else "km")
        tileValues[T_MAX]?.text = f("%.0f %s", hub.tripMaxMps * k, unitName)
        tileValues[T_AVG]?.text =
            if (hub.tripMovingSec > 5.0) f("%.0f %s", (hub.tripMeters / hub.tripMovingSec).toFloat() * k, unitName) else "—"
        val t = hub.tripMovingSec.toLong()
        tileValues[T_TIME]?.text = f("%d:%02d:%02d", t / 3600, (t % 3600) / 60, t % 60)

        tileBoxes[T_PRESS]?.visibility = if (hub.hasPressure) View.VISIBLE else View.GONE
        tileValues[T_PRESS]?.text = if (hub.pressureHpa.isNaN()) "—" else f("%.0f hPa", hub.pressureHpa)
        tileBoxes[T_LUX]?.visibility = if (hub.hasLight) View.VISIBLE else View.GONE
        tileValues[T_LUX]?.text = if (hub.lux < 0f) "—" else f("%.0f lx", hub.lux)

        autoBrightness()
    }

    private fun autoBrightness() {
        if (!prefs.autoDim || hub.lux < 0f) {
            setBrightness(-1f)
            return
        }
        val b = when {
            hub.lux < 6f -> 0.18f
            hub.lux < 40f -> 0.4f
            else -> -1f
        }
        setBrightness(b)
    }

    private fun setBrightness(b: Float) {
        if (b == lastBrightness) return
        lastBrightness = b
        val lp = window.attributes
        lp.screenBrightness = b
        window.attributes = lp
    }

    // ---------- Panel ----------

    private fun launchSlot(isA: Boolean) {
        val pkg = if (isA) prefs.panelA else prefs.panelB
        if (!engine.isInstalled(pkg)) {
            toast("Aplikasi belum dipilih atau belum terpasang. Tekan lama tombol ini untuk memilih.")
            return
        }
        val bounds = if (isA) engine.boundsA() else engine.boundsB()
        val ok = engine.launch(pkg, if (engine.freeformUsable()) bounds else null, isInMultiWindowMode)
        if (!ok) toast("Gagal membuka aplikasi")
    }

    private fun startPanels() {
        val a = prefs.panelA
        val b = prefs.panelB
        if (!engine.isInstalled(a) && !engine.isInstalled(b)) {
            toast("Pilih aplikasi panel dulu: tekan lama tombol A atau B.")
            return
        }
        val first = if (engine.isInstalled(a)) a else b
        if (engine.freeformUsable()) {
            val wanted = if (prefs.panelCount >= 3) {
                listOf(b to engine.boundsB(), a to engine.boundsA())
            } else {
                listOf(first to engine.boundsA())
            }
            val targets = wanted.filter { engine.isInstalled(it.first) }
            panelsActive = true
            applyInsets()
            var delay = 0L
            for ((pkg, bounds) in targets) {
                handler.postDelayed({ engine.launch(pkg, bounds, false) }, delay)
                delay += 900L
            }
            toast("Menjalankan speedometer + ${targets.size} aplikasi")
            return
        }
        if (isInMultiWindowMode) {
            engine.launch(first, null, true)
        } else if (SplitService.instance != null) {
            SplitService.toggleSplit()
            handler.postDelayed({ engine.launch(first, null, true) }, 1100L)
        } else {
            AlertDialog.Builder(this, DIALOG)
                .setTitle("Aktifkan layanan Split")
                .setMessage(
                    "Tablet ini tidak mendukung jendela freeform, jadi launcher perlu menekan tombol split-screen sistem. " +
                        "Aktifkan \"AutoSplit Split\" di Pengaturan > Aksesibilitas. Layanan ini tidak membaca isi layar."
                )
                .setPositiveButton("Buka pengaturan") { _, _ -> openSettings(Settings.ACTION_ACCESSIBILITY_SETTINGS) }
                .setNegativeButton("Nanti", null)
                .show()
            return
        }
        if (prefs.panelCount >= 3) {
            toast("Tablet ini belum mendukung freeform, jadi baru 2 jendela. Coba aktifkan \"Paksa mode freeform\" di ATUR.")
        } else {
            toast("Mode 2 jendela: speedometer + 1 aplikasi. Ketuk A atau B untuk mengganti aplikasi di sisi kanan.")
        }
    }

    private fun openSettings(action: String) {
        try {
            startActivity(Intent(action))
        } catch (e: Exception) {
            toast("Pengaturan tidak bisa dibuka")
        }
    }

    // ---------- Dialog ----------

    private fun launchables(): List<Pair<String, String>> {
        val pm = packageManager
        val q = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(q, 0)
            .map { it.activityInfo.packageName to it.loadLabel(pm).toString() }
            .filter { it.first != packageName }
            .distinctBy { it.first }
            .sortedBy { it.second.lowercase() }
    }

    private fun pickApp(forA: Boolean) {
        if (moving()) {
            toast(LOCK_MSG)
            return
        }
        val list = launchables()
        AlertDialog.Builder(this, DIALOG)
            .setTitle(if (forA) "Pilih aplikasi panel A" else "Pilih aplikasi panel B")
            .setItems(list.map { it.second }.toTypedArray()) { _, i ->
                if (forA) prefs.panelA = list[i].first else prefs.panelB = list[i].first
                updateSlotButtons()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun drawer() {
        if (moving()) {
            toast(LOCK_MSG)
            return
        }
        val list = launchables()
        AlertDialog.Builder(this, DIALOG)
            .setTitle("Semua aplikasi")
            .setItems(list.map { it.second }.toTypedArray()) { _, i ->
                engine.launch(list[i].first, null, isInMultiWindowMode)
            }
            .setNegativeButton("Tutup", null)
            .show()
    }

    private fun onOff(b: Boolean) = if (b) "aktif" else "mati"

    private fun showSettings() {
        if (moving()) {
            toast(LOCK_MSG)
            return
        }
        val items = arrayOf(
            "Satuan: ${if (prefs.useMph) "mph" else "km/jam"}",
            "Lebar panel speedometer: ${prefs.leftPercent}%",
            "Jumlah jendela: ${prefs.panelCount} (speedometer + ${prefs.panelCount - 1} aplikasi)",
            "Susunan: ${if (prefs.entertainmentLayout) "Hiburan (video besar)" else "Navigasi (peta besar)"}",
            "Kecerahan otomatis (sensor cahaya): ${onOff(prefs.autoDim)}",
            "Layar tetap menyala: ${onOff(prefs.keepScreenOn)}",
            "Paksa mode freeform (percobaan): ${onOff(prefs.forceFreeform)}",
            "Aktifkan layanan Split (Aksesibilitas)",
            "Jadikan launcher utama"
        )
        AlertDialog.Builder(this, DIALOG)
            .setTitle("Pengaturan")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> {
                        prefs.useMph = !prefs.useMph
                        setGauge()
                        showSettings()
                    }
                    1 -> askRatio()
                    2 -> {
                        prefs.panelCount = if (prefs.panelCount >= 3) 2 else 3
                        updateSlotButtons()
                        showSettings()
                    }
                    3 -> {
                        prefs.entertainmentLayout = !prefs.entertainmentLayout
                        showSettings()
                    }
                    4 -> {
                        prefs.autoDim = !prefs.autoDim
                        showSettings()
                    }
                    5 -> {
                        prefs.keepScreenOn = !prefs.keepScreenOn
                        if (prefs.keepScreenOn) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        showSettings()
                    }
                    6 -> {
                        prefs.forceFreeform = !prefs.forceFreeform
                        showSettings()
                    }
                    7 -> openSettings(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    8 -> openSettings(Settings.ACTION_HOME_SETTINGS)
                }
            }
            .setNegativeButton("Tutup", null)
            .show()
    }

    private fun askRatio() {
        val label = TextView(this).apply {
            text = "${prefs.leftPercent}%"
            setTextColor(Pal.TEXT)
            gravity = Gravity.CENTER
            textSize = 18f
        }
        val sb = SeekBar(this).apply {
            max = 35
            progress = prefs.leftPercent - 25
        }
        sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                label.text = "${p + 25}%"
            }

            override fun onStartTrackingTouch(s: SeekBar?) {}

            override fun onStopTrackingTouch(s: SeekBar?) {}
        })
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(16), dp(24), 0)
            addView(label)
            addView(sb)
        }
        AlertDialog.Builder(this, DIALOG)
            .setTitle("Lebar panel speedometer (25 sampai 60%)")
            .setView(box)
            .setPositiveButton("Simpan") { _, _ ->
                prefs.leftPercent = sb.progress + 25
                applyInsets()
                showSettings()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun systemInfo(): String = buildString {
        append("Perangkat: ${Build.MANUFACTURER} ${Build.MODEL}\n")
        append("Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n")
        append("RAM: bebas ${hub.ramFreeMb()} MB dari ${hub.ramTotalMb()} MB\n")
        append("Multi-window sekarang: ${if (isInMultiWindowMode) "ya" else "tidak"}\n")
        append("Fitur freeform sistem: ${if (engine.systemHasFreeform()) "ada" else "tidak ada"}\n")
        append("Paksa freeform: ${onOff(prefs.forceFreeform)}\n")
        append("Layanan Split: ${if (SplitService.instance != null) "aktif" else "mati"}\n")
        append("GPS: ${hub.gpsStatus()}\n")
        val compass = when {
            hub.hasRotation -> "rotation vector"
            hub.hasCompassFallback -> "akselerometer + magnetometer"
            else -> "tidak ada"
        }
        append("Kompas: $compass\n")
        append("Gaya G: ${if (hub.hasLinear) "linear acceleration" else if (hub.hasAccel) "akselerometer" else "tidak ada"}\n")
        append("Sensor cahaya: ${if (hub.hasLight) "ada" else "tidak ada"}\n")
        append("Barometer: ${if (hub.hasPressure) "ada" else "tidak ada"}\n")
        append("Suhu ruang: ${if (hub.hasAmbientTemp) "ada" else "tidak ada"}\n")
        append("\n--- Semua sensor (nilai langsung) ---\n")
    }

    private fun showSensors() {
        if (moving()) {
            toast(LOCK_MSG)
            return
        }
        val live = SensorLive(this)
        live.start()
        val tv = TextView(this).apply {
            typeface = Typeface.MONOSPACE
            textSize = 12f
            setTextColor(Pal.TEXT)
            setPadding(dp(20), dp(8), dp(20), dp(8))
        }
        val sv = ScrollView(this).apply { addView(tv) }
        val loop = object : Runnable {
            override fun run() {
                tv.text = systemInfo() + live.render()
                handler.postDelayed(this, 500L)
            }
        }
        val dlg = AlertDialog.Builder(this, DIALOG)
            .setTitle("Sensor dan diagnostik")
            .setView(sv)
            .setPositiveButton("Tutup", null)
            .create()
        dlg.setOnDismissListener {
            handler.removeCallbacks(loop)
            live.stop()
        }
        dlg.show()
        handler.post(loop)
    }

    companion object {
        private const val REQ_LOCATION = 100
        private const val DIALOG = android.R.style.Theme_Material_Dialog_Alert
        private const val LOCK_MSG = "Dikunci saat mobil bergerak"
        private val STYLE_NAMES = arrayOf("BMW", "HONDA", "TOYOTA/LEXUS")

        private const val T_HEAD = "head"
        private const val T_ALT = "alt"
        private const val T_G = "g"
        private const val T_BAT = "bat"
        private const val T_TRIP = "trip"
        private const val T_MAX = "max"
        private const val T_AVG = "avg"
        private const val T_TIME = "time"
        private const val T_PRESS = "press"
        private const val T_LUX = "lux"
    }
}
