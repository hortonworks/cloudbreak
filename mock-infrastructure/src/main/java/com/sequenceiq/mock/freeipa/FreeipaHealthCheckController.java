package com.sequenceiq.mock.freeipa;

import javax.inject.Inject;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/{mock_uuid}")
public class FreeipaHealthCheckController {

    @Inject
    private FreeIpaNodeHealthCheckHandler freeIpaNodeHealthCheckHandler;

    @RequestMapping("/freeipahealthcheck")
    public ResponseEntity<Object> healthCheck(@PathVariable("mock_uuid") String mockUuid) {
        return freeIpaNodeHealthCheckHandler.handle("");
    }
}
