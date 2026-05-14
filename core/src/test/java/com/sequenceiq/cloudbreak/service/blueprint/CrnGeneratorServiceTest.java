package com.sequenceiq.cloudbreak.service.blueprint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;

@ExtendWith(MockitoExtension.class)
class CrnGeneratorServiceTest {

    private static final String ACCOUNT_ID = "test-account-id";

    private static final String CRN_RESULT = "crn";

    @Mock
    private RegionAwareCrnGenerator regionAwareCrnGenerator;

    @InjectMocks
    private CrnGeneratorService underTest;

    @Test
    void testCreateBlueprintCrn() {
        when(regionAwareCrnGenerator.generateCrnStringWithUuid(CrnResourceDescriptor.CLUSTER_TEMPLATE, ACCOUNT_ID))
                .thenReturn(CRN_RESULT);

        String result = underTest.createBlueprintCrn(ACCOUNT_ID);

        assertEquals(CRN_RESULT, result);
        verify(regionAwareCrnGenerator).generateCrnStringWithUuid(CrnResourceDescriptor.CLUSTER_TEMPLATE, ACCOUNT_ID);
    }

    @Test
    void testCreateGlobalDefaultBlueprintCrn() {
        String blueprintName = "My Fancy Blueprint-Name! 123";
        String expectedResourceId = "myfancyblueprintname123";
        when(regionAwareCrnGenerator.generateCrnString(CrnResourceDescriptor.CLUSTER_TEMPLATE, expectedResourceId, "cloudera_default"))
                .thenReturn(CRN_RESULT);

        String result = underTest.createGlobalDefaultBlueprintCrn(blueprintName);

        assertEquals(CRN_RESULT, result);
        verify(regionAwareCrnGenerator).generateCrnString(CrnResourceDescriptor.CLUSTER_TEMPLATE, expectedResourceId, "cloudera_default");
    }

    @Test
    void testCreateGlobalDefaultClusterDefinitionCrn() {
        String definitionName = "My Cluster_Def! 456";
        String expectedResourceId = "myclusterdef456";
        when(regionAwareCrnGenerator.generateCrnString(CrnResourceDescriptor.CLUSTER_DEF, expectedResourceId, "cloudera_default"))
                .thenReturn(CRN_RESULT);

        String result = underTest.createGlobalDefaultClusterDefinitionCrn(definitionName);

        assertEquals(CRN_RESULT, result);
        verify(regionAwareCrnGenerator).generateCrnString(CrnResourceDescriptor.CLUSTER_DEF, expectedResourceId, "cloudera_default");
    }
}