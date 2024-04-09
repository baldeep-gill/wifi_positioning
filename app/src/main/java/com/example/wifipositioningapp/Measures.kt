package com.example.wifipositioningapp

import kotlin.math.abs
import kotlin.math.pow
enum class Measures {
    EUCLIDEAN {
        override fun calculate_distance(
            offlineData: HashMap<String, Int>,
            unseen: HashMap<String, Int>
        ): Float {
            var temp = 0f
            for ((address, level) in unseen) {
                // Get the rssi for the corresponding AP in offline data or continue if does not exist
                val offline_level = offlineData[address] ?: continue
                temp += (offline_level - level).toFloat().pow(2)
            }

            return temp.pow(0.5f)
        }
    },
    MANHATTAN {
        override fun calculate_distance(
            offlineData: HashMap<String, Int>,
            unseen: HashMap<String, Int>
        ): Float {
            var temp = 0f
            for ((address, level) in unseen) {
                val offline_level = offlineData[address] ?: continue
                temp += abs(offline_level - level)
            }

            return temp
        }
    },
    CHISQUARED {
        override fun calculate_distance(
            offlineData: HashMap<String, Int>,
            unseen: HashMap<String, Int>
        ): Float {
            var temp = 0f
            for ((address, level) in unseen) {
                val offline_level = offlineData[address] ?: continue
                temp += ((offline_level - level).toFloat().pow(2)) / (abs(offline_level + level))
            }

            return temp
        }
    };

    abstract fun calculate_distance(offlineData: HashMap<String, Int>, unseen: HashMap<String, Int>): Float

    companion object {
        fun getMeasureByName(name: String) = valueOf(name.uppercase())
    }
}