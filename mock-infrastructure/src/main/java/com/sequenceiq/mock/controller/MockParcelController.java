package com.sequenceiq.mock.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MockParcelController {

    @GetMapping("/mock-parcel/**")
    public ResponseEntity<String> getMockParcel() {
        return new ResponseEntity<>("", HttpStatus.OK);
    }

}
