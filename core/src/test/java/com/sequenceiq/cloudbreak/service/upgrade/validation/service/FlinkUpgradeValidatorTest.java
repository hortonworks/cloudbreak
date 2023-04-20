package com.sequenceiq.cloudbreak.service.upgrade.validation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateService;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.exception.UpgradeValidationFailedException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;

@ExtendWith(MockitoExtension.class)
public class FlinkUpgradeValidatorTest {

    private static final String BLUEPRINT_TEXT = "{blueprint}";

    private static final String CLUSTER_NAME = "clusterName";

    @InjectMocks
    private FlinkUpgradeValidator underTest;

    private Stack stack;

    @Mock
    private CmTemplateService cmTemplateService;

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private ClusterApi clusterApi;

    @BeforeEach
    public void before() {
        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText(BLUEPRINT_TEXT);

        Cluster cluster = new Cluster();
        cluster.setName(CLUSTER_NAME);
        cluster.setBlueprint(blueprint);

        stack = new Stack();
        stack.setCluster(cluster);
    }

    @Test
    void validateNoRuntimeVersion() {
        ServiceUpgradeValidationRequest request =
                new ServiceUpgradeValidationRequest(stack, false, null);
        underTest.validate(request);
        verifyNoInteractions(cmTemplateService);
        verifyNoInteractions(clusterApiConnectors);
    }

    @Test
    void validateNotAffectedRuntime() {
        ServiceUpgradeValidationRequest request =
                new ServiceUpgradeValidationRequest(stack, false, CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_2_14.getVersion());
        underTest.validate(request);
        verifyNoInteractions(cmTemplateService);
        verifyNoInteractions(clusterApiConnectors);
    }

    @Test
    void validateNoFlinkSevice() {
        when(cmTemplateService.isServiceTypePresent("SQL_STREAM_BUILDER", BLUEPRINT_TEXT)).thenReturn(false);
        ServiceUpgradeValidationRequest request =
                new ServiceUpgradeValidationRequest(stack, false, CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_2_16.getVersion());
        underTest.validate(request);

        verifyNoInteractions(clusterApiConnectors);
    }

    @Test
    void validateNoRole() {
        when(cmTemplateService.isServiceTypePresent("SQL_STREAM_BUILDER", BLUEPRINT_TEXT)).thenReturn(true);
        when(clusterApiConnectors.getConnector(stack)).thenReturn(clusterApi);
        when(clusterApi.isRolePresent(CLUSTER_NAME, "STREAMING_SQL_CONSOLE", "SQL_STREAM_BUILDER")).thenReturn(false);
        ServiceUpgradeValidationRequest request =
                new ServiceUpgradeValidationRequest(stack, false, CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_2_16.getVersion());
        underTest.validate(request);
    }

    @Test
    void validateApiException() {
        when(cmTemplateService.isServiceTypePresent("SQL_STREAM_BUILDER", BLUEPRINT_TEXT)).thenReturn(true);
        when(clusterApiConnectors.getConnector(stack)).thenReturn(clusterApi);
        when(clusterApi.isRolePresent(CLUSTER_NAME, "STREAMING_SQL_CONSOLE", "SQL_STREAM_BUILDER"))
                .thenThrow(new CloudbreakServiceException("Api exception"));

        ServiceUpgradeValidationRequest request =
                new ServiceUpgradeValidationRequest(stack, false, CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_2_16.getVersion());

        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class, () -> underTest.validate(request));

        assertEquals("Api exception", exception.getMessage());
    }

    @Test
    void validateRoleExistsValidationFails() {
        when(cmTemplateService.isServiceTypePresent("SQL_STREAM_BUILDER", BLUEPRINT_TEXT)).thenReturn(true);
        when(clusterApiConnectors.getConnector(stack)).thenReturn(clusterApi);
        when(clusterApi.isRolePresent(CLUSTER_NAME, "STREAMING_SQL_CONSOLE", "SQL_STREAM_BUILDER")).thenReturn(true);

        ServiceUpgradeValidationRequest request =
                new ServiceUpgradeValidationRequest(stack, false, CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_2_16.getVersion());

        UpgradeValidationFailedException exception = assertThrows(UpgradeValidationFailedException.class, () -> underTest.validate(request));

        String expected =
                "Version change to CDH 7.2.16 would lose access to role STREAMING_SQL_CONSOLE. To continue, remove this role and any dependent services.";
        assertEquals(expected, exception.getMessage());
    }
}