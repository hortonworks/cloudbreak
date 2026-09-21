package com.sequenceiq.notification.sender.dto;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CreateDistributionListRequestTest {

    private static final String PARENT_CRN = "crn:cdp:environments:us-west-1:acc123:environment:parent";

    private static final String TARGET_CRN = "crn:cdp:environments:us-west-1:acc123:environment:target";

    @Test
    void parentAndTargetIsTheSameWhenCrnsAreEqual() {
        CreateDistributionListRequest request = new CreateDistributionListRequest.Builder()
                .withParentResourceCrn(PARENT_CRN)
                .withTargetResourceCrn(PARENT_CRN)
                .build();
        assertTrue(request.parentAndTargetIsTheSame());
    }

    @Test
    void parentAndTargetIsTheSameWhenCrnsDiffer() {
        CreateDistributionListRequest request = new CreateDistributionListRequest.Builder()
                .withParentResourceCrn(PARENT_CRN)
                .withTargetResourceCrn(TARGET_CRN)
                .build();
        assertFalse(request.parentAndTargetIsTheSame());
    }

    @Test
    void parentAndTargetIsTheSameWhenBothNull() {
        CreateDistributionListRequest request = new CreateDistributionListRequest.Builder()
                .build();
        assertTrue(request.parentAndTargetIsTheSame());
    }
}
