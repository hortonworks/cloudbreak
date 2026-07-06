package com.sequenceiq.cloudbreak.service.stack;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.sharedservice.SharedServiceV4Request;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.sdx.TargetPlatform;
import com.sequenceiq.cloudbreak.sdx.common.PlatformAwareSdxConnector;
import com.sequenceiq.cloudbreak.sdx.common.model.SdxBasicView;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

@ExtendWith(MockitoExtension.class)
class SharedServiceValidatorTest {

    private static final String DATALAKE_NAME = "datalake";

    private static final String RANGER_DB_NAME = "ranger";

    private static final String HIVE_DB_NAME = "hivetest";

    @Mock
    private StackViewService stackViewService;

    @Mock
    private PlatformAwareSdxConnector platformAwareSdxConnector;

    @InjectMocks
    private SharedServiceValidator underTest;

    @Test
    void testWithPdlDatalake() {
        StackV4Request stackRequest = getStackV4Request(CloudPlatform.GCP);
        when(platformAwareSdxConnector.getSdxBasicViewByEnvironmentCrn(any())).thenReturn(Optional.of(getSdxBasicView(TargetPlatform.PDL)));

        ValidationResult validationResult = underTest.checkSharedServiceStackRequirements(stackRequest, getWorkspace());

        assertFalse(validationResult.hasError());
        verifyNoInteractions(stackViewService);
    }

    @Test
    void testWithValidPaasRequest() {
        StackV4Request stackRequest = getStackV4Request(CloudPlatform.GCP);
        when(platformAwareSdxConnector.getSdxBasicViewByEnvironmentCrn(any())).thenReturn(Optional.of(getSdxBasicView(TargetPlatform.PAAS)));
        when(stackViewService.findByName(eq(DATALAKE_NAME), anyLong())).thenReturn(Optional.ofNullable(getStackView()));

        ValidationResult validationResult = underTest.checkSharedServiceStackRequirements(stackRequest, getWorkspace());

        assertFalse(validationResult.hasError());
    }

    @Test
    void testWithInvalidPaasRequest() {
        StackV4Request stackRequest = getStackV4Request(CloudPlatform.AWS);
        when(platformAwareSdxConnector.getSdxBasicViewByEnvironmentCrn(any())).thenReturn(Optional.of(getSdxBasicView(TargetPlatform.PAAS)));
        when(stackViewService.findByName(eq(DATALAKE_NAME), anyLong())).thenReturn(Optional.ofNullable(getStackView()));

        ValidationResult validationResult = underTest.checkSharedServiceStackRequirements(stackRequest, getWorkspace());

        assertTrue(validationResult.hasError());
    }

    private SdxBasicView getSdxBasicView(TargetPlatform targetPlatform) {
        return new SdxBasicView(DATALAKE_NAME, "crn", "", false, 0L, "", targetPlatform);
    }

    private RDSConfig getDatabase(String type) {
        RDSConfig rdsConfig = new RDSConfig();
        rdsConfig.setType(type);
        return rdsConfig;
    }

    private StackV4Request getStackV4Request(CloudPlatform cloudPlatform) {
        ClusterV4Request clusterRequest = new ClusterV4Request();
        clusterRequest.setDatabases(Sets.newHashSet(RANGER_DB_NAME, HIVE_DB_NAME));
        clusterRequest.setBlueprintName("test-blueprint");
        StackV4Request stackRequest = new StackV4Request();
        stackRequest.setSharedService(new SharedServiceV4Request());
        stackRequest.getSharedService().setDatalakeName(DATALAKE_NAME);
        stackRequest.setCluster(clusterRequest);
        stackRequest.setCloudPlatform(cloudPlatform);
        stackRequest.setEnvironmentCrn("env");
        stackRequest.setName("teststack");
        return stackRequest;
    }

    private StackView getStackView() {
        StackView stackView = new StackView();
        stackView.setCloudPlatform("GCP");
        stackView.setWorkspace(getWorkspace());
        return stackView;
    }

    private Workspace getWorkspace() {
        Workspace workspace = new Workspace();
        workspace.setId(1L);
        return workspace;
    }

}