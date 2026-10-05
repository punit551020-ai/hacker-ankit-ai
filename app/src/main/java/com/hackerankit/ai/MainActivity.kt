package com.hackerankit.ai

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.*
import android.speech.*
import android.speech.tts.TextToSpeech
import android.view.*
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.*

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private var ttsReady = false
    private lateinit var root: FrameLayout
    private lateinit var tts: TextToSpeech
    private var recognizer: SpeechRecognizer? = null
    private var listening = false
    private var torchOn = false
    private val prefs by lazy { getSharedPreferences("settings", MODE_PRIVATE) }
    private var assistantName: String
        get() = prefs.getString("assistant_name", "Ankit") ?: "Ankit"
        set(v) { prefs.edit().putString("assistant_name", v).apply() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        root = findViewById(R.id.root)
        tts = TextToSpeech(this, this)
        showIntro()
    }

    private fun showIntro() {
        findViewById<Button>(R.id.assistantButton).setOnClickListener { showDashboard() }
        root.startAnimation(android.view.animation.AnimationUtils.loadAnimation(this, R.anim.fade_scale_in))
        Handler(Looper.getMainLooper()).postDelayed({
            findViewById<TextView>(R.id.status).text = "SYSTEM ONLINE // READY"
        }, 1800)
    }

    private fun showDashboard() {
        root.removeAllViews()
        val scroll = ScrollView(this)
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 28, 22, 32)
        }
        fun tv(text: String, size: Float, color: Int = Color.WHITE) = TextView(this).apply {
            this.text = text; textSize = size; setTextColor(color); setPadding(0, 8, 0, 8)
        }
        box.addView(tv("HACKER ANKIT AI", 28f))
        box.addView(tv("${assistantName.uppercase()} ONLINE  •  LOCAL MODE", 13f, Color.rgb(77,255,154)))

        val orb = tv("◉\nAI CORE", 25f, Color.rgb(39,230,255)).apply {
            gravity = Gravity.CENTER; background = getDrawable(R.drawable.bg_card)
        }
        box.addView(orb, LinearLayout.LayoutParams(-1, 180).apply { setMargins(0,16,0,16) })

        val wave = tv("▁▃▆█▆▃▁   LISTENING READY   ▁▃▆█▆▃▁", 13f, Color.rgb(164,91,255))
        wave.gravity = Gravity.CENTER
        box.addView(wave)

        val input = EditText(this).apply {
            hint = "Type a command…"
            setTextColor(Color.WHITE); setHintTextColor(Color.GRAY)
            setSingleLine(true); background = getDrawable(R.drawable.bg_card)
            setPadding(18, 12, 18, 12)
        }
        box.addView(input, LinearLayout.LayoutParams(-1, 58).apply { setMargins(0,18,0,8) })

        val send = Button(this).apply { text = "RUN COMMAND"; setOnClickListener { handleCommand(input.text.toString()) } }
        box.addView(send)

        val mic = Button(this).apply {
            text = "🎙 START VOICE"
            setOnClickListener { if (listening) stopListening() else startListening() }
        }
        box.addView(mic)

        val active = Button(this).apply {
            text = if (prefs.getBoolean("active", false)) "🟢 ASSISTANT ACTIVE" else "🔴 ASSISTANT OFF"
            setOnClickListener {
                val on = !prefs.getBoolean("active", false)
                prefs.edit().putBoolean("active", on).apply()
                text = if (on) "🟢 ASSISTANT ACTIVE" else "🔴 ASSISTANT OFF"
                if (on) startAssistantService() else stopAssistantService()
            }
        }
        box.addView(active)

        box.addView(tv("SETTINGS", 19f, Color.rgb(39,230,255)))
        val nameBtn = Button(this).apply {
            text = "Assistant name: $assistantName"
            setOnClickListener { editName { text = "Assistant name: $assistantName" } }
        }
        box.addView(nameBtn)

        val voiceBtn = Button(this).apply {
            text = "SELECT VOICE"
            setOnClickListener { selectVoice() }
        }
        box.addView(voiceBtn)

        val perms = Button(this).apply {
            text = "PERMISSIONS / STATUS"
            setOnClickListener { requestCorePermissions() }
        }
        box.addView(perms)

        val about = Button(this).apply {
            text = "ABOUT / PRIVACY"
            setOnClickListener {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Hacker Ankit AI")
                    .setMessage("A local-first Android voice assistant demo. Microphone listening is user-controlled and visible. No secret recording, permission bypass, or credential access is implemented.")
                    .setPositiveButton("OK", null).show()
            }
        }
        box.addView(about)

        scroll.addView(box); root.addView(scroll)
    }

    private fun editName(after: () -> Unit) {
        val edit = EditText(this).apply { setText(assistantName) }
        AlertDialog.Builder(this).setTitle("Assistant name").setView(edit)
            .setPositiveButton("SAVE") { _, _ ->
                val v = edit.text.toString().trim()
                if (v.isNotEmpty()) assistantName = v
                after()
            }.setNegativeButton("CANCEL", null).show()
    }

    private fun selectVoice() {
        val voices = tts.voices?.filter { it.locale.language in listOf("en","hi") }?.sortedBy { it.name } ?: emptyList()
        if (voices.isEmpty()) {
            speak("No compatible device voices were found.")
            return
        }
        val labels = voices.map { "${it.name} — ${it.locale.displayName}" }.toTypedArray()
        AlertDialog.Builder(this).setTitle("SELECT VOICE").setItems(labels) { _, which ->
            tts.voice = voices[which]
            prefs.edit().putString("voice", voices[which].name).apply()
            speak("Voice selected.")
        }.show()
    }

    private fun requestCorePermissions() {
        val needed = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= 33) needed.add(Manifest.permission.POST_NOTIFICATIONS)
        if (Build.VERSION.SDK_INT >= 23 && needed.any { ContextCompat.checkSelfPermission(this,it) != PackageManager.PERMISSION_GRANTED })
            ActivityCompat.requestPermissions(this, needed.toTypedArray(), 100)
        else Toast.makeText(this, "Core permissions already granted.", Toast.LENGTH_SHORT).show()
    }

    private fun startListening() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestCorePermissions(); return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            speak("Speech recognition is not available on this device."); return
        }
        recognizer = SpeechRecognizer.createSpeechRecognizer(this)
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(p: Bundle?) { listening = true }
            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                listening = false; handleCommand(text); recognizer?.destroy()
            }
            override fun onError(error: Int) { listening = false; recognizer?.destroy(); speak("I could not understand that command.") }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(r: Float) {}
            override fun onBufferReceived(b: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(p: Bundle?) {}
            override fun onEvent(t: Int, p: Bundle?) {}
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Hacker Ankit is listening")
        }
        recognizer?.startListening(intent)
    }

    private fun stopListening() { recognizer?.stopListening(); recognizer?.destroy(); recognizer = null; listening = false }

    private fun handleCommand(raw: String) {
        var c = raw.lowercase(Locale.getDefault()).trim()
        if (c.isBlank()) return

        c = normalizeCommand(c)

        when {
            containsAny(c, "youtube", "you tube", "यूट्यूब") ->
                openPackage("com.google.android.youtube", "YouTube")

            containsAny(c, "instagram", "insta", "इंस्टाग्राम", "इंस्टा") ->
                openPackage("com.instagram.android", "Instagram")

            containsAny(c, "whatsapp", "व्हाट्सएप", "व्हाट्सऐप") ->
                openPackage("com.whatsapp", "WhatsApp")

            containsAny(c, "chrome", "क्रोम") ->
                openPackage("com.android.chrome", "Chrome")

            containsAny(c, "github", "git hub", "गिटहब") ->
                openUrl("https://github.com", "GitHub")

            containsAny(c, "wifi", "wi-fi", "वाईफाई", "वाई-फाई") -> {
                startActivity(Intent(android.provider.Settings.Panel.ACTION_WIFI))
                speak("Opening Wi-Fi settings.")
            }

            containsAny(c, "bluetooth", "ब्लूटूथ") -> {
                startActivity(Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS))
                speak("Opening Bluetooth settings.")
            }

            containsAny(c, "camera", "कैमरा") -> {
                startActivity(Intent(android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))
                speak("Opening Camera.")
            }

            containsAny(c, "torch", "flash", "flashlight", "light", "टॉर्च", "फ्लैशलाइट") ->
                toggleTorch()

            containsAny(c, "settings", "setting", "सेटिंग्स", "सेटिंग") -> {
                startActivity(Intent(android.provider.Settings.ACTION_SETTINGS))
                speak("Opening Settings.")
            }

            containsAny(c, "gallery", "photos", "गैलरी") -> {
                startActivity(Intent(Intent.ACTION_VIEW).apply {
                    type = "image/*"
                })
                speak("Opening Gallery.")
            }

            c.startsWith("call ") -> {
                startActivity(Intent(Intent.ACTION_DIAL))
                speak("Opening phone.")
            }

            c.startsWith("open ") ||
                    c.startsWith("खोल ") ||
                    c.startsWith("ओपन ") -> {
                speak("I can open YouTube, Instagram, WhatsApp, Chrome, GitHub, Settings, Gallery and Camera.")
            }

            else -> speak("Command received: $raw")
        }
    }

    private fun normalizeCommand(command: String): String {
        return command
            .replace("इंस्टाग्राम", "instagram")
            .replace("इंस्टा", "instagram")
            .replace("यूट्यूब", "youtube")
            .replace("व्हाट्सएप", "whatsapp")
            .replace("व्हाट्सऐप", "whatsapp")
            .replace("क्रोम", "chrome")
            .replace("गिटहब", "github")
            .replace("वाईफाई", "wifi")
            .replace("वाई-फाई", "wifi")
            .replace("ब्लूटूथ", "bluetooth")
            .replace("टॉर्च", "torch")
            .replace("फ्लैशलाइट", "flashlight")
            .replace("कैमरा", "camera")
            .replace("गैलरी", "gallery")
            .replace("सेटिंग्स", "settings")
            .replace("सेटिंग", "settings")
    }

    private fun containsAny(
        command: String,
        vararg words: String
    ): Boolean {
        return words.any { command.contains(it) }
    }

    private fun openPackage(pkg: String, label: String) {
        val i = packageManager.getLaunchIntentForPackage(pkg)
        if (i != null) { startActivity(i); speak("Opening $label.") }
        else speak("$label is not installed on this device.")
    }

    private fun openUrl(url: String, label: String) {
        startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)))
        speak("Opening $label.")
    }

    private fun toggleTorch() {
        val manager = getSystemService(CAMERA_SERVICE) as CameraManager
        val id = manager.cameraIdList.firstOrNull { manager.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true }
        if (id == null) { speak("Torch is not available on this device."); return }
        torchOn = !torchOn
        try { manager.setTorchMode(id, torchOn); speak(if (torchOn) "Torch on." else "Torch off.") }
        catch (_: Exception) { speak("Torch could not be changed.") }
    }

    private fun speak(text: String) {
        if (::tts.isInitialized && ttsReady) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "hacker-ankit")
    }

    private fun startAssistantService() {
        val i = Intent(this, AssistantForegroundService::class.java)
        ContextCompat.startForegroundService(this, i)
    }
    private fun stopAssistantService() { stopService(Intent(this, AssistantForegroundService::class.java)) }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            ttsReady = true
            tts.language = Locale.getDefault()
            prefs.getString("voice", null)?.let { name -> tts.voices?.firstOrNull { it.name == name }?.let { tts.voice = it } }
        }
    }

    override fun onDestroy() {
        stopListening(); if (::tts.isInitialized) tts.shutdown(); super.onDestroy()
    }
}
