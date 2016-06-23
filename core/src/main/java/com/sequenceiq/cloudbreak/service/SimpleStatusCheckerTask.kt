package com.sequenceiq.cloudbreak.service

abstract class SimpleStatusCheckerTask<T> : StatusCheckerTask<T> {

    override fun handleException(e: Exception) {
        throw CloudbreakServiceException(e)
    }

}
