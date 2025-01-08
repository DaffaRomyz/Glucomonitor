package com.daffaromyz.glucomonitor.ui.edit

import androidx.fragment.app.viewModels
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.daffaromyz.glucomonitor.R
import com.daffaromyz.glucomonitor.database.Glucose
import com.daffaromyz.glucomonitor.database.GlucoseDao
import com.daffaromyz.glucomonitor.database.GlucoseDatabase
import com.daffaromyz.glucomonitor.databinding.FragmentDashboardBinding
import com.daffaromyz.glucomonitor.databinding.FragmentEditBinding
import com.daffaromyz.glucomonitor.ui.dashboard.DashboardViewModel
import com.daffaromyz.glucomonitor.ui.record.GlucoseAdapter
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class EditFragment : Fragment() {

    private var _binding : FragmentEditBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private var isUnitMg : Boolean? = null

    private lateinit var db : GlucoseDatabase
    private lateinit var dao : GlucoseDao

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val editViewModel : EditViewModel by viewModels{ EditViewModel.Factory}

        _binding = FragmentEditBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val glucoseid = arguments?.getInt("glucoseid")!!

        db = GlucoseDatabase.getDatabase(this.requireContext())
        dao = db.glucoseDao()

        Log.i("GLUCOSE ID", glucoseid.toString())
        // Show glucose data
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                editViewModel.homeUiState.collect {
                    homeUiState ->
                    val glucose_list = homeUiState.glucoseList
                    glucose_list.forEach {
                        if (it.id == glucoseid) {
                            val datetext = it.datetime.format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"))
                            binding.editTextDatetimeInput.setText(datetext)
                            binding.editTextValueInput.setText(it.value.toString())
                            binding.editUnitRadio.check(R.id.edit_mg_button)
                        }
                    }
                }
            }
        }

        // Save input
        binding.editMgButton.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                isUnitMg = true
                Log.i("UNIT CHANGED", "mg")
                checkInputValid()
            }
        }

        binding.editMmolButton.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                isUnitMg = false
                Log.i("UNIT CHANGED", "mmol")
                checkInputValid()
            }
        }

        binding.editTextValueInput.addTextChangedListener {
            checkInputValid()
        }

        binding.editSaveValueButton.setOnClickListener {
            val glucoseText = binding.editTextValueInput.text.toString()

            val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")
            val dateText = LocalDateTime.parse(binding.editTextDatetimeInput.text, formatter)

            // mg have to be Int
            if (glucoseText.toIntOrNull() is Int) {
                if (isUnitMg == true) {
                    lifecycleScope.launch {
                        dao.update(Glucose(id = glucoseid, datetime = dateText, value = glucoseText.toInt()))
                        Log.i("INSERT", "mg $glucoseText")
                    }
                    Toast.makeText(this.requireContext(), "Reading Edited", Toast.LENGTH_SHORT).show()
                } else { // mg but value is float
                    Toast.makeText(this.requireContext(), "Value is Integer but Unit is not mg/dL", Toast.LENGTH_SHORT).show()
                }
            } else if (glucoseText.toDoubleOrNull() is Double) { // mmol is float
                if (isUnitMg == false) {
                    val glucoseValue = glucoseText.toDouble() * 18.0156
                    lifecycleScope.launch {
                        dao.update(Glucose(id = glucoseid, datetime = dateText, value = glucoseText.toInt()))
                        Log.i("INSERT", "mmol $glucoseText")
                    }
                    Toast.makeText(this.requireContext(), "Reading Edited", Toast.LENGTH_SHORT).show()
                } else { // mmol but value is integer
                    Toast.makeText(this.requireContext(), "Value is Decimal but Unit is not mmol/L", Toast.LENGTH_SHORT).show()
                }
            } else { // value is null
                Toast.makeText(this.requireContext(), "Value is Null", Toast.LENGTH_SHORT).show()
            }

            view?.findNavController()?.navigate(R.id.action_navigate_to_record)
        }
        return root
    }

    private fun checkInputValid() {
        if (binding.editTextValueInput.text.isNotBlank() && isUnitMg is Boolean) {
            binding.editSaveValueButton.setEnabled(true)
        } else {
            binding.editSaveValueButton.setEnabled(false)
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}