package com.sequenceiq.cloudbreak.service.template;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.responses.ClusterTemplateViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;

class ClusterTemplateServiceFilterTest {

    private ClusterTemplateService underTest;

    @BeforeEach
    void setUp() {
        underTest = new ClusterTemplateService();
    }

    @Test
    void testIfGettingUsableTemplateWhenTemplateIsDefaultThenTrueShouldCome() {
        ClusterTemplateViewV4Response templateViewV4Response = new ClusterTemplateViewV4Response();
        templateViewV4Response.setStatus(ResourceStatus.DEFAULT);

        boolean result = underTest.isUsableClusterTemplate(templateViewV4Response);

        assertTrue(result);
    }

    @Test
    void testIfGettingUsableTemplateWhenTemplateIsUserManagedAndHasEnvironmentNameInItThenTrueShouldCome() {
        ClusterTemplateViewV4Response templateViewV4Response = new ClusterTemplateViewV4Response();
        templateViewV4Response.setStatus(ResourceStatus.USER_MANAGED);
        templateViewV4Response.setEnvironmentName("SomeEnvironmentName");

        boolean result = underTest.isUsableClusterTemplate(templateViewV4Response);

        assertTrue(result);
    }

    @Test
    void testIfGettingUsableTemplateWhenTemplateIsUserManagedButHasNullEnvironmentNameThenFalseShouldCome() {
        ClusterTemplateViewV4Response templateViewV4Response = new ClusterTemplateViewV4Response();
        templateViewV4Response.setStatus(ResourceStatus.USER_MANAGED);
        templateViewV4Response.setEnvironmentName(null);

        boolean result = underTest.isUsableClusterTemplate(templateViewV4Response);

        assertFalse(result);
    }

}