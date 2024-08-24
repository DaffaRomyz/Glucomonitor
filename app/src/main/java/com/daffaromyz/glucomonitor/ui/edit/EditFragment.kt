package com.daffaromyz.glucomonitor.ui.edit

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.daffaromyz.glucomonitor.R
import com.daffaromyz.glucomonitor.database.Glucose
import com.daffaromyz.glucomonitor.databinding.FragmentDashboardBinding
import com.daffaromyz.glucomonitor.databinding.FragmentEditBinding
import com.daffaromyz.glucomonitor.ui.dashboard.DashboardViewModel
import com.daffaromyz.glucomonitor.ui.record.GlucoseAdapter
import kotlinx.coroutines.launch

class EditFragment : Fragment() {

    private var _binding : FragmentEditBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val glucoseid = arguments?.getInt("glucoseid")
        val editViewModel : EditViewModel by viewModels{ EditViewModel.Factory}

        _binding = FragmentEditBinding.inflate(inflater, container, false)
        val root: View = binding.root

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                editViewModel.homeUiState.collect {
                    homeUiState ->
                    val glucose_list = homeUiState.glucoseList
                    glucose_list.forEach {
                        if (it.id == glucoseid) {
                            val datetext = StringBuilder()
                            datetext.append(it.datetime.year)
                            datetext.append("/")
                            datetext.append(it.datetime.month)
                            datetext.append("/")
                            datetext.append(it.datetime.dayOfMonth)
                            datetext.append(" ")
                            datetext.append(it.datetime.hour)
                            datetext.append(":")
                            datetext.append(it.datetime.minute)
                            datetext.append(":")
                            datetext.append(it.datetime.second)
                            binding.editTextDatetimeInput.setText(datetext)
                            binding.editTextValueInput.setText(it.value.toString())
                            binding.editUnitRadio.check(R.id.edit_mg_button)
                        }
                    }
                }
            }
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}