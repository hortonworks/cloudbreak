package com.sequenceiq.cloudbreak.service.upgrade.validation.service;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_2_14;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_2_16;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateService;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.exception.UpgradeValidationFailedException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.upgrade.UpgradeImageInfo;

@ExtendWith(MockitoExtension.class)
public class FlinkUpgradeValidatorTest {

    private static final String BLUEPRINT_TEXT = "{blueprint}";

    private static final String CLUSTER_NAME = "clusterName";

    @InjectMocks
    private FlinkUpgradeValidator underTest;

    @Mock
    private StackDto stack;

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
        lenient().when(stack.getBlueprintJsonText()).thenReturn(BLUEPRINT_TEXT);
        lenient().when(stack.getCluster()).thenReturn(cluster);
    }

    @Test
    void validateNoRuntimeVersion() {
        underTest.validate(createRequest(null));
        verifyNoInteractions(cmTemplateService);
        verifyNoInteractions(clusterApiConnectors);
    }

    @Test
    void validateNotAffectedRuntime() {
        underTest.validate(createRequest(CLOUDERA_STACK_VERSION_7_2_14.getVersion()));
        verifyNoInteractions(cmTemplateService);
        verifyNoInteractions(clusterApiConnectors);
    }

    @Test
    void validateNoFlinkService() {
        when(cmTemplateService.isServiceTypePresent("SQL_STREAM_BUILDER", BLUEPRINT_TEXT)).thenReturn(false);
        underTest.validate(createRequest(CLOUDERA_STACK_VERSION_7_2_16.getVersion()));

        verifyNoInteractions(clusterApiConnectors);
    }

    @Test
    void validateNoRole() {
        when(cmTemplateService.isServiceTypePresent("SQL_STREAM_BUILDER", BLUEPRINT_TEXT)).thenReturn(true);
        when(clusterApiConnectors.getConnector(stack)).thenReturn(clusterApi);
        when(clusterApi.isRolePresent(CLUSTER_NAME, "STREAMING_SQL_CONSOLE", "SQL_STREAM_BUILDER")).thenReturn(false);
        underTest.validate(createRequest(CLOUDERA_STACK_VERSION_7_2_16.getVersion()));
    }

    @Test
    void validateApiException() {
        when(cmTemplateService.isServiceTypePresent("SQL_STREAM_BUILDER", BLUEPRINT_TEXT)).thenReturn(true);
        when(clusterApiConnectors.getConnector(stack)).thenReturn(clusterApi);
        when(clusterApi.isRolePresent(CLUSTER_NAME, "STREAMING_SQL_CONSOLE", "SQL_STREAM_BUILDER"))
                .thenThrow(new CloudbreakServiceException("Api exception"));

        ServiceUpgradeValidationRequest request = createRequest(CLOUDERA_STACK_VERSION_7_2_16.getVersion());

        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class, () -> underTest.validate(request));

        assertEquals("Api exception", exception.getMessage());
    }

    @Test
    void validateRoleExistsValidationFails() {
        when(cmTemplateService.isServiceTypePresent("SQL_STREAM_BUILDER", BLUEPRINT_TEXT)).thenReturn(true);
        when(clusterApiConnectors.getConnector(stack)).thenReturn(clusterApi);
        when(clusterApi.isRolePresent(CLUSTER_NAME, "STREAMING_SQL_CONSOLE", "SQL_STREAM_BUILDER")).thenReturn(true);

        ServiceUpgradeValidationRequest request = createRequest(CLOUDERA_STACK_VERSION_7_2_16.getVersion());

        UpgradeValidationFailedException exception = assertThrows(UpgradeValidationFailedException.class, () -> underTest.validate(request));

        String expected =
                "Version change to CDH 7.2.16 would lose access to role STREAMING_SQL_CONSOLE. To continue, remove this role and any dependent services.";
        assertEquals(expected, exception.getMessage());
    }

    private ServiceUpgradeValidationRequest createRequest(String stackVersion) {
        return new ServiceUpgradeValidationRequest(stack, false, true,
                new UpgradeImageInfo(null, StatedImage.statedImage(Image.builder().withVersion(stackVersion).build(), null, null)), false);
    }
}