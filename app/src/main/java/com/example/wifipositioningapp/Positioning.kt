package com.example.wifipositioningapp

import kotlin.math.pow

class Positioning() {
    private var offlineData: HashMap<Int, HashMap<String, Int>> = HashMap()
    private var _K = 3
    fun updateOfflineData(data: HashMap<Int, HashMap<String, Int>>) {
        offlineData = data
    }

    fun setK(k: Int) {
        this._K = k
    }

    fun calculatePosition(unseen: HashMap<String, Int>, measure: Measures = Measures.EUCLIDEAN, references: HashMap<Int, Pair<Int, Int>>, weighted: Boolean = false): Pair<Float, Float> {
        val distances = HashMap<Int, Float>()

        for ((rp, scan) in offlineData) {
            val distance = measure.calculate_distance(scan, unseen)
            distances[rp] = distance
        }

        // Map -> to list of pairs -> sorted by second element -> take first K elements
        val sorted = distances.toList().sortedBy { it.second }.take(_K)

        if (weighted) {
            val weightSum = sorted.map { it.second }.map { it.pow(-1) }.sum()

            var xTotal = 0f
            var yTotal = 0f

            for ((id, dist) in sorted) {
                val weight = dist.pow(-1)
                xTotal += weight * references[id]!!.first
                yTotal += weight * references[id]!!.second
            }

            return Pair(xTotal / weightSum, yTotal / weightSum)
        }

        // Extract rp ids from pairs and map it to the actual coordinates from references
        val ids = sorted.map { it.first }.map { references[it]!! }
        var totalX = 0f
        var totalY = 0f
        for ((x, y) in ids) {
            totalX += x
            totalY += y
        }

        return Pair((totalX / ids.size), (totalY / ids.size))
    }
}
