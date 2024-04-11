package com.example.wifipositioningapp

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast

class WifiReceiver(private var wifiManager: WifiManager, private val callback: ScanCallBack): BroadcastReceiver() {
    private lateinit var sb: StringBuilder

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent!!.action
        if (action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
//            sb = java.lang.StringBuilder()
            val scanResults: List<ScanResult> = wifiManager.scanResults
            callback.addScansCallback(scanResults)
        }
    }
}