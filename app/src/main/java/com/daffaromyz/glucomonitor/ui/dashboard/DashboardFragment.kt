package com.daffaromyz.glucomonitor.ui.dashboard

import android.graphics.Color
import android.icu.text.DecimalFormat
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.findNavController
import com.daffaromyz.glucomonitor.R
import com.daffaromyz.glucomonitor.databinding.FragmentDashboardBinding
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


class DashboardFragment : Fragment(), AdapterView.OnItemSelectedListener {

    private var _binding: FragmentDashboardBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private var glucoseUnit = "mg"
    private var chartSelect = "bar"
    private var avgAll = 0f
    private var avgWeek = 0f
    private var avgMonth = 0f

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {

        val dashboardViewModel : DashboardViewModel by viewModels{ DashboardViewModel.Factory}

        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // setup chart select spinner
        val spinner = binding.chartSelectSpinner
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.chart_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears.
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner.
            spinner.adapter = adapter
        }

        spinner.onItemSelectedListener = this

        // listener for unit select button
        binding.unitSelectToggleButton.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Unit is mmol/L
                Log.i("UNIT CHANGE", "is checked = mmol")
                glucoseUnit = "mmol"

                // show corresponding chart
                if (chartSelect == "bar") {
                    showChart("barMmol")
                }
                else if (chartSelect == "line") {
                    showChart("lineMmol")
                }

                // set avg text
                val decimalFormat = DecimalFormat("#.##")
                if (binding.avgAllValue.text.isNotBlank()) {
                    binding.avgAllValue.text = decimalFormat.format(avgAll / 18.0156f)
                    binding.avgWeekValue.text = decimalFormat.format(avgWeek / 18.0156f)
                    binding.avgMonthValue.text = decimalFormat.format(avgMonth / 18.0156f)
                }
            } else {
                // Unit is mg/dL
                Log.i("UNIT CHANGE", "is not checked = mg")
                glucoseUnit = "mg"

                // show corresponding chart
                if (chartSelect == "bar") {
                    showChart("barMg")
                }
                else if (chartSelect == "line") {
                    showChart("lineMg")
                }

                // set avg text
                val decimalFormat = DecimalFormat("#.##")
                if (binding.avgAllValue.text.isNotBlank()) {
                    binding.avgAllValue.text = decimalFormat.format(avgAll)
                    binding.avgWeekValue.text = decimalFormat.format(avgWeek)
                    binding.avgMonthValue.text = decimalFormat.format(avgMonth)
                }
            }
        }

        // prepare color for each class
        val red = Color.rgb(155,0, 0)
        val yellow = Color.rgb(155,155,0)
        val green = Color.rgb(0,155,0)
        val blue = Color.rgb(0, 155,155)
        val white = Color.rgb(255, 255,255)
        val black = Color.rgb(0, 0,0)

        // settings for charts
        binding.barchartMg.setDrawValueAboveBar(true)
        binding.barchartMg.setDrawBarShadow(false)
        binding.barchartMg.description.isEnabled = false
        binding.barchartMg.setPinchZoom(false)
        binding.barchartMg.setDrawGridBackground(false)
        binding.barchartMg.legend.verticalAlignment = Legend.LegendVerticalAlignment.TOP
        binding.barchartMg.legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
        binding.barchartMg.legend.orientation = Legend.LegendOrientation.HORIZONTAL
        binding.barchartMg.legend.setDrawInside(false)
        binding.barchartMg.xAxis.granularity = 1f
        binding.barchartMg.xAxis.labelRotationAngle = 45f
        binding.barchartMg.xAxis.position = XAxis.XAxisPosition.BOTTOM
        binding.barchartMg.axisRight.isEnabled = false

        binding.barchartMmol.setDrawValueAboveBar(true)
        binding.barchartMmol.setDrawBarShadow(false)
        binding.barchartMmol.description.isEnabled = false
        binding.barchartMmol.setPinchZoom(false)
        binding.barchartMmol.setDrawGridBackground(false)
        binding.barchartMmol.legend.verticalAlignment = Legend.LegendVerticalAlignment.TOP
        binding.barchartMmol.legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
        binding.barchartMmol.legend.orientation = Legend.LegendOrientation.HORIZONTAL
        binding.barchartMmol.legend.setDrawInside(false)
        binding.barchartMmol.xAxis.granularity = 1f
        binding.barchartMmol.xAxis.labelRotationAngle = 45f
        binding.barchartMmol.xAxis.position = XAxis.XAxisPosition.BOTTOM
        binding.barchartMmol.axisRight.isEnabled = false

        binding.linechartMg.description.isEnabled = false
        binding.linechartMg.setPinchZoom(false)
        binding.linechartMg.setDrawGridBackground(false)
        binding.linechartMg.legend.verticalAlignment = Legend.LegendVerticalAlignment.TOP
        binding.linechartMg.legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
        binding.linechartMg.legend.orientation = Legend.LegendOrientation.HORIZONTAL
        binding.linechartMg.legend.setDrawInside(false)
        binding.linechartMg.xAxis.granularity = 1f
        binding.linechartMg.xAxis.labelRotationAngle = 45f
        binding.linechartMg.xAxis.position = XAxis.XAxisPosition.BOTTOM
        binding.linechartMg.axisRight.isEnabled = false
        binding.linechartMg.legend.isEnabled = false

        binding.linechartMmol.description.isEnabled = false
        binding.linechartMmol.setPinchZoom(false)
        binding.linechartMmol.setDrawGridBackground(false)
        binding.linechartMmol.legend.verticalAlignment = Legend.LegendVerticalAlignment.TOP
        binding.linechartMmol.legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
        binding.linechartMmol.legend.orientation = Legend.LegendOrientation.HORIZONTAL
        binding.linechartMmol.legend.setDrawInside(false)
        binding.linechartMmol.xAxis.granularity = 1f
        binding.linechartMmol.xAxis.labelRotationAngle = 45f
        binding.linechartMmol.xAxis.position = XAxis.XAxisPosition.BOTTOM
        binding.linechartMmol.axisRight.isEnabled = false
        binding.linechartMmol.legend.isEnabled = false

        binding.piechart.description.isEnabled = false
        binding.piechart.setEntryLabelTextSize(13f)
        binding.piechart.setEntryLabelColor(white)

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                dashboardViewModel.homeUiState.collect {
                    homeUiState ->

                    // get view
                    val barchartMg = binding.barchartMg
                    val barchartMmol = binding.barchartMmol
                    val linechartMg = binding.linechartMg
                    val linechartMmol = binding.linechartMmol
                    val piechart = binding.piechart

                    // prepare entries for each class
                    val barchartMgDiabetesEntries = ArrayList<BarEntry>()
                    val barchartMgPrediabetesEntries = ArrayList<BarEntry>()
                    val barchartMgNormalEntries = ArrayList<BarEntry>()
                    val barchartMgHypoEntries = ArrayList<BarEntry>()

                    val barchartMmolDiabetesEntries = ArrayList<BarEntry>()
                    val barchartMmolPrediabetesEntries = ArrayList<BarEntry>()
                    val barchartMmolNormalEntries = ArrayList<BarEntry>()
                    val barchartMmolHypoEntries = ArrayList<BarEntry>()

                    val linechartMgEntries = ArrayList<Entry>()

                    val linechartMmolEntries = ArrayList<Entry>()

                    var numDiabetes = 0f
                    var numPrediabetes = 0f
                    var numNormal = 0f
                    var numHypo = 0f

                    // Set the list to ascending order
                    val reversedList = homeUiState.glucoseList.reversed()

                    // prepare list for x axis label
                    val datetimeList = ArrayList<String>()

                    // prepare index for x axis value
                    var i = 0

                    // prepare sum and num for average
                    var currentTime = LocalDateTime.now()

                    if (reversedList.isNotEmpty()) {
                        currentTime = reversedList.last().datetime
                    }

                    var sumWeek = 0f
                    var sumMonth = 0f
                    var sumAll = 0f
                    var numWeek = 0f
                    var numMonth = 0f

                    reversedList.forEach {

                        // add datetime to x axis label
                        datetimeList.add(it.datetime.format(DateTimeFormatter.ofPattern("dd MMM HH:mm")))

                        // add entry to corresponding class
                        val glucoseValue = it.value.toFloat()

                        linechartMgEntries.add(Entry( i.toFloat(), glucoseValue))
                        linechartMmolEntries.add(Entry( i.toFloat(), glucoseValue / 18.0156f))

                        if (it.value >= 126) { // Diabetes
                            barchartMgDiabetesEntries.add(BarEntry( i.toFloat(), glucoseValue))
                            barchartMmolDiabetesEntries.add(BarEntry( i.toFloat(), glucoseValue / 18.0156f))
                            numDiabetes += 1
                        } else if (it.value >= 100) { // Prediabetes
                            barchartMgPrediabetesEntries.add(BarEntry( i.toFloat(), glucoseValue))
                            barchartMmolPrediabetesEntries.add(BarEntry( i.toFloat(), glucoseValue / 18.0156f))
                            numPrediabetes += 1
                        } else if (it.value >= 70) { // Normal
                            barchartMgNormalEntries.add(BarEntry( i.toFloat(), glucoseValue))
                            barchartMmolNormalEntries.add(BarEntry( i.toFloat(), glucoseValue / 18.0156f))
                            numNormal += 1
                        } else { // Low
                            barchartMgHypoEntries.add(BarEntry( i.toFloat(), glucoseValue))
                            barchartMmolHypoEntries.add(BarEntry( i.toFloat(), glucoseValue / 18.0156f))
                            numHypo += 1
                        }

                        // increment x axis value
                        i += 1

                        // add for avg sum
                        sumAll += glucoseValue

                        if (currentTime.dayOfYear - it.datetime.dayOfYear < 7 && currentTime.year == it.datetime.year) {
                            sumWeek += glucoseValue
                            numWeek += 1
                        }

                        if (currentTime.month == it.datetime.month && currentTime.year == it.datetime.year) {
                            sumMonth += glucoseValue
                            numMonth += 1
                        }
                    }

                    // calculate avg
                    if (reversedList.isNotEmpty()) {
                        avgAll = sumAll / reversedList.size.toFloat()
                        avgWeek = sumWeek / numWeek
                        avgMonth = sumMonth / numMonth
                    }

                    // set avg text
                    val decimalFormat = DecimalFormat("#.##")
                    binding.avgAllValue.text = decimalFormat.format(avgAll)
                    binding.avgWeekValue.text = decimalFormat.format(avgWeek)
                    binding.avgMonthValue.text = decimalFormat.format(avgMonth)

                    // set dataset with entries
                    val barchartMgDiabetesDataSet = BarDataSet(barchartMgDiabetesEntries, "Diabetes")
                    val barchartMgPrediabetesDataSet = BarDataSet(barchartMgPrediabetesEntries, "Prediabetes")
                    val barchartMgNormalDataSet = BarDataSet(barchartMgNormalEntries, "Normal")
                    val barchartMgHypoDataSet = BarDataSet(barchartMgHypoEntries, "Low")

                    val barchartMmolDiabetesDataSet = BarDataSet(barchartMmolDiabetesEntries, "Diabetes")
                    val barchartMmolPrediabetesDataSet = BarDataSet(barchartMmolPrediabetesEntries, "Prediabetes")
                    val barchartMmolNormalDataSet = BarDataSet(barchartMmolNormalEntries, "Normal")
                    val barchartMmolHypoDataSet = BarDataSet(barchartMmolHypoEntries, "Low")

                    val linechartMgDataSet = LineDataSet(linechartMgEntries, "")

                    val linechartMmolDataSet = LineDataSet(linechartMmolEntries, "")

                    val sumNum = numDiabetes + numPrediabetes + numNormal + numHypo
                    val pieEntries = ArrayList<PieEntry>()
                    pieEntries.add(PieEntry(numDiabetes/sumNum, "Diabetes"))
                    pieEntries.add(PieEntry(numPrediabetes/sumNum, "Prediabetes"))
                    pieEntries.add(PieEntry(numNormal/sumNum, "Normal"))
                    pieEntries.add(PieEntry(numHypo/sumNum, "Low"))
                    val pieDataSet = PieDataSet(pieEntries, "")

                    // set color
                    barchartMgDiabetesDataSet.setColor(red)
                    barchartMgPrediabetesDataSet.setColor(yellow)
                    barchartMgNormalDataSet.setColor(green)
                    barchartMgHypoDataSet.setColor(blue)

                    barchartMmolDiabetesDataSet.setColor(red)
                    barchartMmolPrediabetesDataSet.setColor(yellow)
                    barchartMmolNormalDataSet.setColor(green)
                    barchartMmolHypoDataSet.setColor(blue)

                    linechartMgDataSet.setColor(black)
                    linechartMgDataSet.setCircleColor(black)

                    linechartMmolDataSet.setColor(black)
                    linechartMmolDataSet.setCircleColor(black)

                    val colorList = ArrayList<Int>()
                    colorList.add(red)
                    colorList.add(yellow)
                    colorList.add(green)
                    colorList.add(blue)
                    pieDataSet.colors = colorList

                    // formatter for x axis label
                    class LabelFormatter : ValueFormatter() {
                        override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                            return datetimeList.getOrNull(value.toInt()) ?: value.toString()
                        }
                    }

                    // set formatter
                    barchartMg.xAxis.valueFormatter = LabelFormatter()

                    barchartMmol.xAxis.valueFormatter = LabelFormatter()

                    linechartMg.xAxis.valueFormatter = LabelFormatter()

                    linechartMmol.xAxis.valueFormatter = LabelFormatter()

                    pieDataSet.valueFormatter = PercentFormatter()

                    // add limit line to line chart
                    val lineDiabetesMg = LimitLine(126f, "Diabetes")
                    val linePrediabatesMg = LimitLine(100f, "Prediabetes")
                    lineDiabetesMg.lineColor = red
                    lineDiabetesMg.textColor = red
                    linePrediabatesMg.lineColor = yellow
                    linePrediabatesMg.textColor = yellow
                    linechartMg.axisLeft.addLimitLine(lineDiabetesMg)
                    linechartMg.axisLeft.addLimitLine(linePrediabatesMg)

                    val lineDiabetesMmol = LimitLine(126f / 18.0156f, "Diabetes")
                    val linePrediabatesMmol = LimitLine(100f / 18.0156f, "Prediabetes")
                    lineDiabetesMmol.lineColor = red
                    lineDiabetesMmol.textColor = red
                    linePrediabatesMmol.lineColor = yellow
                    linePrediabatesMmol.textColor = yellow
                    linechartMmol.axisLeft.addLimitLine(lineDiabetesMmol)
                    linechartMmol.axisLeft.addLimitLine(linePrediabatesMmol)

                    // set data to chart
                    val barchartMgDatasets = ArrayList<IBarDataSet>()
                    barchartMgDatasets.add(barchartMgDiabetesDataSet)
                    barchartMgDatasets.add(barchartMgPrediabetesDataSet)
                    barchartMgDatasets.add(barchartMgNormalDataSet)
                    barchartMgDatasets.add(barchartMgHypoDataSet)
                    barchartMg.setData(BarData(barchartMgDatasets))

                    val barchartMmolDatasets = ArrayList<IBarDataSet>()
                    barchartMmolDatasets.add(barchartMmolDiabetesDataSet)
                    barchartMmolDatasets.add(barchartMmolPrediabetesDataSet)
                    barchartMmolDatasets.add(barchartMmolNormalDataSet)
                    barchartMmolDatasets.add(barchartMmolHypoDataSet)
                    barchartMmol.setData(BarData(barchartMmolDatasets))

                    linechartMg.data = LineData(linechartMgDataSet)

                    linechartMmol.data = LineData(linechartMmolDataSet)

                    val pieData = PieData(pieDataSet)
                    pieData.setValueTextSize(13f)
                    pieData.setValueTextColor(white)
                    piechart.setUsePercentValues(true)
                    piechart.data = pieData


                    // chart view settings
                    barchartMg.setVisibleXRangeMaximum(reversedList.size.toFloat())
                    barchartMg.setVisibleXRangeMinimum(6f)
                    barchartMg.moveViewToX(reversedList.size.toFloat())

                    barchartMmol.setVisibleXRangeMaximum(reversedList.size.toFloat())
                    barchartMmol.setVisibleXRangeMinimum(6f)
                    barchartMmol.moveViewToX(reversedList.size.toFloat())

                    linechartMg.setVisibleXRangeMaximum(reversedList.size.toFloat())
                    linechartMg.setVisibleXRangeMinimum(6f)
                    linechartMg.moveViewToX(reversedList.size.toFloat())

                    linechartMmol.setVisibleXRangeMaximum(reversedList.size.toFloat())
                    linechartMmol.setVisibleXRangeMinimum(6f)
                    linechartMmol.moveViewToX(reversedList.size.toFloat())

                    // refresh chart
                    barchartMg.invalidate()
                    barchartMmol.invalidate()
                    linechartMg.invalidate()
                    linechartMmol.invalidate()
                    piechart.invalidate()
                }
            }
        }

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set Menu
        val menuHost: MenuHost = requireActivity()

        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                // Add menu items here
                menuInflater.inflate(R.menu.dashboard_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                // Handle the menu selection
                return when (menuItem.itemId) {
                    R.id.navigation_record -> {
                        view.findNavController().navigate(R.id.action_navigation_dashboard_to_navigation_record)
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // overide function for spinner
    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        if (parent != null) {
            val spinnerValue = parent.getItemAtPosition(position).toString()
            Log.i("SPINNER", spinnerValue)

            when (spinnerValue) {
                "Bar" -> {
                    chartSelect = "bar"

                    if (glucoseUnit == "mg") {
                        showChart("barMg")
                    } else if (glucoseUnit == "mmol") {
                        showChart("barMmol")
                    }
                }
                "Line" -> {
                    chartSelect = "line"

                    if (glucoseUnit == "mg") {
                        showChart("lineMg")
                    } else if (glucoseUnit == "mmol") {
                        showChart("lineMmol")
                    }
                }
                "Pie" -> {
                    chartSelect = "pie"
                    showChart("pie")
                }
            }
        }
    }

    // overide function for spinner
    override fun onNothingSelected(parent: AdapterView<*>?) {
        //STUB
    }

    // helper function to show one particular chart
    private fun showChart(chart: String) {
        when (chart) {
            "barMg" -> {
                binding.barchartMg.visibility = View.VISIBLE
                binding.barchartMmol.visibility = View.INVISIBLE
                binding.linechartMg.visibility = View.INVISIBLE
                binding.linechartMmol.visibility = View.INVISIBLE
                binding.piechart.visibility = View.INVISIBLE
            }
            "barMmol" -> {
                binding.barchartMg.visibility = View.INVISIBLE
                binding.barchartMmol.visibility = View.VISIBLE
                binding.linechartMg.visibility = View.INVISIBLE
                binding.linechartMmol.visibility = View.INVISIBLE
                binding.piechart.visibility = View.INVISIBLE
            }
            "lineMg" -> {
                binding.barchartMg.visibility = View.INVISIBLE
                binding.barchartMmol.visibility = View.INVISIBLE
                binding.linechartMg.visibility = View.VISIBLE
                binding.linechartMmol.visibility = View.INVISIBLE
                binding.piechart.visibility = View.INVISIBLE
            }
            "lineMmol" -> {
                binding.barchartMg.visibility = View.INVISIBLE
                binding.barchartMmol.visibility = View.INVISIBLE
                binding.linechartMg.visibility = View.INVISIBLE
                binding.linechartMmol.visibility = View.VISIBLE
                binding.piechart.visibility = View.INVISIBLE
            }
            "pie" -> {
                binding.barchartMg.visibility = View.INVISIBLE
                binding.barchartMmol.visibility = View.INVISIBLE
                binding.linechartMg.visibility = View.INVISIBLE
                binding.linechartMmol.visibility = View.INVISIBLE
                binding.piechart.visibility = View.VISIBLE
            }
        }
    }
}
