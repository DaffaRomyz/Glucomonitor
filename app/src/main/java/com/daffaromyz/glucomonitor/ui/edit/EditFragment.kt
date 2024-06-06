package com.daffaromyz.glucomonitor.ui.edit

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.daffaromyz.glucomonitor.R
import com.daffaromyz.glucomonitor.databinding.FragmentDashboardBinding
import com.daffaromyz.glucomonitor.databinding.FragmentEditBinding
import com.daffaromyz.glucomonitor.ui.dashboard.DashboardViewModel
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
        val editViewModel : EditViewModel by viewModels{ EditViewModel.Factory}

        _binding = FragmentEditBinding.inflate(inflater, container, false)
        val root: View = binding.root

//        lifecycleScope.launch {
//
//        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}