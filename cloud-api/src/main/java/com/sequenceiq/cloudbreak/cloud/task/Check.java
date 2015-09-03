package com.sequenceiq.cloudbreak.cloud.task;

public interface Check<T> {

    boolean completed(T t);

    boolean cancelled();

}
