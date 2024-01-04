package com.sequenceiq.mock.salt.controller;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sequenceiq.mock.salt.SaltApiRunComponent;

@RestController
@RequestMapping("/{mock_uuid}/saltapi/")
public class SaltApiController {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltApiController.class);

    @Inject
    private SaltApiRunComponent saltApiRunComponent;

    @PostMapping(value = "run", produces = MediaType.APPLICATION_JSON)
    public Object saltRun(@PathVariable("mock_uuid") String mockUuid, @RequestBody String body) throws Exception {
        Object saltApiResponse = saltApiRunComponent.createSaltApiResponse(mockUuid, body);
        LOGGER.trace("{} body with result: {}", body, saltApiResponse);
        return saltApiResponse;
    }

    @PostMapping(value = "run/{run_arg}/failure", produces = MediaType.APPLICATION_JSON)
    public void setRunFailure(@PathVariable("mock_uuid") String mockUuid, @PathVariable("run_arg") String runArg) {
        saltApiRunComponent.setFailure(mockUuid, runArg);
        LOGGER.trace("Set failure for {} when arg is {}", mockUuid, runArg);
    }

    @DeleteMapping(value = "run/{run_arg}/failure", produces = MediaType.APPLICATION_JSON)
    public void deleteRunFailure(@PathVariable("mock_uuid") String mockUuid, @PathVariable("run_arg") String runArg) {
        saltApiRunComponent.deleteFailure(mockUuid, runArg);
        LOGGER.trace("Removed failure for {} when arg is {}", mockUuid, runArg);
    }
}
