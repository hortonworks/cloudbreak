package com.sequenceiq.cloudbreak.cloud.task

import java.util.concurrent.Callable

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext

interface FetchTask<T> : Callable<T> {

    val authenticatedContext: AuthenticatedContext

}
