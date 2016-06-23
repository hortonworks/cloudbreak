package com.sequenceiq.cloudbreak.cloud.task


import javax.inject.Inject

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext

abstract class PollBooleanStateTask
@Inject
constructor(authenticatedContext: AuthenticatedContext, cancellable: Boolean) : AbstractPollTask<Boolean>(authenticatedContext, cancellable) {

    override fun completed(aBoolean: Boolean?): Boolean {
        return aBoolean!!
    }
}
