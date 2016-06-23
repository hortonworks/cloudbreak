package com.sequenceiq.cloudbreak.service

enum class PollingResult {
    TIMEOUT, EXIT, SUCCESS, FAILURE;


    companion object {

        fun isSuccess(pollingResult: PollingResult): Boolean {
            return PollingResult.SUCCESS == pollingResult
        }

        fun isExited(pollingResult: PollingResult): Boolean {
            return PollingResult.EXIT == pollingResult
        }

        fun isTimeout(pollingResult: PollingResult): Boolean {
            return PollingResult.TIMEOUT == pollingResult
        }

        fun isFailure(pollingResult: PollingResult): Boolean {
            return PollingResult.FAILURE == pollingResult
        }
    }

}