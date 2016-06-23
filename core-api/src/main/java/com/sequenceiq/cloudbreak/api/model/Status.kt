package com.sequenceiq.cloudbreak.api.model

import java.util.Arrays

enum class Status {
    REQUESTED,
    CREATE_IN_PROGRESS,
    AVAILABLE,
    UPDATE_IN_PROGRESS,
    UPDATE_REQUESTED,
    UPDATE_FAILED,
    CREATE_FAILED,
    ENABLE_SECURITY_FAILED,
    DELETE_IN_PROGRESS,
    DELETE_FAILED,
    DELETE_COMPLETED,
    STOPPED,
    STOP_REQUESTED,
    START_REQUESTED,
    STOP_IN_PROGRESS,
    START_IN_PROGRESS,
    START_FAILED,
    STOP_FAILED,
    WAIT_FOR_SYNC;

    fun normalizedStatusName(): String {
        return name.replace("_".toRegex(), " ").toLowerCase()
    }

    companion object {

        fun stopStatusesForUpdate(): List<Status> {
            return Arrays.asList(START_FAILED, START_IN_PROGRESS, START_REQUESTED)
        }

        fun availableStatusesForUpdate(): List<Status> {
            return Arrays.asList(REQUESTED, CREATE_IN_PROGRESS, UPDATE_IN_PROGRESS, UPDATE_REQUESTED,
                    UPDATE_FAILED, CREATE_FAILED, ENABLE_SECURITY_FAILED, STOP_REQUESTED, STOP_IN_PROGRESS, STOP_FAILED)
        }
    }
}
