package com.sequenceiq.cloudbreak.cloud.event.context;

import com.sequenceiq.cloudbreak.cloud.task.FetchTask;

public class PollingContext<T> {

    private FetchTask<T> statusCheckTask;

    public PollingContext(FetchTask<T> statusCheckTask) {
        this.statusCheckTask = statusCheckTask;
    }

    public FetchTask<T> getStatusCheckTask() {
        return statusCheckTask;
    }
}
