package com.sequenceiq.mock.spi.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/db")
public class DbResourceController {
    public static final String CERTIFICATE_1 = "certificate1";

    public static final String CERTIFICATE_2 = "certificate2";

    @GetMapping("certificates")
    public String[] getCertificates() {
        return new String[] {CERTIFICATE_1, CERTIFICATE_2};
    }
}
