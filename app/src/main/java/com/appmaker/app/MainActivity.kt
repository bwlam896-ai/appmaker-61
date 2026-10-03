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
    private lateinit var rvAzkarList: RecyclerView

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
        rvAzkarList = findViewById(R.id.rvAzkarList)
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
                    .setTitle("تصفير العداد")
                    .setMessage("هل تود تصفير هذا الذكر؟")
                    .setPositiveButton("نعم") { _, _ ->
                        dhikr.count = 0
                        saveAzkarData()
                        updateActiveDisplay()
                        adapter.notifyDataSetChanged()
                    }
                    .setNegativeButton("إلغاء", null)
                    .show()
            }
        }

        btnAddDhikr.setOnClickListener {
            showAddDhikrDialog()
        }
    }

    private fun updateActiveDisplay() {
        val dhikr = activeDhikr ?: return
        tvCurrentCategory.text = dhikr.category
        tvCurrentTarget.text = getString(R.string.target_label, dhikr.target)
        tvCurrentDhikrText.text = dhikr.text
        tvCurrentVirtue.text = if (dhikr.virtue.isNotEmpty()) dhikr.virtue else "فضل وثواب الذكر عظيم عند الله"
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

        val categories = arrayOf("أذكار الصباح والمساء", "أذكار الصلاة", "استغفار وتوبة", "أدعية مأثورة", "ذكر عام")
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
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
                Toast.makeText(this, "تمت إضافة الذكر إلى المختارة بنجاح", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .create()
            .show()
    }

    private fun confirmDelete(dhikr: Dhikr) {
        if (azkarList.size <= 1) {
            Toast.makeText(this, "يجب أن يبقى ذكر واحد على الأقل في القائمة", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.delete_confirm_title)
            .setMessage(R.string.delete_confirm_msg)
            .setPositiveButton("حذف") { _, _ ->
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
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun loadAzkarData() {
        val json = sharedPreferences.getString("saved_azkar_json", null)
        if (!json.isNullOrEmpty()) {
            val type = object : TypeToken<MutableList<Dhikr>>() {}.type
            val loaded: MutableList<Dhikr> = gson.fromJson(json, type)
            azkarList.clear()
            azkarList.addAll(loaded)
        }

        if (azkarList.isEmpty()) {
            // Default built-in selected Azkar
            azkarList.add(
                Dhikr(
                    id = 1L,
                    text = "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ ، سُبْحَانَ اللَّهِ الْعَظِيمِ",
                    virtue = "حبيبتان إلى الرحمن، خفيفتان على اللسان، ثقيلتان في الميزان",
                    category = "استغفار وتوبة",
                    count = 0,
                    target = 100
                )
            )
            azkarList.add(
                Dhikr(
                    id = 2L,
                    text = "أَسْتَغْفِرُ اللَّهَ الْعَظِيمَ وَأَتُوبُ إِلَيْهِ",
                    virtue = "من لزم الاستغفار جعل الله له من كل هم فرجاً",
                    category = "استغفار وتوبة",
                    count = 0,
                    target = 70
                )
            )
            azkarList.add(
                Dhikr(
                    id = 3L,
                    text = "لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ الْعَلِيِّ الْعَظِيمِ",
                    virtue = "كنز من كنوز الجنة",
                    category = "أدعية مأثورة",
                    count = 0,
                    target = 33
                )
            )
            azkarList.add(
                Dhikr(
                    id = 4L,
                    text = "اللَّهُمَّ صَلِّ وَسَلِّمْ عَلَى نَبِيِّنَا مُحَمَّدٍ",
                    virtue = "من صلى علي صلاة صلى الله عليه بها عشراً",
                    category = "أذكار الصلاة",
                    count = 0,
                    target = 100
                )
            )
        }

        val lastSelectedId = sharedPreferences.getLong("last_selected_dhikr_id", azkarList[0].id)
        activeDhikr = azkarList.find { it.id == lastSelectedId } ?: azkarList[0]
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