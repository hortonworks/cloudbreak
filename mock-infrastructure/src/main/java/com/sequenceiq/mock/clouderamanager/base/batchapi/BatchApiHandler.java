package com.sequenceiq.mock.clouderamanager.base.batchapi;

import com.sequenceiq.mock.swagger.model.ApiBatchRequestElement;
import com.sequenceiq.mock.swagger.model.ApiBatchResponseElement;

public interface BatchApiHandler {

    default String getDescription() {
        return "<Description missing>";
    }

    boolean canProcess(ApiBatchRequestElement apiBatchRequestElement);

    ApiBatchResponseElement process(String mockUuid, ApiBatchRequestElement apiBatchRequestElement);

}
