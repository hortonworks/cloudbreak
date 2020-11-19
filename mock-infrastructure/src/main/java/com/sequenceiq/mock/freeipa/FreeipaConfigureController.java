package com.sequenceiq.mock.freeipa;

import javax.inject.Inject;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/ipa/")
public class FreeipaConfigureController {

    @Inject
    private FreeIpaNodeHealthCheckHandler freeIpaNodeHealthCheckHandler;

    @GetMapping("status/configure")
    public void statusConfigure(@RequestParam("status") HttpStatus status) {
        if (status == HttpStatus.OK) {
            freeIpaNodeHealthCheckHandler.setHealthy();
        } else if (status == HttpStatus.SERVICE_UNAVAILABLE) {
            freeIpaNodeHealthCheckHandler.setUnreachable();
        }
    }

}
