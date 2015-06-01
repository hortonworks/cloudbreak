package com.sequenceiq.cloudbreak.cloud.task;

public interface PollTask<T> extends FetchTask<T>, CheckResult<T> {

}
