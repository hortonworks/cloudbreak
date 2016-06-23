package com.sequenceiq.it.spark.docker.model

import com.google.gson.annotations.SerializedName

class InspectContainerResponse {

    @SerializedName("Id")
    private var id: String? = null
    @SerializedName("State")
    private var state: ContainerState? = null

    constructor() {
    }

    constructor(id: String) {
        this.id = id
        state = InspectContainerResponse.ContainerState()
        state!!.isRunning = true
    }

    fun setId(id: String) {
        this.id = id
    }

    fun setState(state: ContainerState) {
        this.state = state
    }

    class ContainerState {

        @SerializedName("Running")
        var isRunning: Boolean = false
    }

}
