package com.sequenceiq.cloudbreak.rotation.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.rotation.context.CMServiceRoleRestartRotationContext.CMServiceRoleRestartRotationContextBuilder;

@ExtendWith(MockitoExtension.class)
class CMServiceRoleRestartRotationContextTest {

    private static final String SERVICE_TYPE = "serviceType";

    private static final String ROLE_TYPE = "roleType";

    private static final String RESOURCE_CRN = "resourceCrn";

    @Test
    void testContextBuilder() {
        CMServiceRoleRestartRotationContextBuilder builder = CMServiceRoleRestartRotationContext.builder();
        builder.withResourceCrn(RESOURCE_CRN);
        builder.withServiceType(SERVICE_TYPE);
        builder.withRoleType(ROLE_TYPE);

        CMServiceRoleRestartRotationContext context = builder.build();
        assertEquals(RESOURCE_CRN, context.getResourceCrn());
        assertEquals(SERVICE_TYPE, context.getServiceType());
        assertEquals(ROLE_TYPE, context.getRoleType());
    }

    @Test
    void testContextBuilderForNullResourceCrn() {
        CMServiceRoleRestartRotationContextBuilder builder = CMServiceRoleRestartRotationContext.builder();
        builder.withServiceType(SERVICE_TYPE);
        builder.withRoleType(ROLE_TYPE);

        CloudbreakServiceException e = assertThrows(CloudbreakServiceException.class, () -> builder.build());
        assertEquals("Failed to build cm service role restart rotation context." +
                " [resourceCrn='null', serviceType='serviceType', roleType='roleType'", e.getMessage());
    }

    @Test
    void testContextBuilderForNullServiceName() {
        CMServiceRoleRestartRotationContextBuilder builder = CMServiceRoleRestartRotationContext.builder();
        builder.withResourceCrn(RESOURCE_CRN);
        builder.withRoleType(ROLE_TYPE);

        CloudbreakServiceException e = assertThrows(CloudbreakServiceException.class, () -> builder.build());
        assertEquals("Failed to build cm service role restart rotation context." +
                " [resourceCrn='resourceCrn', serviceType='null', roleType='roleType'", e.getMessage());
    }

    @Test
    void testContextBuilderForNullRoleType() {
        CMServiceRoleRestartRotationContextBuilder builder = CMServiceRoleRestartRotationContext.builder();
        builder.withResourceCrn(RESOURCE_CRN);
        builder.withServiceType(SERVICE_TYPE);

        CloudbreakServiceException e = assertThrows(CloudbreakServiceException.class, () -> builder.build());
        assertEquals("Failed to build cm service role restart rotation context." +
                " [resourceCrn='resourceCrn', serviceType='serviceType', roleType='null'", e.getMessage());
    }

}