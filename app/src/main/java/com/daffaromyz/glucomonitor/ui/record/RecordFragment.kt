package com.daffaromyz.glucomonitor.ui.record

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.daffaromyz.glucomonitor.R
import com.daffaromyz.glucomonitor.database.Glucose
import com.daffaromyz.glucomonitor.database.GlucoseDao
import com.daffaromyz.glucomonitor.database.GlucoseDatabase
import com.daffaromyz.glucomonitor.databinding.FragmentRecordBinding
import com.daffaromyz.glucomonitor.ui.edit.EditFragment
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter


class RecordFragment : Fragment() {

    private var _binding: FragmentRecordBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var db : GlucoseDatabase
    private lateinit var dao : GlucoseDao

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {

        val recordViewModel : RecordViewModel by viewModels{ RecordViewModel.Factory }

        _binding = FragmentRecordBinding.inflate(inflater, container, false)
        val root: View = binding.root

        db = GlucoseDatabase.getDatabase(this.requireContext())
        dao = db.glucoseDao()

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                recordViewModel.homeUiState.collect {
                    homeUiState ->
                    val glucose_list = homeUiState.glucoseList
                    val glucoseAdapter = GlucoseAdapter(glucose_list)
                    val recyclerView: RecyclerView = binding.recyclerViewGlucose
                    recyclerView.layoutManager = LinearLayoutManager(context)
                    recyclerView.adapter = glucoseAdapter

                    glucoseAdapter.setDeleteOnClickListener(object :
                        GlucoseAdapter.OnClickListener {
                        override fun onClick(position: Int, model: Glucose) {
                            lifecycleScope.launch {
                                dao.delete(model)
                                Toast.makeText(requireContext(), "Record Deleted", Toast.LENGTH_SHORT).show()
                            }
                        }
                    })

//                    glucoseAdapter.setEditOnClickListener(object :
//                        GlucoseAdapter.OnClickListener {
//                        override fun onClick(position: Int, model: Glucose) {
//                        }
//                    })
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
