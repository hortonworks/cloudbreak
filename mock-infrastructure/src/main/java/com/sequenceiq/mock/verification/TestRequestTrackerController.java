package com.sequenceiq.mock.verification;

import jakarta.inject.Inject;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class TestRequestTrackerController {

    @Inject
    private RequestResponseStorageService requestResponseStorageService;

    @PostMapping("/calls/disable")
    public void disableCallStorage(@RequestParam("mockUuid") String mockUuid) {
        requestResponseStorageService.disableCallStorage(mockUuid);
    }

    @PostMapping("/calls/enable")
    public void enableCallStorage(@RequestParam("mockUuid") String mockUuid) {
        requestResponseStorageService.enableCallStorage(mockUuid);
    }

    @PostMapping("/calls/reset")
    public void resetCallStorage(@RequestParam("mockUuid") String mockUuid) {
        requestResponseStorageService.resetCallStorage(mockUuid);
    }
}
