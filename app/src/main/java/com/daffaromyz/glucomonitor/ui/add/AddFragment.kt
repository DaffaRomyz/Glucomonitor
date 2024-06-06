package com.daffaromyz.glucomonitor.ui.add

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.daffaromyz.glucomonitor.database.Glucose
import com.daffaromyz.glucomonitor.database.GlucoseDao
import com.daffaromyz.glucomonitor.database.GlucoseDatabase
import com.daffaromyz.glucomonitor.databinding.FragmentAddBinding
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class AddFragment : Fragment() {

    private var _binding: FragmentAddBinding? = null
    private var isUnitMg : Boolean? = null

    private lateinit var db : GlucoseDatabase
    private lateinit var dao : GlucoseDao

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
//        val addViewModel : AddViewModel = ViewModelProvider(this).get(AddViewModel::class.java)

        _binding = FragmentAddBinding.inflate(inflater, container, false)
        val root: View = binding.root

        db = GlucoseDatabase.getDatabase(this.requireContext())
        dao = db.glucoseDao()

        binding.saveValueButton.setEnabled(false)

        binding.mgButton.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                isUnitMg = true
                Log.i("UNIT CHANGED", "mg")
                checkInputValid()
            }
        }

        binding.mmolButton.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                isUnitMg = false
                Log.i("UNIT CHANGED", "mmol")
                checkInputValid()
            }
        }

        binding.textValueInput.addTextChangedListener {
            checkInputValid()
        }

        binding.saveValueButton.setOnClickListener {
            val glucoseText = binding.textValueInput.text.toString()
            if (glucoseText.toIntOrNull() is Int) {
                if (isUnitMg == true) {
                    lifecycleScope.launch {
                        dao.insert(Glucose(id = 0,value = glucoseText.toInt()))
                        Log.i("INSERT", "mg $glucoseText")
                    }
                    Toast.makeText(this.requireContext(), "Reading Successfully Added", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this.requireContext(), "Value is Integer but Unit is not mg/dL", Toast.LENGTH_SHORT).show()
                }
            } else if (glucoseText.toDoubleOrNull() is Double) {
                if (isUnitMg == false) {
                    val glucoseValue = glucoseText.toDouble() * 18.018
                    lifecycleScope.launch {
                        dao.insert(Glucose(id = 0,value = glucoseValue.toInt()))
                        Log.i("INSERT", "mmol $glucoseText")
                    }
                    Toast.makeText(this.requireContext(), "Reading Successfully Added", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this.requireContext(), "Value is Decimal but Unit is not mmol/L", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this.requireContext(), "Value is Null", Toast.LENGTH_SHORT).show()
            }

        }

//        val textView: TextView = binding.textValueInput
//        addViewModel.valueText.observe(viewLifecycleOwner) {
//            textView.text = it
//        }

        return root
    }

    private fun checkInputValid() {
        if (binding.textValueInput.text.isNotBlank() && isUnitMg is Boolean) {
            binding.saveValueButton.setEnabled(true)
        } else {
            binding.saveValueButton.setEnabled(false)
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}