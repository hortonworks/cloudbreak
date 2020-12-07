package com.sequenceiq.mock.spi.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/spi")
public class SpiResourceController {

    private Map<String, PublicKey> publicKeys = new HashMap<>();

    @PostMapping("/register_public_key")
    public void registerPublicKey(@RequestBody PublicKey publicKey) {
        publicKeys.put(publicKey.getPublicKeyId(), publicKey);
    }

    @PostMapping("/unregister_public_key")
    public void unregisterPublicKey(@RequestBody PublicKey publicKey) {
        publicKeys.remove(publicKey.getPublicKeyId());
    }

    @GetMapping("/get_public_key/{publicKeyId}")
    public PublicKey getPubicKeyId(@PathVariable("publicKeyId") String publicKeyId) {
        return publicKeys.get(publicKeyId);
    }
}
