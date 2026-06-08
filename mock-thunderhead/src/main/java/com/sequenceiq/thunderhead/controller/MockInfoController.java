package com.sequenceiq.thunderhead.controller;

import java.util.Map;

import jakarta.inject.Inject;

import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(MockInfoController.PATH)
public class MockInfoController {

    public static final String PATH = "mock-thunderhead";

    @Inject
    private InfoEndpoint infoEndpoint;

    @GetMapping("/info")
    public Map<String, Object> info() {
        return infoEndpoint.info();
    }
}
