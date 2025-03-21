package com.nefyra.exo

import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonProperty

class VideoConfig {
    var initialValue: InitialValue? = null
    val episodes: MutableMap<Int, String> = HashMap()

    class InitialValue {
        @JsonProperty("Ep")
        var ep: Int = 0
        var time: Long = 0
    }

    @JsonAnySetter
    fun setEpisode(key: String, value: String) {
        try {
            val ep = key.toInt()
            episodes[ep] = value
        } catch (ignored: NumberFormatException) {
        }
    }
}