package com.sequenceiq.mock.clouderamanager.base.batchapi;

import java.math.BigDecimal;

import org.springframework.http.HttpStatus;

import com.sequenceiq.mock.swagger.model.ApiBatchResponseElement;

public abstract class AbstractBatchApiHandler implements BatchApiHandler {

    protected ApiBatchResponseElement getSuccessApiBatchResponseElement(String response) {
        return new ApiBatchResponseElement()
                .statusCode(BigDecimal.valueOf(HttpStatus.OK.value()))
                .response(response);
    }

}
