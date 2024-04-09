package com.example.wifipositioningapp

import android.net.wifi.ScanResult

interface ScanCallBack {
    fun addScansCallback(results: List<ScanResult>)
}