package com.sequenceiq.flow.core;

import org.apache.commons.lang3.NotImplementedException;

import com.sequenceiq.cloudbreak.common.event.PayloadContext;

public interface PayloadContextProvider {

    default PayloadContext getPayloadContext(Long resourceId) {
        throw new NotImplementedException("You have to implement getPayloadContext for your resource to be able "
                + "to use Flow API to get the payload context!");
    }

}
