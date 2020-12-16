package com.ffflicker.ages_android

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.CompoundButton
import android.widget.DatePicker
import android.widget.Switch
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : Activity(), DatePickerDialog.OnDateSetListener {

    companion object {
        public var mDateOfBirth:Long = 0
    }

    private val mCalendar:Calendar = Calendar.getInstance()
    private lateinit var mDateTextView:TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mDateTextView = findViewById(R.id.date)
        findViewById<View>(R.id.pickDate).setOnClickListener {
            openDatePicker()
        }

        findViewById<Switch>(R.id.switchNotification).setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    startForegroundService(Intent(this, AgeService::class.java))
                else
                    startService(Intent(this, AgeService::class.java))
            } else {
                stopService(Intent(this, AgeService::class.java))
            }
        }

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
    }


}