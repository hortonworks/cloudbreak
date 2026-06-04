package com.sequenceiq.thunderhead.controller;

import jakarta.inject.Inject;

import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sequenceiq.thunderhead.entity.MockInfoResponse;

@RestController
@RequestMapping(MockInfoController.PATH)
public class MockInfoController {

    public static final String PATH = "mock";

    @Inject
    private InfoEndpoint infoEndpoint;

    @GetMapping("/info")
    public MockInfoResponse info() {
        MockInfoResponse cloudbreakInfoResponse = new MockInfoResponse();
        cloudbreakInfoResponse.setInfo(infoEndpoint.info());
        return cloudbreakInfoResponse;
    }
}
