package com.sequenceiq.environment.expressonboarding.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.environment.api.v1.expressonboarding.model.response.TenantInformationResponse;

class TenantInformationResponseConverterTest {

    private static final String USER_CRN = "crn:altus:iam:us-west-1:12345:user:" + UUID.randomUUID();

    @Test
    public void testTenantInformationResponse() {
        String expectedAccountId = "12345";
        TenantInformationResponseConverter converter = new TenantInformationResponseConverter();
        TenantInformationResponse response = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> converter.tenantInformationResponse());

        assertEquals(expectedAccountId, response.getTenantId());
    }
}