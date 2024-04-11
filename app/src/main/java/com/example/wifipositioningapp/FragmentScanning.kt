package com.example.wifipositioningapp

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.concurrent.thread

class FragmentScanning(private val dbHelper: DbHelper, private val database: SQLiteDatabase) : Fragment() {
    private lateinit var wifiList: ListView
    private lateinit var xText: EditText
    private lateinit var yText: EditText
    private lateinit var scanButton: Button
    private lateinit var deleteButton: Button

    private lateinit var wifiManager: WifiManager
    private lateinit var wifiReceiver: WifiReceiver

    private val SCAN_NUMBER: Int = 4
    private val SCAN_FREQ: Int = 7

    private var scans: HashMap<String, ArrayList<Int>> = HashMap()
    private var count: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.CHANGE_WIFI_STATE) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.CHANGE_WIFI_STATE,
                    Manifest.permission.ACCESS_WIFI_STATE),
                1)
        }

        wifiManager = requireContext().getSystemService(Context.WIFI_SERVICE) as WifiManager

        if (!wifiManager.isWifiEnabled) {
            Toast.makeText(requireContext(), "Please turn WiFi On", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_scanning, container, false)

        wifiList = view.findViewById(R.id.wifiList)
        xText = view.findViewById(R.id.editX)
        yText = view.findViewById(R.id.editY)
        scanButton = view.findViewById(R.id.scanButton)
        deleteButton = view.findViewById(R.id.database_delete)

        startScan()

        return view
    }

    private fun startScan() {
        val watcher = object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                scanButton.isEnabled = !(xText.text.toString() == "" || yText.text.toString() == "")
            }

            override fun afterTextChanged(s: Editable?) {}
        }
        xText.addTextChangedListener(watcher)
        yText.addTextChangedListener(watcher)

        deleteButton.setOnClickListener {
            Toast.makeText(requireContext(), "${dbHelper.deleteAll(database)} rows deleted.", Toast.LENGTH_SHORT).show()
        }

        scanButton.setOnClickListener {
            val x = xText.text.toString().toInt()
            val y = yText.text.toString().toInt()
//            Toast.makeText(requireContext(), "x: $x, y: $y", Toast.LENGTH_SHORT).show()

            xText.clearFocus()
            yText.clearFocus()
            val imm = requireContext().getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(xText.windowToken, 0)
            imm.hideSoftInputFromWindow(yText.windowToken, 0)

            val test = dbHelper.addNewReference(database, x, y)
//            Toast.makeText(requireContext(), test.toString(), Toast.LENGTH_SHORT).show()

            scanButton.isEnabled = false
            GlobalScope.launch { runScans() } // Start a coroutine that calls startScan()
        }
    }

    private suspend fun runScans() {
        repeat(SCAN_NUMBER) {
            // wifiReceiver callback will handle averaging of scans and putting it into the database
            wifiManager.startScan()

            delay((SCAN_FREQ * 1000).toLong())
        }

        scanButton.post {
            scanButton.isEnabled = true
        }
    }

    override fun onResume() {
        super.onResume()

        wifiReceiver = WifiReceiver(wifiManager, object: ScanCallBack {
            override fun addScansCallback(results: List<ScanResult>) {
                // Display data to list view
                val resultList: ArrayList<String> = ArrayList()

                for (scanResult in results) {
                    //                sb!!.append("\n").append(scanResult.SSID).append(" - ").append(scanResult.level)
                    resultList.add(scanResult.SSID.toString() + ":" + scanResult.BSSID + " - (" + scanResult.level + ")")
                }
                //            Toast.makeText(context, sb, Toast.LENGTH_SHORT).show()
                val arrayAdapter: ArrayAdapter<*> = ArrayAdapter(context!!.applicationContext, android.R.layout.simple_list_item_1, resultList.toArray())
                wifiList.adapter = arrayAdapter

                // Add scans to buffer
                for (result in results) {
                    val mac = result.BSSID.toString()
                    if (scans.containsKey(mac)) { // If address is already in the map, update the corresponding list with the new level
                        scans[mac] = (scans[mac]!!.plus(mutableListOf(result.level)) as java.util.ArrayList<Int>)
                    } else {
                        scans[mac] = (mutableListOf(result.level) as java.util.ArrayList<Int>) // Else make a new entry
                    }
                }

                count += 1
                Toast.makeText(requireContext(), "Completed scan $count", Toast.LENGTH_SHORT).show()

                // Check if required number of scans have been completed
                if (count == SCAN_NUMBER) {
                    Toast.makeText(requireContext(), "Finished scans.", Toast.LENGTH_SHORT).show()
                    for ((address, values) in scans) {
                        dbHelper.updateOrAddNewScan(
                            database,
                            xText.text.toString().toInt(),
                            yText.text.toString().toInt(),
                            address,
                            values.average().toInt()
                        )
                    }
                    scans.clear()
                    count = 0
                }
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(requireContext(), "Permission granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Permission not granted", Toast.LENGTH_SHORT).show()
        }
    }
}