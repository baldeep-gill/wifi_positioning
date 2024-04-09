package com.example.wifipositioningapp

import android.content.Context
import android.database.sqlite.SQLiteDatabase
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        wifiManager = requireContext().getSystemService(Context.WIFI_SERVICE) as WifiManager
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_positioning, container, false)

        positioningText = view.findViewById(R.id.positioning_text)
        positioningButton = view.findViewById(R.id.positioning_button)

        return view
    }

    private fun startScan() {
        positioningButton.setOnClickListener {
            val values = dbHelper.getAllScanResults(database)

            println(values)
        }
    }
}