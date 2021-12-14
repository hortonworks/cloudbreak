package com.sequenceiq.mock.controller;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sequenceiq.mock.verification.Call;
import com.sequenceiq.mock.verification.RequestResponseStorageService;

@RestController
@RequestMapping("/tests")
public class TestRelatedController {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestRelatedController.class);

    @Inject
    private RequestResponseStorageService requestResponseStorageService;

    @GetMapping("/new")
    public void newTest() {
        LOGGER.info("Re-init test data");
//        defaultModelService.reinit();
    }

    @GetMapping("/calls/{mockUuid}")
    public List<Call> getCalls(@PathVariable String mockUuid, @RequestParam("path") String path) {
        return requestResponseStorageService.get(mockUuid, path);
    }

    @GetMapping("/calls")
    public List<Call> getCallsByPath(@RequestParam("path") String path) {
        return requestResponseStorageService.get(path, path);
    }
}
