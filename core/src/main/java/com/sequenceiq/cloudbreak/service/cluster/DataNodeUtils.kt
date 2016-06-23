package com.sequenceiq.cloudbreak.service.cluster

import java.util.Collections.reverseOrder

import java.util.ArrayList
import java.util.LinkedHashMap
import java.util.TreeMap

class DataNodeUtils private constructor() {

    init {
        throw IllegalStateException()
    }

    companion object {

        fun sortByUsedSpace(dataNodes: Map<String, Map<Long, Long>>, reverse: Boolean): Map<String, Long> {
            val sorted = sort(dataNodes, reverse)
            val result = LinkedHashMap<String, Long>()
            for (space in sorted.keys) {
                val hosts = sorted[space]
                for (host in hosts) {
                    result.put(host, space)
                }
            }
            return result
        }

        private fun sort(dataNodes: Map<String, Map<Long, Long>>, reverse: Boolean): Map<Long, List<String>> {
            val result = getSortedMap(reverse)
            for (hostName in dataNodes.keys) {
                val usage = dataNodes[hostName]
                val space = usage.values.iterator().next()
                var hosts: MutableList<String>? = result[space]
                if (hosts == null) {
                    hosts = ArrayList<String>()
                }
                hosts.add(hostName)
                result.put(space, hosts)
            }
            return result
        }

        private fun getSortedMap(reverse: Boolean): MutableMap<Long, List<String>> {
            if (reverse) {
                return TreeMap(reverseOrder<Long>())
            } else {
                return TreeMap()
            }
        }
    }
}
