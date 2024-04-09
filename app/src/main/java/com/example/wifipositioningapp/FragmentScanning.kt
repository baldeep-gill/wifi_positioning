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
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.core.app.ActivityCompat

class FragmentScanning(private val dbHelper: DbHelper, private val database: SQLiteDatabase) : Fragment() {
    private lateinit var wifiList: ListView
    private lateinit var xText: EditText
    private lateinit var yText: EditText
    private lateinit var scanButton: Button

    private lateinit var wifiManager: WifiManager
    private lateinit var wifiReceiver: WifiReceiver

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
            Toast.makeText(requireContext(), test.toString(), Toast.LENGTH_SHORT).show()

            wifiManager.startScan()
        }
    }

    override fun onResume() {
        super.onResume()

        wifiReceiver = WifiReceiver(wifiManager, wifiList, object: ScanCallBack {
            override fun addScansCallback(results: List<ScanResult>) {
                for (result in results) {
                    dbHelper.updateOrAddNewScan(database, xText.text.toString().toInt(), yText.text.toString().toInt(), result.BSSID.toString(), result.level)
                }
                /**
                 * TODO: Make multiple scans before saving the result to the database
                 *
                 * Maybe make the callback a function that saves a list of previous scan results.
                 * When list reaches a certain length, average and then add to database and then clear the list.
                 * Make sure it's unique for each rp.
                 *
                 * E.g. Put start scan on a 15 second loop and have this callback add to the list or update when ready?
                 */
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