package com.sequenceiq.mock.legacy.freeipa;

import javax.inject.Inject;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class FreeipaHealthCheckController {

    @Inject
    private FreeIpaNodeHealthCheckHandler freeIpaNodeHealthCheckHandler;

    @RequestMapping("/freeipahealthcheck")
    public ResponseEntity<Object> healthCheck() {
        return freeIpaNodeHealthCheckHandler.handle("");
    }
}
