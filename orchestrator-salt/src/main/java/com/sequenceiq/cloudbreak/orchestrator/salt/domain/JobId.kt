package com.sequenceiq.cloudbreak.orchestrator.salt.domain

class JobId private constructor(val jobId: String) {

    override fun toString(): String {
        return "JobId{"
        +"jobId='" + jobId + '\''
        +'}'
    }

    companion object {

        fun jobId(id: String): JobId {
            return JobId(id)
        }
    }
}
