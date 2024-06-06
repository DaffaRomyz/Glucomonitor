package com.daffaromyz.glucomonitor.ui.dashboard

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.daffaromyz.glucomonitor.database.GlucoseDao
import com.daffaromyz.glucomonitor.database.GlucoseDatabase
import com.daffaromyz.glucomonitor.databinding.FragmentDashboardBinding
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter


class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {

        val dashboardViewModel : DashboardViewModel by viewModels{ DashboardViewModel.Factory}

        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.chart.setDrawValueAboveBar(true)
        binding.chart.setDrawBarShadow(false)
        binding.chart.description.isEnabled = false
        binding.chart.setPinchZoom(false)
        binding.chart.setDrawGridBackground(false)

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                dashboardViewModel.homeUiState.collect {
                    homeUiState ->
                    val chart = binding.chart

                    val diabetesEntries = ArrayList<BarEntry>()
                    val prediabetesEntries = ArrayList<BarEntry>()
                    val normalEntries = ArrayList<BarEntry>()
                    val hypoEntries = ArrayList<BarEntry>()

                    val red = Color.rgb(155,0, 0)
                    val yellow = Color.rgb(155,155,0)
                    val green = Color.rgb(0,155,0)
                    val blue = Color.rgb(0, 155,155)

                    val reversedList = homeUiState.glucoseList.reversed()
                    val datetimeList = ArrayList<String>()
                    val colorList = ArrayList<Int>()
                    var i = 0

                    reversedList.forEach {
                        datetimeList.add(it.datetime.format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")))
                        if (it.value >= 126) {
                            diabetesEntries.add(BarEntry( i.toFloat(), it.value.toFloat()))
                        } else if (it.value >= 100) {
                            prediabetesEntries.add(BarEntry( i.toFloat(), it.value.toFloat()))
                        } else if (it.value >= 70) {
                            normalEntries.add(BarEntry( i.toFloat(), it.value.toFloat()))
                        } else {
                            hypoEntries.add(BarEntry( i.toFloat(), it.value.toFloat()))
                        }
                        i += 1
                    }

                    class formatter : ValueFormatter() {
                        override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                            return datetimeList.getOrNull(value.toInt()) ?: value.toString()
                        }
                    }

                    val diabetesDataSet = BarDataSet(diabetesEntries, "Diabetes")
                    val prediabetesDataSet = BarDataSet(prediabetesEntries, "Prediabetes")
                    val normalDataSet = BarDataSet(normalEntries, "Normal")
                    val hypoDataSet = BarDataSet(hypoEntries, "Hypoglycemia")

                    diabetesDataSet.setColor(red)
                    prediabetesDataSet.setColor(yellow)
                    normalDataSet.setColor(green)
                    hypoDataSet.setColor(blue)

                    chart.xAxis.valueFormatter = formatter()
                    chart.xAxis.granularity = 1f
                    chart.xAxis.labelRotationAngle = 45f

                    val datasets = ArrayList<IBarDataSet>()
                    datasets.add(diabetesDataSet)
                    datasets.add(prediabetesDataSet)
                    datasets.add(normalDataSet)
                    datasets.add(hypoDataSet)

                    val data = BarData(datasets)
                    chart.setData(data)

                    chart.setVisibleXRangeMaximum(reversedList.size.toFloat())
                    chart.setVisibleXRangeMinimum(6f)
                    chart.moveViewToX(reversedList.size.toFloat())

                    chart.invalidate()
                }
            }
        }

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
