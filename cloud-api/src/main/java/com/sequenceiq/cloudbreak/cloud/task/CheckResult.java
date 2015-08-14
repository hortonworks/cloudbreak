package com.sequenceiq.cloudbreak.cloud.task;

public interface CheckResult<T> {

    boolean completed(T t);

}
