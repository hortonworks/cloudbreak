package com.sequenceiq.mock.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MockParcelController {

    @GetMapping("/mock-parcel/**")
    public ResponseEntity<String> getMockParcel() {
        return new ResponseEntity<>("", HttpStatus.OK);
    }

    @GetMapping("/mock-parcel/someParcel/manifest.json")
    public ResponseEntity<String> getMockSomeParcel() throws IOException {
        ClassPathResource res = new ClassPathResource("mock-image-catalogs/manifest.json");
        String json = StreamUtils.copyToString(res.getInputStream(), StandardCharsets.UTF_8);
        return ResponseEntity.ok(json);
    }
}
