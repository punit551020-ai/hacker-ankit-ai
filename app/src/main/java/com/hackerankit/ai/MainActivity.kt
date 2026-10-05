package com.hackerankit.ai

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.speech.*
import android.speech.tts.TextToSpeech
import android.view.*
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.*

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private lateinit var root: FrameLayout
    private lateinit var tts: TextToSpeech

    private var ttsReady = false
    private var recognizer: SpeechRecognizer? = null
    private var listening = false
    private var torchOn = false

    private val prefs by lazy {
        getSharedPreferences("settings", MODE_PRIVATE)
    }

    private var assistantName: String
        get() = prefs.getString("assistant_name", "Ankit") ?: "Ankit"
        set(value) {
            prefs.edit().putString("assistant_name", value).apply()
        }

    private var offlineMode: Boolean
        get() = prefs.getBoolean("offline_mode", true)
        set(value) {
            prefs.edit().putBoolean("offline_mode", value).apply()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        root = findViewById(R.id.root)

        tts = TextToSpeech(this, this)

        showIntro()
    }

    private fun showIntro() {

        findViewById<Button>(R.id.assistantButton)
            .setOnClickListener {
                showDashboard()
            }

        root.startAnimation(
            android.view.animation.AnimationUtils.loadAnimation(
                this,
                R.anim.fade_scale_in
            )
        )

        Handler(Looper.getMainLooper()).postDelayed({

            findViewById<TextView>(R.id.status).text =
                "SYSTEM ONLINE // READY"

        }, 1800)
    }

    private fun showDashboard() {

        root.removeAllViews()

        val scroll = ScrollView(this)

        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 28, 22, 32)
        }

        fun tv(
            text: String,
            size: Float,
            color: Int = Color.WHITE
        ) =
            TextView(this).apply {
                this.text = text
                textSize = size
                setTextColor(color)
                setPadding(0, 8, 0, 8)
            }

        box.addView(
            tv(
                "HACKER ANKIT AI",
                28f
            )
        )

        box.addView(
            tv(
                "${assistantName.uppercase()} ONLINE  •  LOCAL MODE",
                13f,
                Color.rgb(77, 255, 154)
            )
        )

        val orb = tv(
            "◉\nAI CORE",
            25f,
            Color.rgb(39, 230, 255)
        ).apply {

            gravity = Gravity.CENTER

            background =
                getDrawable(R.drawable.bg_card)
        }

        box.addView(
            orb,
            LinearLayout.LayoutParams(
                -1,
                180
            ).apply {
                setMargins(0, 16, 0, 16)
            }
        )

        val wave = tv(
            "▁▃▆█▆▃▁   LISTENING READY   ▁▃▆█▆▃▁",
            13f,
            Color.rgb(164, 91, 255)
        )

        wave.gravity = Gravity.CENTER

        box.addView(wave)

        val input = EditText(this).apply {

            hint = "Type a command…"

            setTextColor(Color.WHITE)

            setHintTextColor(Color.GRAY)

            setSingleLine(true)

            background =
                getDrawable(R.drawable.bg_card)

            setPadding(18, 12, 18, 12)
        }

        box.addView(
            input,
            LinearLayout.LayoutParams(
                -1,
                58
            ).apply {
                setMargins(0, 18, 0, 8)
            }
        )

        val send = Button(this).apply {

            text = "RUN COMMAND"

            setOnClickListener {

                handleCommand(
                    input.text.toString()
                )
            }
        }

        box.addView(send)

        val mic = Button(this).apply {

            text = "🎙 START VOICE"

            setOnClickListener {

                if (listening) {
                    stopListening()
                } else {
                    startListening()
                }
            }
        }

        box.addView(mic)

        val active = Button(this).apply {

            text =
                if (prefs.getBoolean("active", false))
                    "🟢 ASSISTANT ACTIVE"
                else
                    "🔴 ASSISTANT OFF"

            setOnClickListener {

                val on =
                    !prefs.getBoolean(
                        "active",
                        false
                    )

                prefs.edit()
                    .putBoolean("active", on)
                    .apply()

                text =
                    if (on)
                        "🟢 ASSISTANT ACTIVE"
                    else
                        "🔴 ASSISTANT OFF"

                if (on)
                    startAssistantService()
                else
                    stopAssistantService()
            }
        }

        box.addView(active)

        box.addView(
            tv(
                "SYSTEM CONTROLS",
                19f,
                Color.rgb(39, 230, 255)
            )
        )

        val wifi = Button(this).apply {

            text = "📶 WI-FI SETTINGS"

            setOnClickListener {
                openWifiSettings()
            }
        }

        box.addView(wifi)

        val bluetooth = Button(this).apply {

            text = "🔵 BLUETOOTH SETTINGS"

            setOnClickListener {
                openBluetoothSettings()
            }
        }

        box.addView(bluetooth)

        val torch = Button(this).apply {

            text = "🔦 LIGHT / TORCH"

            setOnClickListener {
                toggleTorch()
            }
        }

        box.addView(torch)

        val offline = Button(this).apply {

            text =
                if (offlineMode)
                    "🟢 OFFLINE MODE ON"
                else
                    "🔴 OFFLINE MODE OFF"

            setOnClickListener {

                offlineMode = !offlineMode

                text =
                    if (offlineMode)
                        "🟢 OFFLINE MODE ON"
                    else
                        "🔴 OFFLINE MODE OFF"

                speak(
                    if (offlineMode)
                        "Offline mode on."
                    else
                        "Offline mode off."
                )
            }
        }

        box.addView(offline)

        box.addView(
            tv(
                "SETTINGS",
                19f,
                Color.rgb(39, 230, 255)
            )
        )

        val nameBtn = Button(this).apply {

            text =
                "Assistant name: $assistantName"

            setOnClickListener {

                editName {
                    text =
                        "Assistant name: $assistantName"
                }
            }
        }

        box.addView(nameBtn)

        val voiceBtn = Button(this).apply {

            text = "SELECT VOICE"

            setOnClickListener {
                selectVoice()
            }
        }

        box.addView(voiceBtn)

        val perms = Button(this).apply {

            text = "PERMISSIONS / STATUS"

            setOnClickListener {
                requestCorePermissions()
            }
        }

        box.addView(perms)

        val about = Button(this).apply {

            text = "ABOUT / PRIVACY"

            setOnClickListener {

                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Hacker Ankit AI")
                    .setMessage(
                        "A local-first Android voice assistant demo. " +
                        "Microphone listening is user-controlled and visible. " +
                        "No secret recording, permission bypass, or credential access is implemented."
                    )
                    .setPositiveButton("OK", null)
                    .show()
            }
        }

        box.addView(about)

        scroll.addView(box)

        root.addView(scroll)
    }

    private fun editName(after: () -> Unit) {

        val edit = EditText(this).apply {
            setText(assistantName)
        }

        AlertDialog.Builder(this)
            .setTitle("Assistant name")
            .setView(edit)
            .setPositiveButton("SAVE") { _, _ ->

                val value =
                    edit.text.toString().trim()

                if (value.isNotEmpty()) {
                    assistantName = value
                }

                after()
            }
            .setNegativeButton(
                "CANCEL",
                null
            )
            .show()
    }

    private fun selectVoice() {

        val voices =
            tts.voices
                ?.filter {
                    it.locale.language in
                            listOf("en", "hi")
                }
                ?.sortedBy {
                    it.name
                }
                ?: emptyList()

        if (voices.isEmpty()) {

            speak(
                "No compatible device voices were found."
            )

            return
        }

        val labels =
            voices.map {
                "${it.name} — ${it.locale.displayName}"
            }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("SELECT VOICE")
            .setItems(labels) { _, which ->

                tts.voice =
                    voices[which]

                prefs.edit()
                    .putString(
                        "voice",
                        voices[which].name
                    )
                    .apply()

                speak("Voice selected.")
            }
            .show()
    }

    private fun requestCorePermissions() {

        val needed =
            mutableListOf(
                Manifest.permission.RECORD_AUDIO
            )

        if (Build.VERSION.SDK_INT >= 33) {

            needed.add(
                Manifest.permission.POST_NOTIFICATIONS
            )
        }

        if (
            Build.VERSION.SDK_INT >= 23 &&
            needed.any {
                ContextCompat.checkSelfPermission(
                    this,
                    it
                ) != PackageManager.PERMISSION_GRANTED
            }
        ) {

            ActivityCompat.requestPermissions(
                this,
                needed.toTypedArray(),
                100
            )

        } else {

            Toast.makeText(
                this,
                "Core permissions already granted.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun startListening() {

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            requestCorePermissions()
            return
        }

        if (
            !SpeechRecognizer.isRecognitionAvailable(
                this
            )
        ) {

            speak(
                "Speech recognition is not available on this device."
            )

            return
        }

        stopListening()

        recognizer =
            SpeechRecognizer.createSpeechRecognizer(
                this
            )

        recognizer?.setRecognitionListener(
            object : RecognitionListener {

                override fun onReadyForSpeech(
                    params: Bundle?
                ) {
                    listening = true
                }

                override fun onResults(
                    results: Bundle?
                ) {

                    val text =
                        results
                            ?.getStringArrayList(
                                SpeechRecognizer.RESULTS_RECOGNITION
                            )
                            ?.firstOrNull()
                            .orEmpty()

                    listening = false

                    recognizer?.destroy()
                    recognizer = null

                    if (text.isBlank()) {
                        speak("I did not hear a command.")
                    } else {
                        handleCommand(text)
                    }
                }

                override fun onError(
                    error: Int
                ) {

                    listening = false

                    recognizer?.destroy()
                    recognizer = null

                    when (error) {

                        SpeechRecognizer.ERROR_NO_MATCH -> {
                            speak("I could not understand that.")
                        }

                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                            speak("I did not hear anything.")
                        }

                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                            speak("Microphone permission is required.")
                        }

                        else -> {
                            speak("Voice recognition failed.")
                        }
                    }
                }

                override fun onBeginningOfSpeech() {}

                override fun onRmsChanged(
                    rmsdB: Float
                ) {}

                override fun onBufferReceived(
                    buffer: ByteArray?
                ) {}

                override fun onEndOfSpeech() {}

                override fun onPartialResults(
                    partialResults: Bundle?
                ) {}

                override fun onEvent(
                    eventType: Int,
                    params: Bundle?
                ) {}
            }
        )

        val intent =
            Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH
            ).apply {

                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )

                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE,
                    Locale("en", "IN")
                )

                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,
                    "en-IN"
                )

                putExtra(
                    RecognizerIntent.EXTRA_PARTIAL_RESULTS,
                    false
                )

                putExtra(
                    RecognizerIntent.EXTRA_PROMPT,
                    "Hacker Ankit is listening"
                )
            }

        try {

            recognizer?.startListening(intent)

        } catch (_: Exception) {

            listening = false

            recognizer?.destroy()
            recognizer = null

            speak(
                "I could not start voice recognition."
            )
        }
    }

    private fun stopListening() {

        try {
            recognizer?.stopListening()
        } catch (_: Exception) {
        }

        try {
            recognizer?.cancel()
        } catch (_: Exception) {
        }

        try {
            recognizer?.destroy()
        } catch (_: Exception) {
        }

        recognizer = null
        listening = false
    }

    private fun handleCommand(raw: String) {

        var c =
            raw.lowercase(
                Locale.getDefault()
            ).trim()

        if (c.isBlank()) return

        c = normalizeCommand(c)

        /*
         * If the user says:
         *
         * "Ankit YouTube open"
         * "Ankit Instagram open"
         * "Ankit WhatsApp open"
         *
         * remove the assistant name first.
         */

        val name =
            assistantName
                .lowercase(Locale.getDefault())
                .trim()

        if (name.isNotEmpty()) {

            c = c
                .removePrefix(name)
                .trim()
        }

        // Also handle common speech-recognition variations.
        c = c
            .removePrefix("hey ankit")
            .removePrefix("okay ankit")
            .removePrefix("ok ankit")
            .trim()

        when {

            containsAny(
                c,
                "youtube",
                "you tube",
                "यूट्यूब"
            ) -> {

                openPackage(
                    "com.google.android.youtube",
                    "YouTube"
                )
            }

            containsAny(
                c,
                "instagram",
                "insta",
                "इंस्टाग्राम",
                "इंस्टा"
            ) -> {

                openPackage(
                    "com.instagram.android",
                    "Instagram"
                )
            }

            containsAny(
                c,
                "whatsapp",
                "व्हाट्सएप",
                "व्हाट्सऐप"
            ) -> {

                openPackage(
                    "com.whatsapp",
                    "WhatsApp"
                )
            }

            containsAny(
                c,
                "chrome",
                "क्रोम"
            ) -> {

                openPackage(
                    "com.android.chrome",
                    "Chrome"
                )
            }

            containsAny(
                c,
                "github",
                "git hub",
                "गिटहब"
            ) -> {

                openUrl(
                    "https://github.com",
                    "GitHub"
                )
            }

            containsAny(
                c,
                "wifi",
                "wi-fi",
                "wi fi",
                "वाईफाई",
                "वाई-फाई"
            ) -> {

                openWifiSettings()
            }

            containsAny(
                c,
                "bluetooth",
                "blue tooth",
                "ब्लूटूथ"
            ) -> {

                openBluetoothSettings()
            }

            containsAny(
                c,
                "torch",
                "flashlight",
                "flash light",
                "light on",
                "light off",
                "light",
                "टॉर्च",
                "लाइट",
                "फ्लैशलाइट"
            ) -> {

                toggleTorch()
            }

            containsAny(
                c,
                "settings",
                "setting",
                "सेटिंग",
                "सेटिंग्स"
            ) -> {

                openSystemSettings()
            }

            containsAny(
                c,
                "gallery",
                "photos",
                "photo",
                "गैलरी",
                "फोटो"
            ) -> {

                openGallery()
            }

            containsAny(
                c,
                "camera",
                "कैमरा"
            ) -> {

                openCamera()
            }

            containsAny(
                c,
                "offline mode on",
                "offline on",
                "ऑफलाइन मोड ऑन",
                "ऑफलाइन ऑन"
            ) -> {

                offlineMode = true

                speak(
                    "Offline mode on."
                )
            }

            containsAny(
                c,
                "offline mode off",
                "offline off",
                "ऑफलाइन मोड ऑफ",
                "ऑफलाइन ऑफ"
            ) -> {

                offlineMode = false

                speak(
                    "Offline mode off."
                )
            }

            containsAny(
                c,
                "assistant on",
                "assistant चालू",
                "असिस्टेंट ऑन",
                "असिस्टेंट चालू"
            ) -> {

                setAssistantActive(true)
            }

            containsAny(
                c,
                "assistant off",
                "assistant बंद",
                "असिस्टेंट ऑफ",
                "असिस्टेंट बंद"
            ) -> {

                setAssistantActive(false)
            }

            c.startsWith("call ") ||
                    c.startsWith("कॉल ") -> {

                speak(
                    "Opening the phone dialer."
                )

                try {

                    startActivity(
                        Intent(
                            Intent.ACTION_DIAL
                        )
                    )

                } catch (_: Exception) {

                    speak(
                        "Phone dialer could not be opened."
                    )
                }
            }

            c.startsWith("open ") ||
                    c.startsWith("खोल ") ||
                    c.startsWith("ओपन ") -> {

                speak(
                    "I can open YouTube, Instagram, WhatsApp, Chrome, GitHub, Settings, Gallery and Camera."
                )
            }

            c == name ||
                    c == "hey ankit" ||
                    c == "okay ankit" ||
                    c == "ok ankit" -> {

                speak(
                    "Yes, I am listening."
                )
            }

            else -> {

                speak(
                    "Command received: $raw"
                )
            }
        }
    }

    private fun normalizeCommand(
        command: String
    ): String {

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

        return words.any {
            command.contains(it)
        }
    }

    private fun openPackage(
        packageName: String,
        label: String
    ) {

        val intent =
            packageManager.getLaunchIntentForPackage(
                packageName
            )

        if (intent != null) {

            intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
            )

            try {

                startActivity(intent)

                speak(
                    "Opening $label."
                )

            } catch (_: Exception) {

                speak(
                    "I could not open $label."
                )
            }

        } else {

            speak(
                "$label is not installed on this device."
            )
        }
    }

    private fun openUrl(
        url: String,
        label: String
    ) {

        try {

            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(url)
                )
            )

            speak(
                "Opening $label."
            )

        } catch (_: Exception) {

            speak(
                "I could not open $label."
            )
        }
    }

    private fun openWifiSettings() {

        try {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

                startActivity(
                    Intent(
                        Settings.Panel.ACTION_WIFI
                    )
                )

            } else {

                startActivity(
                    Intent(
                        Settings.ACTION_WIFI_SETTINGS
                    )
                )
            }

            speak(
                "Opening Wi-Fi controls."
            )

        } catch (_: Exception) {

            try {

                startActivity(
                    Intent(
                        Settings.ACTION_WIFI_SETTINGS
                    )
                )

            } catch (_: Exception) {

                speak(
                    "Wi-Fi settings could not be opened."
                )
            }
        }
    }

    private fun openBluetoothSettings() {

        try {

            startActivity(
                Intent(
                    Settings.ACTION_BLUETOOTH_SETTINGS
                )
            )

            speak(
                "Opening Bluetooth controls."
            )

        } catch (_: Exception) {

            speak(
                "Bluetooth settings could not be opened."
            )
        }
    }

    private fun openSystemSettings() {

        try {

            startActivity(
                Intent(
                    Settings.ACTION_SETTINGS
                )
            )

            speak(
                "Opening Settings."
            )

        } catch (_: Exception) {

            speak(
                "Settings could not be opened."
            )
        }
    }

    private fun openGallery() {

        try {

            val intent =
                Intent(
                    Intent.ACTION_VIEW
                ).apply {

                    type = "image/*"

                    flags =
                        Intent.FLAG_ACTIVITY_NEW_TASK
                }

            startActivity(intent)

            speak(
                "Opening Gallery."
            )

        } catch (_: Exception) {

            speak(
                "Gallery could not be opened."
            )
        }
    }

    private fun openCamera() {

        try {

            startActivity(
                Intent(
                    android.provider.MediaStore
                        .INTENT_ACTION_STILL_IMAGE_CAMERA
                )
            )

            speak(
                "Opening Camera."
            )

        } catch (_: Exception) {

            speak(
                "Camera could not be opened."
            )
        }
    }

    private fun toggleTorch() {

        val manager =
            getSystemService(
                CAMERA_SERVICE
            ) as CameraManager

        val cameraId =
            try {

                manager.cameraIdList.firstOrNull {

                    manager.getCameraCharacteristics(it)
                        .get(
                            CameraCharacteristics
                                .FLASH_INFO_AVAILABLE
                        ) == true
                }

            } catch (_: Exception) {

                null
            }

        if (cameraId == null) {

            speak(
                "Torch is not available on this device."
            )

            return
        }

        torchOn = !torchOn

        try {

            manager.setTorchMode(
                cameraId,
                torchOn
            )

            speak(
                if (torchOn)
                    "Light on."
                else
                    "Light off."
            )

        } catch (_: Exception) {

            speak(
                "Torch could not be changed."
            )
        }
    }

    private fun setAssistantActive(
        active: Boolean
    ) {

        prefs.edit()
            .putBoolean(
                "active",
                active
            )
            .apply()

        if (active) {

            startAssistantService()

            speak(
                "Assistant active."
            )

        } else {

            stopAssistantService()

            speak(
                "Assistant off."
            )
        }
    }

    private fun speak(
        text: String
    ) {

        if (!::tts.isInitialized) return

        if (!ttsReady) {

            Handler(Looper.getMainLooper())
                .postDelayed({

                    if (ttsReady) {

                        try {

                            tts.speak(
                                text,
                                TextToSpeech.QUEUE_FLUSH,
                                null,
                                "hacker-ankit"
                            )

                        } catch (_: Exception) {
                        }
                    }

                }, 500)

            return
        }

        try {

            tts.speak(
                text,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "hacker-ankit"
            )

        } catch (_: Exception) {
        }
    }

    private fun startAssistantService() {

        val intent =
            Intent(
                this,
                AssistantForegroundService::class.java
            )

        try {

            ContextCompat.startForegroundService(
                this,
                intent
            )

        } catch (_: Exception) {

            Toast.makeText(
                this,
                "Assistant service could not start.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun stopAssistantService() {

        stopService(
            Intent(
                this,
                AssistantForegroundService::class.java
            )
        )
    }

    override fun onInit(
        status: Int
    ) {

        if (status == TextToSpeech.SUCCESS) {

            ttsReady = true

            var result =
                tts.setLanguage(
                    Locale("en", "IN")
                )

            if (
                result == TextToSpeech.LANG_MISSING_DATA ||
                result == TextToSpeech.LANG_NOT_SUPPORTED
            ) {

                result =
                    tts.setLanguage(
                        Locale.US
                    )
            }

            prefs.getString(
                "voice",
                null
            )?.let { name ->

                tts.voices
                    ?.firstOrNull {
                        it.name == name
                    }
                    ?.let {
                        tts.voice = it
                    }
            }
        } else {

            ttsReady = false
        }
    }

    override fun onDestroy() {

        stopListening()

        if (::tts.isInitialized) {
            tts.shutdown()
        }

        super.onDestroy()
    }
}
