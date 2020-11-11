package com.sequenceiq.mock.verification;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class TestRequestTrackerController {

    @Inject
    private RequestResponseStorageService requestResponseStorageService;

    @GetMapping("/calls/{testName}")
    public List<Call> getCalls(@PathVariable("testName") String testName) {
        return requestResponseStorageService.get(testName);
    }
}
