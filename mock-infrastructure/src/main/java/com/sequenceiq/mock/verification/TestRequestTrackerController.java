package com.sequenceiq.mock.verification;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class TestRequestTrackerController {

    @Inject
    private RequestResponseStorageService requestResponseStorageService;

    @GetMapping("/calls/{mockUuid}")
    public List<Call> getCalls(@PathVariable("mockUuid") String mockUuid) {
        return requestResponseStorageService.get(mockUuid);
    }

    @PostMapping("/calls/{mockUuid}/disable")
    public void disableCallStorage(@PathVariable("mockUuid") String mockUuid) {
        requestResponseStorageService.disableCallStorage(mockUuid);
    }

    @PostMapping("/calls/{mockUuid}/enable")
    public void enableCallStorage(@PathVariable("mockUuid") String mockUuid) {
        requestResponseStorageService.enableCallStorage(mockUuid);
    }
}
