package com.appmaker.app

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private var currentCount = 33
    private var maxCount = 33

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val tvZikrText = findViewById<TextView>(R.id.tvZikrText)
        val tvCounter = findViewById<TextView>(R.id.tvCounter)
        val tvCurrentZikrBadge = findViewById<TextView>(R.id.tvCurrentZikrBadge)
        val btnTasbeeh = findViewById<Button>(R.id.btnTasbeeh)
        val btnReset = findViewById<Button>(R.id.btnReset)

        val rowZikr1 = findViewById<RelativeLayout>(R.id.rowZikr1)
        val rowZikr2 = findViewById<RelativeLayout>(R.id.rowZikr2)
        val rowZikr3 = findViewById<RelativeLayout>(R.id.rowZikr3)

        fun updateDisplay() {
            tvCounter.text = "$currentCount"
        }

        fun selectZikr(name: String, text: String, count: Int) {
            maxCount = count
            currentCount = count
            tvCurrentZikrBadge.text = name
            tvZikrText.text = text
            updateDisplay()
        }

        // اختيار الذكر الأول
        rowZikr1?.setOnClickListener {
            selectZikr("سبحان الله", "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ", 33)
        }

        // اختيار الذكر الثاني
        rowZikr2?.setOnClickListener {
            selectZikr("الحمد لله", "الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ", 33)
        }

        // اختيار الذكر الثالث
        rowZikr3?.setOnClickListener {
            selectZikr("الله أكبر", "اللَّهُ أَكْبَرُ كَبِيرًا", 34)
        }

        // زر التسبيح (ينقص العداد حتى الصفر)
        btnTasbeeh.setOnClickListener {
            if (currentCount > 0) {
                currentCount--
                updateDisplay()
            }
        }

        // زر إعادة الضبط
        btnReset.setOnClickListener {
            currentCount = maxCount
            updateDisplay()
        }

        // ضبط القيمة الابتدائية
        selectZikr("الله أكبر", "اللَّهُ أَكْبَرُ كَبِيرًا", 34)
    }
}
