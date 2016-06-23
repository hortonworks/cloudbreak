package com.sequenceiq.cloudbreak.cloud.gcp.model

import java.util.HashMap

class MachineDefinitionView(map: Map<Any, Any>) {

    private val map = HashMap()

    init {
        this.map = map
    }

    private fun getParameter(key: String): String {
        return if (map.get(key) == null) "" else map.get(key).toString()
    }

    val kind: String
        get() = getParameter("kind")

    val id: String
        get() = getParameter("id")

    val creationTimestamp: String
        get() = getParameter("creationTimestamp")

    val name: String
        get() = getParameter("name")

    val description: String
        get() = getParameter("description")

    val guestCpus: String
        get() = getParameter("guestCpus")

    val memoryMb: String
        get() = getParameter("memoryMb")

    val maximumPersistentDisks: String
        get() = getParameter("maximumPersistentDisks")

    val maximumNumberWithLimit: Int?
        get() {
            val maxNumber = Integer.valueOf(maximumPersistentDisks)!!
            return if (maxNumber > LIMIT) LIMIT else maxNumber
        }

    val maximumPersistentDisksSizeGb: String
        get() = getParameter("maximumPersistentDisksSizeGb")

    val zone: String
        get() = getParameter("zone")

    val selfLink: String
        get() = getParameter("selfLink")

    companion object {

        private val LIMIT = 24
    }
}
