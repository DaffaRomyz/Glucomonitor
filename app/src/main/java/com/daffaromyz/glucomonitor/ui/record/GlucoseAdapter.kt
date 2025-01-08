package com.daffaromyz.glucomonitor.ui.record

import android.graphics.Color
import android.icu.text.DecimalFormat
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
        val textValueMmol : TextView
        val textClass: TextView
        val divider : View
        val deleteButton : MaterialButton
        val editButton : MaterialButton

        init {
            textDatetime = view.findViewById(R.id.record_datetime)
            textValue = view.findViewById(R.id.record_value)
            textValueMmol =  view.findViewById(R.id.record_value_mmol)
            textClass = view.findViewById(R.id.record_class)
            divider = view.findViewById(R.id.record_divider)
            deleteButton = view.findViewById(R.id.delete_button)
            editButton = view.findViewById(R.id.edit_button)
        }

    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.record_item, viewGroup, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {

        viewHolder.textDatetime.text = dataSet[position].datetime.format(DateTimeFormatter.ofPattern("dd MMM HH:mm"))

        val value = dataSet[position].value

        viewHolder.textValue.text = value.toString()

        val decimalFormat = DecimalFormat("#.#")
        viewHolder.textValueMmol.text = decimalFormat.format(value / 18.0156)
        var valueClass = ""

        if (value >= 126) {
            valueClass = "Diabetes"
            viewHolder.divider.setBackgroundColor(Color.rgb(155,0, 0))
        } else if (value >= 100) {
            valueClass = "Prediabetes"
            viewHolder.divider.setBackgroundColor(Color.rgb(155,155,0))
        } else if (value >= 70) {
            valueClass = "Normal"
            viewHolder.divider.setBackgroundColor(Color.rgb(0,155,0))
        } else {
            valueClass = "Low"
            viewHolder.divider.setBackgroundColor(Color.rgb(0, 155,155))
        }

        viewHolder.textClass.text = valueClass

        viewHolder.deleteButton.setOnClickListener {
            if (deleteOnClickListener != null) {
                deleteOnClickListener!!.onClick(position, dataSet[position])
            }
        }

        viewHolder.editButton.setOnClickListener {
            if (editOnClickListener != null) {
                editOnClickListener!!.onClick(position, dataSet[position])
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