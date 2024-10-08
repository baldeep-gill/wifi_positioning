package com.example.wifipositioningapp

import android.content.Context
import android.content.IntentFilter
import android.database.sqlite.SQLiteDatabase
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.switchmaterial.SwitchMaterial

class FragmentPositioning(private val dbHelper: DbHelper, private val database: SQLiteDatabase) : Fragment() {
    private lateinit var positioningText: TextView
    private lateinit var positioningButton: Button
    private lateinit var measuresSpinner: Spinner
    private lateinit var weightedSwitch: SwitchMaterial

    private lateinit var wifiManager: WifiManager
    private lateinit var wifiReceiver: WifiReceiver
    private val positioning: Positioning = Positioning()

    private lateinit var scanResults: HashMap<Int, HashMap<String, Int>>
    private lateinit var referencePoints: HashMap<Int, Pair<Int, Int>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        wifiManager = requireContext().getSystemService(Context.WIFI_SERVICE) as WifiManager
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_positioning, container, false)


        positioningText = view.findViewById(R.id.positioning_text)
        positioningButton = view.findViewById(R.id.positioning_button)

        measuresSpinner = view.findViewById(R.id.spinner)
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.measures_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            measuresSpinner.adapter = adapter
        }

        positioningButton.setOnClickListener {
            scanResults = dbHelper.getAllScanResults(database)
            referencePoints = dbHelper.getAllReferencePoints(database)
            positioning.updateOfflineData(scanResults)

            wifiManager.startScan()
        }

        weightedSwitch = view.findViewById(R.id.weighted_switch)


        return view
    }
    // Remember modularity of KNN vs WKNN
    private fun calculatePosition(results: List<ScanResult>) {
        val unseen = HashMap<String, Int>()

        for (result in results) {
            unseen[result.BSSID] = result.level
        }

        val position = positioning.calculatePosition(unseen, Measures.getMeasureByName(measuresSpinner.selectedItem.toString()), referencePoints, weightedSwitch.isChecked)

        positioningText.text = buildString {
            append("Position: (${position.first}, ${position.second})")
        }
    }

    override fun onResume() {
        super.onResume()

        wifiReceiver = WifiReceiver(wifiManager, object: ScanCallBack {
            override fun addScansCallback(results: List<ScanResult>) {
                calculatePosition(results)
            }
        })

        val intentFilter = IntentFilter()
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        requireContext().registerReceiver(wifiReceiver, intentFilter)
    }

    override fun onPause() {
        super.onPause()
        requireContext().unregisterReceiver(wifiReceiver)
    }
}