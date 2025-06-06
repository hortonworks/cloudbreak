package com.sequenceiq.mock.clouderamanager.v45.controller;

import jakarta.inject.Inject;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import com.sequenceiq.mock.clouderamanager.base.BatchResourceOperation;
import com.sequenceiq.mock.swagger.model.ApiBatchRequest;
import com.sequenceiq.mock.swagger.model.ApiBatchResponse;
import com.sequenceiq.mock.swagger.v45.api.BatchResourceApi;

@Controller
public class BatchResourceV45Controller implements BatchResourceApi {

    @Inject
    private BatchResourceOperation batchResourceOperation;

    @Override
    public ResponseEntity<ApiBatchResponse> execute(String mockUuid, ApiBatchRequest body) {
        return batchResourceOperation.execute(mockUuid, body);
    }

}
