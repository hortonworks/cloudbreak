package com.sequenceiq.cloudbreak.orchestrator.model

enum class RecipeExecutionPhase private constructor(private val value: String) {
    PRE("pre"), POST("post");

    fun value(): String {
        return value
    }

}
