package com.sequenceiq.mock.controller;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sequenceiq.mock.clouderamanager.DefaultModelService;

@RestController
@RequestMapping("/tests")
public class TestRelatedController {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestRelatedController.class);

    @Inject
    private DefaultModelService defaultModelService;

    @GetMapping("/new")
    public void newTest() {
        LOGGER.info("Re-init test data");
//        defaultModelService.reinit();
    }
}
