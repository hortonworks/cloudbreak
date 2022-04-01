package com.sequenceiq.periscope.api;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.apiformat.ApiFormatValidator;

public class ApiFormatTest {

    @Test
    public void testApiFormat() {
        ApiFormatValidator.builder()
                .modelPackage("com.sequenceiq.periscope.api.model")
                .build()
                .validate();
    }

}