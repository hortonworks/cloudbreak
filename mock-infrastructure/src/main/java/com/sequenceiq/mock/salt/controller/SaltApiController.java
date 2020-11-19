package com.sequenceiq.mock.salt.controller;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
        LOGGER.debug("{} body with result: {}", body, saltApiResponse);
        return saltApiResponse;
    }
}
