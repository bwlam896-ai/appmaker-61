package com.appmaker.app

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private val gson = Gson()

    private val azkarList = mutableListOf<Dhikr>()
    private var activeDhikr: Dhikr? = null
    private lateinit var adapter: DhikrAdapter

    // Views
    private lateinit var tvTotalCounts: TextView
    private lateinit var tvCurrentCategory: TextView
    private lateinit var tvCurrentTarget: TextView
    private lateinit var tvCurrentDhikrText: TextView
    private lateinit var tvCurrentVirtue: TextView
    private lateinit var tvCounterValue: TextView
    private lateinit var tvCompletionPercent: TextView
    private lateinit var btnTapCounter: FrameLayout
    private lateinit var btnReset: MaterialButton
    private lateinit var btnAddDhikr: MaterialButton
    private lateinit var btnLang: MaterialButton
    private lateinit var rvAzkarList: RecyclerView
    private lateinit var tvSelectedAzkarTitle: TextView
    private lateinit var tvTapToCountLabel: TextView

    override fun attachBaseContext(newBase: Context) {
        val lang = newBase.getSharedPreferences("SelectedAzkarPrefs", Context.MODE_PRIVATE)
            .getString("app_lang", "ar") ?: "ar"
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = newBase.resources.configuration
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        val context = newBase.createConfigurationContext(config)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPreferences = getSharedPreferences("SelectedAzkarPrefs", Context.MODE_PRIVATE)

        initViews()
        loadAzkarData()
        setupRecyclerView()
        setupListeners()
        updateActiveDisplay()
    }

    private fun initViews() {
        tvTotalCounts = findViewById(R.id.tvTotalCounts)
        tvCurrentCategory = findViewById(R.id.tvCurrentCategory)
        tvCurrentTarget = findViewById(R.id.tvCurrentTarget)
        tvCurrentDhikrText = findViewById(R.id.tvCurrentDhikrText)
        tvCurrentVirtue = findViewById(R.id.tvCurrentVirtue)
        tvCounterValue = findViewById(R.id.tvCounterValue)
        tvCompletionPercent = findViewById(R.id.tvCompletionPercent)
        btnTapCounter = findViewById(R.id.btnTapCounter)
        btnReset = findViewById(R.id.btnReset)
        btnAddDhikr = findViewById(R.id.btnAddDhikr)
        btnLang = findViewById(R.id.btnLang)
        rvAzkarList = findViewById(R.id.rvAzkarList)
        tvSelectedAzkarTitle = findViewById(R.id.tvSelectedAzkarTitle)
        tvTapToCountLabel = findViewById(R.id.tvTapToCountLabel)

        val currentLang = sharedPreferences.getString("app_lang", "ar") ?: "ar"
        btnLang.text = "🌐 ${currentLang.uppercase()}"
    }

    private fun setupRecyclerView() {
        rvAzkarList.layoutManager = LinearLayoutManager(this)
        adapter = DhikrAdapter(
            azkarList = azkarList,
            selectedDhikrId = activeDhikr?.id ?: 0L,
            onDhikrSelected = { selected ->
                activeDhikr = selected
                adapter.updateSelectedId(selected.id)
                updateActiveDisplay()
                vibrateDevice(20)
            },
            onQuickAddClicked = { item ->
                item.count += 1
                saveAzkarData()
                updateActiveDisplay()
                adapter.notifyDataSetChanged()
                vibrateDevice(25)
            },
            onDeleteClicked = { item ->
                confirmDelete(item)
            }
        )
        rvAzkarList.adapter = adapter
    }

    private fun setupListeners() {
        btnTapCounter.setOnClickListener {
            activeDhikr?.let { dhikr ->
                dhikr.count += 1
                if (dhikr.count == dhikr.target) {
                    vibrateDevice(120)
                } else {
                    vibrateDevice(35)
                }
                saveAzkarData()
                updateActiveDisplay()
                adapter.notifyDataSetChanged()
            }
        }

        btnReset.setOnClickListener {
            activeDhikr?.let { dhikr ->
                AlertDialog.Builder(this)
                    .setTitle(getString(R.string.reset_confirm_title))
                    .setMessage(getString(R.string.reset_confirm_msg))
                    .setPositiveButton(getString(R.string.yes)) { _, _ ->
                        dhikr.count = 0
                        saveAzkarData()
                        updateActiveDisplay()
                        adapter.notifyDataSetChanged()
                    }
                    .setNegativeButton(getString(R.string.no), null)
                    .show()
            }
        }

        btnAddDhikr.setOnClickListener {
            showAddDhikrDialog()
        }

        btnLang.setOnClickListener {
            showLanguageSelectionDialog()
        }
    }

    private fun showLanguageSelectionDialog() {
        val languages = arrayOf("العربية", "English", "Français")
        val langCodes = arrayOf("ar", "en", "fr")
        
        val currentLang = sharedPreferences.getString("app_lang", "ar") ?: "ar"
        val checkedItem = langCodes.indexOf(currentLang)

        AlertDialog.Builder(this)
            .setTitle(R.string.select_language)
            .setSingleChoiceItems(languages, checkedItem) { dialog, which ->
                val selectedLang = langCodes[which]
                if (selectedLang != currentLang) {
                    sharedPreferences.edit().putString("app_lang", selectedLang).apply()
                    dialog.dismiss()
                    recreate()
                }
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    private fun updateActiveDisplay() {
        val dhikr = activeDhikr ?: return
        tvCurrentCategory.text = dhikr.category
        tvCurrentTarget.text = getString(R.string.target_label, dhikr.target)
        tvCurrentDhikrText.text = dhikr.text
        tvCurrentVirtue.text = if (dhikr.virtue.isNotEmpty()) dhikr.virtue else getString(R.string.category_default)
        tvCounterValue.text = dhikr.count.toString()

        val percent = if (dhikr.target > 0) {
            ((dhikr.count.toFloat() / dhikr.target.toFloat()) * 100).toInt()
        } else 0
        tvCompletionPercent.text = "$percent%"

        val total = azkarList.sumOf { it.count }
        tvTotalCounts.text = getString(R.string.total_counts_label, total)
    }

    private fun showAddDhikrDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_dhikr, null)
        val etText = dialogView.findViewById<EditText>(R.id.etDhikrText)
        val etVirtue = dialogView.findViewById<EditText>(R.id.etDhikrVirtue)
        val etTarget = dialogView.findViewById<EditText>(R.id.etDhikrTarget)
        val spCategory = dialogView.findViewById<Spinner>(R.id.spCategory)

        val currentLang = sharedPreferences.getString("app_lang", "ar") ?: "ar"
        val categories = if (currentLang == "ar") {
            arrayOf("أذكار الصباح والمساء", "أذكار الصلاة", "استغفار وتوبة", "أدعية مأثورة", "ذكر عام")
        } else if (currentLang == "fr") {
            arrayOf("Matin & Soir", "Invocations de Prière", "Repentance & Istighfar", "Invocations Prophétiques", "Dhikr Général")
        } else {
            arrayOf("Morning & Evening", "Prayer Azkar", "Repentance & Istighfar", "Prophetic Supplications", "General Dhikr")
        }

        // Fixed: Using local R.layout.simple_spinner_dropdown_item layout resource to resolve build error
        val spinnerAdapter = ArrayAdapter(this, R.layout.simple_spinner_dropdown_item, categories)
        spCategory.adapter = spinnerAdapter
        spCategory.setSelection(4)

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton(R.string.btn_save) { _, _ ->
                val text = etText.text.toString().trim()
                val virtue = etVirtue.text.toString().trim()
                val targetStr = etTarget.text.toString().trim()
                val target = if (targetStr.isNotEmpty()) targetStr.toIntOrNull() ?: 33 else 33
                val category = spCategory.selectedItem.toString()

                if (text.isEmpty()) {
                    Toast.makeText(this, getString(R.string.error_empty_text), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val newDhikr = Dhikr(
                    id = System.currentTimeMillis(),
                    text = text,
                    virtue = virtue,
                    category = category,
                    count = 0,
                    target = target
                )

                azkarList.add(0, newDhikr)
                activeDhikr = newDhikr
                saveAzkarData()
                adapter.updateSelectedId(newDhikr.id)
                updateActiveDisplay()
                Toast.makeText(this, getString(R.string.success_added), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .create()
            .show()
    }

    private fun confirmDelete(dhikr: Dhikr) {
        if (azkarList.size <= 1) {
            Toast.makeText(this, getString(R.string.min_dhikr_error), Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.delete_confirm_title)
            .setMessage(R.string.delete_confirm_msg)
            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                val index = azkarList.indexOfFirst { it.id == dhikr.id }
                if (index != -1) {
                    azkarList.removeAt(index)
                    if (activeDhikr?.id == dhikr.id) {
                        activeDhikr = azkarList.firstOrNull()
                        adapter.updateSelectedId(activeDhikr?.id ?: 0L)
                    }
                    saveAzkarData()
                    adapter.notifyDataSetChanged()
                    updateActiveDisplay()
                }
            }
            .setNegativeButton(getString(R.string.no), null)
            .show()
    }

    private fun loadAzkarData() {
        val currentLang = sharedPreferences.getString("app_lang", "ar") ?: "ar"
        val json = sharedPreferences.getString("saved_azkar_json", null)
        if (!json.isNullOrEmpty()) {
            val type = object : TypeToken<MutableList<Dhikr>>() {}.type
            val loaded: MutableList<Dhikr> = gson.fromJson(json, type)
            azkarList.clear()
            azkarList.addAll(loaded)
        }

        if (azkarList.isEmpty()) {
            azkarList.addAll(getDefaultAzkar(currentLang))
        } else {
            // Translate default items if they exist to match current language
            azkarList.forEach { item ->
                val defaultItem = getDefaultAzkar(currentLang).find { it.id == item.id }
                if (defaultItem != null) {
                    item.text = defaultItem.text
                    item.virtue = defaultItem.virtue
                    item.category = defaultItem.category
                }
            }
        }

        val lastSelectedId = sharedPreferences.getLong("last_selected_dhikr_id", azkarList[0].id)
        activeDhikr = azkarList.find { it.id == lastSelectedId } ?: azkarList[0]
    }

    private fun getDefaultAzkar(lang: String): List<Dhikr> {
        return when (lang) {
            "en" -> listOf(
                Dhikr(1L, "Subhan-Allahi wa bihamdihi, Subhan-Allahil-Azim", "Two words are light on the tongue, heavy in the balance, beloved to the Most Merciful", "Repentance & Istighfar", 0, 100),
                Dhikr(2L, "Astaghfirullah al-Adheem wa atoobu ilayh", "If anyone constantly seeks pardon, Allah will appoint for him a way out of every distress", "Repentance & Istighfar", 0, 70),
                Dhikr(3L, "La hawla wa la quwwata illa billahil-Aliyyil-Azheem", "A treasure from the treasures of Paradise", "Prophetic Supplications", 0, 33),
                Dhikr(4L, "Allahumma salli wa sallim 'ala nabiyyina Muhammad", "Whoever sends blessings upon me once, Allah sends blessings upon him ten times", "Prayer Azkar", 0, 100)
            )
            "fr" -> listOf(
                Dhikr(1L, "Subhan-Allahi wa bihamdihi, Subhan-Allahil-Azim", "Deux paroles légères sur la langue, lourdes sur la balance, aimées du Tout-Miséricordieux", "Repentance & Istighfar", 0, 100),
                Dhikr(2L, "Astaghfirullah al-Adheem wa atoobu ilayh", "Quiconque implore le pardon d'Allah, Allah lui ménagera une issue à chaque détresse", "Repentance & Istighfar", 0, 70),
                Dhikr(3L, "La hawla wa la quwwata illa billahil-Aliyyil-Azheem", "Un trésor parmi les trésors du Paradis", "Invocations Prophétiques", 0, 33),
                Dhikr(4L, "Allahumma salli wa sallim 'ala nabiyyina Muhammad", "Quiconque prie sur moi une fois, Allah prie sur lui dix fois", "Invocations de Prière", 0, 100)
            )
            else -> listOf(
                Dhikr(1L, "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ ، سُبْحَانَ اللَّهِ الْعَظِيمِ", "حبيبتان إلى الرحمن، خفيفتان على اللسان، ثقيلتان في الميزان", "استغفار وتوبة", 0, 100),
                Dhikr(2L, "أَسْتَغْفِرُ اللَّهَ الْعَظِيمَ وَأَتُوبُ إِلَيْهِ", "من لزم الاستغفار جعل الله له من كل هم فرجاً", "استغفار وتوبة", 0, 70),
                Dhikr(3L, "لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ الْعَلِيِّ الْعَظِيمِ", "كنز من كنوز الجنة", "أدعية مأثورة", 0, 33),
                Dhikr(4L, "اللَّهُمَّ صَلِّ وَسَلِّمْ عَلَى نَبِيِّنَا مُحَمَّدٍ", "من صلى علي صلاة صلى الله عليه بها عشراً", "أذكار الصلاة", 0, 100)
            )
        }
    }

    private fun saveAzkarData() {
        val editor = sharedPreferences.edit()
        val json = gson.toJson(azkarList)
        editor.putString("saved_azkar_json", json)
        activeDhikr?.let {
            editor.putLong("last_selected_dhikr_id", it.id)
        }
        editor.apply()
    }

    private fun vibrateDevice(durationMillis: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                val vibrator = vibratorManager.defaultVibrator
                vibrator.vibrate(VibrationEffect.createOneShot(durationMillis, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(durationMillis, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(durationMillis)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}