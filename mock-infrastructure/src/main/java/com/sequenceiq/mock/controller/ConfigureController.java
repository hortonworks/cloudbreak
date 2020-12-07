package com.sequenceiq.mock.controller;

import javax.inject.Inject;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.sequenceiq.mock.service.ResponseModifierService;
import com.sequenceiq.mock.spi.MockResponse;

@RestController
//@RequestMapping()
public class ConfigureController {

    @Inject
    private ResponseModifierService responseModifierService;

    @PostMapping("/configure")
    public void configure(@RequestBody MockResponse mockResponse) {
        responseModifierService.addResponse(mockResponse);
    }
}
