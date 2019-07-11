package com.sequenceiq.cloudbreak.polling;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;

public abstract class SimpleStatusCheckerTask<T> implements StatusCheckerTask<T> {

    @Override
    public void handleException(Exception e) {
        throw new CloudbreakServiceException(e);
    }

}
