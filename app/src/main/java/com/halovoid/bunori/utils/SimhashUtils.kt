package com.halovoid.bunori.utils

import java.security.MessageDigest

object SimhashUtils {
    
    /**
     * Generates a 64-bit simhash for the given text.
     * Uses 3-grams for tokenization and the first 8 bytes of MD5 for feature hashing.
     */
    fun generateSimhash(text: String): Long {
        val normalized = normalize(text)
        if (normalized.isBlank()) return 0L
        
        val tokens = tokenize(normalized)
        val v = IntArray(64)
        
        val md = MessageDigest.getInstance("MD5")
        
        for (token in tokens) {
            val digest = md.digest(token.toByteArray())
            var hash = 0L
            for (j in 0 until 8) {
                hash = (hash shl 8) or (digest[j].toLong() and 0xFFL)
            }
            
            for (i in 0 until 64) {
                val bit = (hash ushr i) and 1L
                if (bit == 1L) {
                    v[i]++
                } else {
                    v[i]--
                }
            }
        }
        
        var simhash = 0L
        for (i in 0 until 64) {
            if (v[i] > 0) {
                simhash = simhash or (1L shl i)
            }
        }
        return simhash
    }

    /**
     * Calculates the Hamming distance between two 64-bit hashes.
     */
    fun hammingDistance(h1: Long, h2: Long): Int {
        var x = h1 xor h2
        var distance = 0
        while (x != 0L) {
            x = x and (x - 1)
            distance++
        }
        return distance
    }

    private fun normalize(text: String): String {
        return text.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun tokenize(text: String): List<String> {
        if (text.length < 3) return listOf(text)
        return text.windowed(3)
    }
}
