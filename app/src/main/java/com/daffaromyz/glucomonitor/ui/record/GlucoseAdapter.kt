package com.daffaromyz.glucomonitor.ui.record

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.RecyclerView
import com.daffaromyz.glucomonitor.R
import com.daffaromyz.glucomonitor.database.Glucose
import com.daffaromyz.glucomonitor.database.GlucoseDao
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.runBlocking
import java.time.format.DateTimeFormatter

class GlucoseAdapter(private val dataSet: List<Glucose>)
    : RecyclerView.Adapter<GlucoseAdapter.ViewHolder>() {

    private var deleteOnClickListener: OnClickListener? = null
    private var editOnClickListener: OnClickListener? = null
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val textDatetime: TextView
        val textValue: TextView
        val textClass: TextView
        val deleteButton : MaterialButton

        init {
            textDatetime = view.findViewById(R.id.record_datetime)
            textValue = view.findViewById(R.id.record_value)
            textClass = view.findViewById(R.id.record_class)
            deleteButton = view.findViewById(R.id.delete_button)
        }

    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.record_item, viewGroup, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {

        viewHolder.textDatetime.text = dataSet[position].datetime.format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"))

        val value = dataSet[position].value
        viewHolder.textValue.text = value.toString()

        var valueClass = ""
        var colorValue  = 0xFF9B0000
        if (value >= 126) {
            valueClass = "Diabetes"
            colorValue = 0xFF9B0000
        } else if (value >= 100) {
            valueClass = "Prediabetes"
            colorValue = 0xFF9B9B00
        } else if (value >= 70) {
            valueClass = "Normal"
            colorValue = 0xFF009B00
        } else {
            valueClass = "Hypogycemia"
            colorValue = 0xFF009B9B
        }

        viewHolder.textClass.text = valueClass
//        viewHolder.textClass.setBackgroundColor(colorValue.toColorInt())

        viewHolder.deleteButton.setOnClickListener {
            if (deleteOnClickListener != null) {
                deleteOnClickListener!!.onClick(position, dataSet[position])
            }
        }
    }

    override fun getItemCount() = dataSet.size

    // A function to bind the onclickListener.
    fun setDeleteOnClickListener(onClickListener: OnClickListener) {
        this.deleteOnClickListener = onClickListener
    }

    fun setEditOnClickListener(onClickListener: OnClickListener) {
        this.editOnClickListener = onClickListener
    }

    // onClickListener Interface
    interface OnClickListener {
        fun onClick(position: Int, model: Glucose)
    }

}