package com.sequenceiq.cloudbreak.orchestrator.model

import java.util.HashMap

class RecipeModel(var name: String?) {
    var preInstall: String? = null
    var postInstall: String? = null
    var keyValues: Map<String, String> = HashMap()

    override fun toString(): String {
        val sb = StringBuilder("RecipeModel{")
        sb.append(", name='").append(name).append('\'')
        sb.append(", preInstall=").append(preInstall)
        sb.append(", postInstall=").append(postInstall)
        sb.append(", keyValues=").append(keyValues)
        sb.append('}')
        return sb.toString()
    }
}
