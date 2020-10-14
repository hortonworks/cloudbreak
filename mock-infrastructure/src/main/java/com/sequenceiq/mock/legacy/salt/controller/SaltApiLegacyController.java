package com.sequenceiq.mock.legacy.salt.controller;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sequenceiq.mock.legacy.salt.SaltApiRunComponent;

@RestController
@RequestMapping("/saltapi/")
public class SaltApiLegacyController {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltApiLegacyController.class);

    @Inject
    private SaltApiRunComponent saltApiRunComponent;

    @PostMapping(value = "run", produces = MediaType.APPLICATION_JSON)
    public Object saltRun(@RequestBody String body) throws Exception {
        Object saltApiResponse = saltApiRunComponent.createSaltApiResponse(body);
        LOGGER.debug("{} body with result: {}", body, saltApiResponse);
        return saltApiResponse;
    }
}
