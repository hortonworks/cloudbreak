package com.sequenceiq.mock.clouderamanager.base.batchapi;

import org.springframework.http.HttpStatus;

import com.sequenceiq.mock.swagger.model.ApiBatchResponseElement;

public abstract class AbstractBatchApiHandler implements BatchApiHandler {

    protected ApiBatchResponseElement getSuccessApiBatchResponseElement(String response) {
        return new ApiBatchResponseElement()
                .statusCode(HttpStatus.OK.value())
                .response(response);
    }

}
