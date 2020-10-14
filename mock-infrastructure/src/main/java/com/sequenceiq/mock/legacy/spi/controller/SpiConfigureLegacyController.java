package com.sequenceiq.mock.legacy.spi.controller;

import javax.inject.Inject;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sequenceiq.mock.legacy.response.MockResponse;
import com.sequenceiq.mock.legacy.service.ResponseModifierService;

@RestController
@RequestMapping("/spi")
public class SpiConfigureLegacyController {

    @Inject
    private ResponseModifierService responseModifierService;
//
//    @PostMapping("/register_public_key/configure")
//    public String registerPublicKey() {
//        return "";
//    }
//
//    @PostMapping("/unregister_public_key/configure")
//    public String unregisterPublicKey() {
//        return "";
//    }

    @PostMapping("/get_public_key/{publicKeyId}/configure")
    public void getPubicKeyId(@PathVariable("publicKeyId") String publicKeyId, @RequestBody MockResponse mockResponse) {
        responseModifierService.addResponse(mockResponse);
    }

}
