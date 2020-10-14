package com.sequenceiq.mock.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UnmappedController {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnmappedController.class);

    @GetMapping("/")
    public void getRoot() {
        LOGGER.warn("Add content to GET /");
    }
}
