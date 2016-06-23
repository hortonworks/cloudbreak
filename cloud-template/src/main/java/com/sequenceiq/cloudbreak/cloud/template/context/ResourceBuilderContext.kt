package com.sequenceiq.cloudbreak.cloud.template.context

import java.util.ArrayList
import java.util.HashMap
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue

import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.cloud.model.Location
import com.sequenceiq.cloudbreak.cloud.model.generic.DynamicModel

open class ResourceBuilderContext(val name: String, val location: Location, val parallelResourceRequest: Int) : DynamicModel() {
    private val networkResources = ConcurrentLinkedQueue<CloudResource>()
    private val computeResources = HashMap<Long, List<CloudResource>>()
    val isBuild: Boolean

    constructor(name: String, location: Location, parallelResourceRequest: Int, build: Boolean) : this(name, location, parallelResourceRequest) {
        this.isBuild = build
    }

    fun getNetworkResources(): List<CloudResource> {
        return ArrayList(networkResources)
    }

    fun addNetworkResources(resources: List<CloudResource>) {
        this.networkResources.addAll(resources)
    }

    @Synchronized fun addComputeResources(index: Long, resources: List<CloudResource>) {
        var list: MutableList<CloudResource>? = computeResources[index]
        if (list == null) {
            list = ArrayList<CloudResource>()
            computeResources.put(index, list)
        }
        list.addAll(resources)
    }

    fun getComputeResources(index: Long): List<CloudResource> {
        return computeResources[index]
    }

}
