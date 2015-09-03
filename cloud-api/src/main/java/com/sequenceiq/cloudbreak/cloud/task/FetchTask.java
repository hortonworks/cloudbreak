package com.sequenceiq.cloudbreak.cloud.task;

import java.util.concurrent.Callable;

import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;

public interface FetchTask<T> extends Callable<T> {

    AuthenticatedContext getAuthenticatedContext();

}
