package com.sequenceiq.thunderhead.controller;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.json.Json;

@RestController
public class MockThunderheadComputeApiController {
    private static final Logger LOGGER = LoggerFactory.getLogger(MockThunderheadComputeApiController.class);

    @PostMapping("api/v1/compute/describeCustomConfig")
    public ResponseEntity<String> describeCustomConfig(@RequestBody String requestEntity) {
        LOGGER.info("Describe custom config request has arrived with body: '{}'", requestEntity);
        try {
            Json requestJson = new Json(requestEntity);
            String crnFromRequest = requestJson.getMap().getOrDefault("crn", "dummyCRN").toString();
            Map<String, String> responseMap = new HashMap<>();
            responseMap.put("name", "mockedCustomConfig");
            responseMap.put("secretPath", "thunderhead-dockerconfig/shared/dummyPath");
            if (crnFromRequest.contains("wrong-crn")) {
                throw new BadRequestException("The actual Docker Registry Config not found");
            } else {
                responseMap.put("crn", crnFromRequest);
            }
            return new ResponseEntity<>(new Json(responseMap).getValue(), HttpStatus.OK);
        } catch (Exception ex) {
            String msg = "UH-OH something went wrong!";
            LOGGER.warn(msg, ex);
            return new ResponseEntity(msg, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
