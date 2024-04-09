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
import android.widget.Button
import android.widget.TextView

class FragmentPositioning(private val dbHelper: DbHelper, private val database: SQLiteDatabase) : Fragment() {
    private lateinit var positioningText: TextView
    private lateinit var positioningButton: Button

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

        positioningButton.setOnClickListener {
            scanResults = dbHelper.getAllScanResults(database)
            referencePoints = dbHelper.getAllReferencePoints(database)
            positioning.updateOfflineData(scanResults)

            wifiManager.startScan()
        }

        return view
    }
    // Remember modularity of KNN vs WKNN
    private fun calculatePosition(results: List<ScanResult>) {
        val unseen = HashMap<String, Int>()

        for (result in results) {
            unseen[result.BSSID] = result.level
        }

        val position = positioning.calculatePosition(unseen, Measures.EUCLIDEAN, referencePoints, false)

        positioningText.text = buildString {
            append("Position: (${position.first}, ${position.second})")
        }
    }

    override fun onResume() {
        super.onResume()

        wifiReceiver = WifiReceiver(wifiManager, null, object: ScanCallBack {
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