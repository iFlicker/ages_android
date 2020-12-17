package com.ffflicker.ages_android

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.WindowManager
import android.widget.*
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : Activity(), DatePickerDialog.OnDateSetListener {

    companion object {
        public var mDateOfBirth:Long = 0
        public var mTargetAge:Int = 35
        public var mTargetTimpStamp:Long = 3376656000000L // 2077年
        val PARAM_BIRTH = "birth"
        val PARAM_TARGET = "target"
    }

    private val mCalendar:Calendar = Calendar.getInstance()
    private lateinit var mDateTextView:TextView
    private lateinit var mSP:SharedPreferences
    private var currentYear = 2020

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        currentYear = SimpleDateFormat("yyyy").format(Date(System.currentTimeMillis())).toInt()

        mSP = getSharedPreferences("ages", MODE_PRIVATE)
        mDateTextView = findViewById(R.id.date)

        mDateOfBirth = getParam(PARAM_BIRTH, 0)
        mTargetAge = getParam(PARAM_TARGET, 35).toInt()
        findViewById<EditText>(R.id.countdown_age).setText(mTargetAge.toString())


        findViewById<View>(R.id.pickDate).setOnClickListener {
            openDatePicker()
        }

        val intent = Intent(this, AgeService::class.java)
        findViewById<Switch>(R.id.switch_ages).setOnCheckedChangeListener { buttonView, isChecked ->
            intent.putExtra(AgeService.ACTION_TYPE, AgeService.ACTION_AGES)
            if (isChecked)
                intent.putExtra(AgeService.ACTION_STATE, true)
            else
                intent.putExtra(AgeService.ACTION_STATE, false)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                startForegroundService(intent)
            else
                startService(intent)
        }

        findViewById<Switch>(R.id.switch_countdown).setOnCheckedChangeListener { buttonView, isChecked ->
            intent.putExtra(AgeService.ACTION_TYPE, AgeService.ACTION_COUNTDOWN)
            if (isChecked)
                intent.putExtra(AgeService.ACTION_STATE, true)
            else
                intent.putExtra(AgeService.ACTION_STATE, false)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                startForegroundService(intent)
            else
                startService(intent)
        }

        findViewById<EditText>(R.id.countdown_age).addTextChangedListener(
            object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                }

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                }

                override fun afterTextChanged(s: Editable?) {
                    mTargetAge = s.toString().toInt()
                    // 根据当前结果生日算出目标年龄时间戳
                    updateTargetTime()
                    saveParam(PARAM_TARGET, mTargetAge.toLong())
                }
            }
        )

    }

    private fun updateTargetTime() {
        val targetYear = mCalendar.get(Calendar.YEAR) + mTargetAge
        if (targetYear < currentYear) {
            Toast.makeText(this@MainActivity, "目标年龄低于当前年龄！", Toast.LENGTH_SHORT).show()
            return
        }
        var calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, targetYear)
        calendar.set(Calendar.MONTH, mCalendar.get(Calendar.MONTH))
        calendar.set(Calendar.DAY_OF_MONTH, mCalendar.get(Calendar.DAY_OF_MONTH))
        mTargetTimpStamp = calendar.timeInMillis
    }

    private fun saveParam(key:String, value:Long ) {
        mSP.edit().putLong(key, value).apply()
    }

    private fun getParam(key:String, default:Long ) :Long {
        return mSP.getLong(key, default)
    }

    private fun openDatePicker() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            val datePickerDialog = DatePickerDialog(this, this, mCalendar.get(Calendar.YEAR),
                mCalendar.get(Calendar.MONTH),
                mCalendar.get(Calendar.DAY_OF_MONTH))
            datePickerDialog.show()
        } else {
            val builder = AlertDialog.Builder(this)
            val view = View.inflate(this, R.layout.datedialog, null)
            val datePicker = view.findViewById(R.id.date_dialog_picker) as DatePicker
            builder.setView(view)
            builder.setPositiveButton(getString(R.string.text_ok)) { dialog, _ ->
                val year = datePicker.year
                val monthTemp = datePicker.month
                val day = datePicker.dayOfMonth
                mCalendar.set(Calendar.YEAR, year)
                mCalendar.set(Calendar.MONTH, monthTemp)
                mCalendar.set(Calendar.DAY_OF_MONTH, day)
                mDateTextView.text = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(mCalendar.time)
                mDateOfBirth = mCalendar.timeInMillis
                updateTargetTime()
                saveParam(PARAM_BIRTH, mDateOfBirth)
                dialog.cancel()
            }
            val dialog: Dialog = builder.create()
            val lp: WindowManager.LayoutParams = dialog.window!!.attributes  // attention
            lp.alpha = 1f
            dialog.window!!.attributes = lp
            dialog.setCancelable(false)
            dialog.show()
        }
    }

    override fun onDateSet(view: DatePicker?, year: Int, month: Int, dayOfMonth: Int) {
        mCalendar.set(Calendar.YEAR, year)
        mCalendar.set(Calendar.MONTH, month)
        mCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
        mDateTextView.text = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(mCalendar.time)
        mDateOfBirth = mCalendar.timeInMillis
        updateTargetTime()
        saveParam(PARAM_BIRTH, mDateOfBirth)
    }


}