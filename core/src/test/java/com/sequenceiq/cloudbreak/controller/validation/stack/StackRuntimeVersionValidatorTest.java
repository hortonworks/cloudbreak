package com.sequenceiq.cloudbreak.controller.validation.stack;

import static com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider.INTERNAL_ACTOR_CRN;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.ClouderaManagerV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.repository.ClouderaManagerRepositoryV4Request;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.StackDetails;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.service.datalake.SdxClientService;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;

@RunWith(MockitoJUnitRunner.class)
public class StackRuntimeVersionValidatorTest {

    private static final String DATA_HUB_VERSION = "7.2.0";

    private static final String ENVIRONMENT_CRN = "environment-crn";

    private static final String UUID = java.util.UUID.randomUUID().toString();

    private static final String USER_CRN = "crn:altus:iam:us-west-1:" + UUID + ":user:" + UUID;

    private static final String SDX_VERSION = "7.2.0";

    @InjectMocks
    private StackRuntimeVersionValidator underTest;

    @Mock
    private SdxClientService sdxClientService;

    @Mock
    private EntitlementService entitlementService;

    @Test
    public void testValidateShouldNotDoAnythingWhenTheEntitlementIsTurnedOn() {
        when(entitlementService.isDifferentDataHubAndDataLakeVersionAllowed(eq(INTERNAL_ACTOR_CRN), any())).thenReturn(true);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.validate(new StackV4Request(), mock(Image.class)));

        verify(entitlementService).isDifferentDataHubAndDataLakeVersionAllowed(eq(INTERNAL_ACTOR_CRN), any());
        verifyNoInteractions(sdxClientService);
    }

    @Test
    public void testValidateShouldNotDoAnythingWhenDataHubVersionIsNotPresent() {
        StackV4Request request = createStackRequestWithoutCm();
        when(entitlementService.isDifferentDataHubAndDataLakeVersionAllowed(eq(INTERNAL_ACTOR_CRN), any())).thenReturn(false);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.validate(request, mock(Image.class)));

        verify(entitlementService).isDifferentDataHubAndDataLakeVersionAllowed(eq(INTERNAL_ACTOR_CRN), any());
        verifyNoInteractions(sdxClientService);
    }

    @Test
    public void testValidateShouldNotDoAnythingWhenTheEntitlementIsTurnedOffAndTheImageContainsTheVersionAndTheVersionsAreEquals() {
        StackV4Request request = createStackRequestWithoutCm();
        Image image = createImage(DATA_HUB_VERSION);
        when(entitlementService.isDifferentDataHubAndDataLakeVersionAllowed(eq(INTERNAL_ACTOR_CRN), any())).thenReturn(false);
        when(sdxClientService.getByEnvironmentCrn(ENVIRONMENT_CRN)).thenReturn(createSdxClusterResponse());

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.validate(request, image));

        verify(entitlementService).isDifferentDataHubAndDataLakeVersionAllowed(eq(INTERNAL_ACTOR_CRN), any());
        verify(sdxClientService).getByEnvironmentCrn(ENVIRONMENT_CRN);
    }

    @Test
    public void testValidateShouldNotDoAnythingWhenTheEntitlementIsTurnedOffAndTheRequestContainsTheVersionAndTheVersionsAreEquals() {
        StackV4Request request = createStackRequest(DATA_HUB_VERSION);
        when(entitlementService.isDifferentDataHubAndDataLakeVersionAllowed(eq(INTERNAL_ACTOR_CRN), any())).thenReturn(false);
        when(sdxClientService.getByEnvironmentCrn(ENVIRONMENT_CRN)).thenReturn(createSdxClusterResponse());

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.validate(request, mock(Image.class)));

        verify(entitlementService).isDifferentDataHubAndDataLakeVersionAllowed(eq(INTERNAL_ACTOR_CRN), any());
        verify(sdxClientService).getByEnvironmentCrn(ENVIRONMENT_CRN);
    }

    @Test(expected = BadRequestException.class)
    public void testValidateShouldThrowExceptionWhenTheEntitlementIsTurnedOffAndTheImageContainsTheVersionAndTheVersionsAreNotEquals() {
        StackV4Request request = createStackRequestWithoutCm();
        Image image = createImage("7.2.2");
        when(entitlementService.isDifferentDataHubAndDataLakeVersionAllowed(eq(INTERNAL_ACTOR_CRN), any())).thenReturn(false);
        when(sdxClientService.getByEnvironmentCrn(ENVIRONMENT_CRN)).thenReturn(createSdxClusterResponse());

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.validate(request, image));

        verify(entitlementService).isDifferentDataHubAndDataLakeVersionAllowed(eq(INTERNAL_ACTOR_CRN), any());
        verify(sdxClientService).getByEnvironmentCrn(ENVIRONMENT_CRN);
    }

    @Test(expected = BadRequestException.class)
    public void testValidateShouldThrowExceptionWhenTheEntitlementIsTurnedOffAndTheStackContainsTheVersionAndTheVersionsAreNotEquals() {
        StackV4Request request = createStackRequest("7.2.2");
        when(entitlementService.isDifferentDataHubAndDataLakeVersionAllowed(eq(INTERNAL_ACTOR_CRN), any())).thenReturn(false);
        when(sdxClientService.getByEnvironmentCrn(ENVIRONMENT_CRN)).thenReturn(createSdxClusterResponse());

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.validate(request, mock(Image.class)));

        verify(entitlementService).isDifferentDataHubAndDataLakeVersionAllowed(eq(INTERNAL_ACTOR_CRN), any());
        verify(sdxClientService).getByEnvironmentCrn(ENVIRONMENT_CRN);
    }

    private StackV4Request createStackRequest(String dataHubVersion) {
        ClouderaManagerRepositoryV4Request clouderaManagerRepositoryV4Request = new ClouderaManagerRepositoryV4Request();
        clouderaManagerRepositoryV4Request.setVersion(dataHubVersion);
        ClouderaManagerV4Request clouderaManagerV4Request = new ClouderaManagerV4Request();
        clouderaManagerV4Request.setRepository(clouderaManagerRepositoryV4Request);
        ClusterV4Request clusterV4Request = new ClusterV4Request();
        clusterV4Request.setCm(clouderaManagerV4Request);
        StackV4Request stackV4Request = new StackV4Request();
        stackV4Request.setCluster(clusterV4Request);
        stackV4Request.setEnvironmentCrn(ENVIRONMENT_CRN);
        return stackV4Request;
    }

    private Image createImage(String dataHubVersion) {
        return new Image(null, null, null, null, null, null, null, null, new StackDetails(dataHubVersion, null, null), null, null, null, null, null);
    }

    private StackV4Request createStackRequestWithoutCm() {
        ClusterV4Request clusterV4Request = new ClusterV4Request();
        StackV4Request stackV4Request = new StackV4Request();
        stackV4Request.setCluster(clusterV4Request);
        stackV4Request.setEnvironmentCrn(ENVIRONMENT_CRN);
        return stackV4Request;
    }

    private List<SdxClusterResponse> createSdxClusterResponse() {
        SdxClusterResponse sdxClusterResponse = new SdxClusterResponse();
        sdxClusterResponse.setRuntime(SDX_VERSION);
        return Collections.singletonList(sdxClusterResponse);
    }

}