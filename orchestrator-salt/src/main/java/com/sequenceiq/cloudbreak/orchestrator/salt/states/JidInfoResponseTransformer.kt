package com.sequenceiq.cloudbreak.orchestrator.salt.states

import java.util.Collections
import java.util.HashMap

import com.sequenceiq.cloudbreak.orchestrator.salt.domain.RunnerInfoObject

object JidInfoResponseTransformer {

    fun getHighStates(map: Map<Any, Any>): Map<String, Map<String, RunnerInfoObject>> {
        val tmp = map
        val stringMapMap = tmp["return"].get(0).get("data")
        val result = HashMap<String, Map<String, RunnerInfoObject>>()

        for (stringMapEntry in stringMapMap.entries) {
            result.put(stringMapEntry.key, runnerInfoObjects(stringMapEntry.value))
        }

        return result
    }

    fun getSimpleStates(map: Map<Any, Any>): Map<String, Map<String, RunnerInfoObject>> {
        val tmp = map
        val stringMapMap = tmp["return"].get(0)

        val result = HashMap<String, Map<String, RunnerInfoObject>>()

        for (stringMapEntry in stringMapMap.entries) {
            result.put(stringMapEntry.key, runnerInfoObjects(stringMapEntry.value))
        }

        return result
    }

    private fun runnerInfoObjects(map: Map<String, Map<String, Any>>): Map<String, RunnerInfoObject> {
        val runnerInfoObjectMap = HashMap<String, RunnerInfoObject>()
        for (stringMapEntry in map.entries) {
            val value = stringMapEntry.value
            val runnerInfoObject = RunnerInfoObject()
            val changes = value["changes"]
            runnerInfoObject.changes = if (changes == null) emptyMap<String, Any>() else changes as Map<String, Any>?
            runnerInfoObject.comment = value["comment"].toString()
            runnerInfoObject.duration = value["duration"].toString()
            runnerInfoObject.name = value["name"].toString()
            runnerInfoObject.result = java.lang.Boolean.valueOf(value["result"].toString())
            val runNum = value["__run_num__"].toString()
            runnerInfoObject.runNum = if (runNum == null) -1 else Integer.parseInt(runNum)
            runnerInfoObject.startTime = value["start_time"].toString()
            runnerInfoObjectMap.put(stringMapEntry.key, runnerInfoObject)
        }
        return runnerInfoObjectMap
    }
}
