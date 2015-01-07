package com.sequenceiq.cloudbreak.service;

public abstract class SimpleStatusCheckerTask<T> implements StatusCheckerTask<T> {

    public boolean exitPolling(T t) {
        return false;
    }

}
