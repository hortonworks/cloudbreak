package com.sequenceiq.mock.spi.controller;

import javax.inject.Inject;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sequenceiq.mock.service.ResponseModifierService;
import com.sequenceiq.mock.spi.MockResponse;

@RestController
@RequestMapping("/spi")
public class SpiResourceController {

    @Inject
    private ResponseModifierService responseModifierService;

    @PostMapping("/register_public_key")
    public void registerPublicKey() {
    }

    @PostMapping("/unregister_public_key")
    public void unregisterPublicKey(@RequestBody String body) {
    }

    @GetMapping("/get_public_key/{publicKeyId}")
    public Boolean getPubicKeyId(@PathVariable("publicKeyId") String publicKeyId) {
        MockResponse response = responseModifierService.getResponse("get", "/spi/get_public_key/{publicKeyId}");
        if (response == null) {
            return true;
        }
        return (Boolean) response.getResponse();
    }
}
