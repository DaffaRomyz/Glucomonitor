package com.daffaromyz.glucomonitor.ui.dashboard

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.daffaromyz.glucomonitor.database.GlucoseDao
import com.daffaromyz.glucomonitor.database.GlucoseDatabase
import com.daffaromyz.glucomonitor.databinding.FragmentDashboardBinding
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlinx.coroutines.launch


class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null

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
        db = GlucoseDatabase.getDatabase(this.requireContext())
        dao = db.glucoseDao()

        val dashboardViewModel : DashboardViewModel by viewModels{ DashboardViewModel.Factory}

        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                dashboardViewModel.homeUiState.collect {
                    homeUiState ->
                    val chart = binding.chart
                    val entries = ArrayList<Entry>()
                    var reversedList = homeUiState.glucoseList.reversed()
                    var i = 0
                    reversedList.forEach {
                        entries.add(Entry( i.toFloat(), it.value.toFloat()))
                        i += 1
                    }
                    val lineDataSet = LineDataSet(entries, "Glucose Value")
                    lineDataSet.setColor(Color.rgb(0, 155, 0));
                    val data : LineData = LineData(lineDataSet)
                    chart.setData(data)
                    chart.invalidate()
                }
            }
        }

//        val textView: TextView = binding.textDashboard
//        dashboardViewModel.text.observe(viewLifecycleOwner) {
//            textView.text = it
//        }
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
