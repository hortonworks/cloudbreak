package com.sequenceiq.cloudbreak.service.upgrade.validation.service;

import static com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider.doAs;
import static com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion.STACK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.common.exception.UpgradeValidationFailedException;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.upgrade.UpgradeImageInfo;
import com.sequenceiq.cloudbreak.util.CdhVersionProvider;
import com.sequenceiq.cloudbreak.view.ClusterView;

@ExtendWith(MockitoExtension.class)
class StackVersionBasedRollingUpgradeValidatorTest {

    private static final String ACTOR = "crn:cdp:iam:us-west-1:cloudera:user:__internal__actor__";

    private static final long CLUSTER_ID = 1L;

    @InjectMocks
    private StackVersionBasedRollingUpgradeValidator underTest;

    @Spy
    private CdhVersionProvider cdhVersionProvider;

    @Mock
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private StackDto stackDto;

    @Mock
    private ClusterView clusterView;

    @BeforeEach
    void before() {
        lenient().when(stackDto.getCluster()).thenReturn(clusterView);
        lenient().when(clusterView.getId()).thenReturn(CLUSTER_ID);
    }

    @Test
    void testValidateShouldSkipValidationWhenTheRollingUpgradeIsNotEnabled() {
        underTest.validate(createRequest(false, "7.2.17", "7.2.18"));
        verifyNoInteractions(clusterComponentConfigProvider, entitlementService);
    }

    @Test
    void testValidateShouldSkipValidationWhenTheTargetRuntimeIsLowerThan7217() {
        doAs(ACTOR, () -> underTest.validate(createRequest(true, "7.2.15", "7.2.16")));
        verify(clusterComponentConfigProvider).getCdhProduct(CLUSTER_ID);
    }

    @Test
    void testValidateShouldSkipValidationWhenTheCurrentRuntimeIsLowerThan7217() {
        when(entitlementService.isSkipRollingUpgradeValidationEnabled(anyString())).thenReturn(false);

        doAs(ACTOR, () -> underTest.validate(createRequest(true, "7.2.18", "7.2.18")));

        verify(entitlementService).isSkipRollingUpgradeValidationEnabled(anyString());
        verify(clusterComponentConfigProvider).getCdhProduct(CLUSTER_ID);
    }

    @Test
    void testValidateShouldSkipValidationWhenTheEntitlementIsEnabled() {
        when(entitlementService.isSkipRollingUpgradeValidationEnabled(anyString())).thenReturn(true);

        doAs(ACTOR, () -> underTest.validate(createRequest(true, "7.2.17", "7.2.18")));

        verify(entitlementService).isSkipRollingUpgradeValidationEnabled(anyString());
        verifyNoInteractions(clusterComponentConfigProvider);
    }

    @Test
    void testValidateShouldNotThrowExceptionWhenTheRollingUpgradeIsPermittedFor7218() {
        when(entitlementService.isSkipRollingUpgradeValidationEnabled(anyString())).thenReturn(false);
        when(clusterComponentConfigProvider.getCdhProduct(CLUSTER_ID)).thenReturn(Optional.of(createCdhProduct("7.2.17-1.cdh7.2.17.p300.49453544")));

        doAs(ACTOR, () -> underTest.validate(createRequest(true, "7.2.17", "7.2.18")));

        verify(entitlementService).isSkipRollingUpgradeValidationEnabled(anyString());
        verify(clusterComponentConfigProvider).getCdhProduct(CLUSTER_ID);
    }

    @Test
    void testValidateShouldNotThrowExceptionWhenTheCurrentRuntimeIs7217p200AndTheTargetRuntimeIs7217() {
        when(entitlementService.isSkipRollingUpgradeValidationEnabled(anyString())).thenReturn(false);
        when(clusterComponentConfigProvider.getCdhProduct(CLUSTER_ID)).thenReturn(Optional.of(createCdhProduct("7.2.17-1.cdh7.2.17.p200.49453544")));

        doAs(ACTOR, () -> underTest.validate(createRequest(true, "7.2.17", "7.2.17")));

        verify(entitlementService).isSkipRollingUpgradeValidationEnabled(anyString());
        verify(clusterComponentConfigProvider).getCdhProduct(CLUSTER_ID);
    }

    @Test
    void testValidateShouldThrowExceptionWhenTheCurrentRuntimeIs7217p100AndTheTargetRuntimeIs7217() {
        when(entitlementService.isSkipRollingUpgradeValidationEnabled(anyString())).thenReturn(false);
        when(clusterComponentConfigProvider.getCdhProduct(CLUSTER_ID)).thenReturn(Optional.of(createCdhProduct("7.2.17-1.cdh7.2.17.p100.49453544")));

        Exception exception = assertThrows(UpgradeValidationFailedException.class,
                () -> doAs(ACTOR, () -> underTest.validate(createRequest(true, "7.2.17", "7.2.17"))));

        assertEquals("You are not eligible to perform rolling upgrade to the selected runtime because your current runtime version (7.2.17.p100) "
                + "does not contains important changes. Please run a runtime upgrade to the latest (or at least 7.2.17.p200) service pack for your current "
                + "runtime version.After this you will be able to launch a rolling upgrade to the more recent runtime.", exception.getMessage());

        verify(entitlementService).isSkipRollingUpgradeValidationEnabled(anyString());
        verify(clusterComponentConfigProvider).getCdhProduct(CLUSTER_ID);
    }

    @Test
    void testValidateShouldThrowExceptionWhenTheRuntimeVersionIsLowerThan7217ButThePatchVersionIsAcceptable() {
        when(entitlementService.isSkipRollingUpgradeValidationEnabled(anyString())).thenReturn(false);
        when(clusterComponentConfigProvider.getCdhProduct(CLUSTER_ID)).thenReturn(Optional.of(createCdhProduct("7.2.16-1.cdh7.2.16.p300.49453544")));

        Exception exception = assertThrows(UpgradeValidationFailedException.class,
                () -> doAs(ACTOR, () -> underTest.validate(createRequest(true, "7.2.16", "7.2.18"))));

        assertEquals("You are not eligible to perform rolling upgrade to the selected runtime because your current runtime version (7.2.16.p300) "
                + "does not contains important changes. Please run a runtime upgrade to the latest (or at least 7.2.17.p300) service pack for your current "
                + "runtime version.After this you will be able to launch a rolling upgrade to the more recent runtime.", exception.getMessage());

        verify(entitlementService).isSkipRollingUpgradeValidationEnabled(anyString());
        verify(clusterComponentConfigProvider).getCdhProduct(CLUSTER_ID);
    }

    @Test
    void testValidateShouldThrowExceptionWhenTheRuntimeVersionIs7217ButThePatchVersionLowerThanAccepted() {
        when(entitlementService.isSkipRollingUpgradeValidationEnabled(anyString())).thenReturn(false);
        when(clusterComponentConfigProvider.getCdhProduct(CLUSTER_ID)).thenReturn(Optional.of(createCdhProduct("7.2.17-1.cdh7.2.17.p0.49453544")));

        Exception exception = assertThrows(UpgradeValidationFailedException.class,
                () -> doAs(ACTOR, () -> underTest.validate(createRequest(true, "7.2.17", "7.2.18"))));

        assertEquals("You are not eligible to perform rolling upgrade to the selected runtime because your current runtime version (7.2.17.p0) "
                + "does not contains important changes. Please run a runtime upgrade to the latest (or at least 7.2.17.p300) service pack for your current "
                + "runtime version.After this you will be able to launch a rolling upgrade to the more recent runtime.", exception.getMessage());

        verify(entitlementService).isSkipRollingUpgradeValidationEnabled(anyString());
        verify(clusterComponentConfigProvider).getCdhProduct(CLUSTER_ID);
    }

    private ServiceUpgradeValidationRequest createRequest(boolean rollingUpgradeEnabled, String currentRuntimeVersion, String targetRuntimeVersion) {
        com.sequenceiq.cloudbreak.cloud.model.Image image = com.sequenceiq.cloudbreak.cloud.model.Image.builder()
                .withPackageVersions(Map.of(STACK.getKey(), currentRuntimeVersion))
                .build();
        return new ServiceUpgradeValidationRequest(stackDto, false, rollingUpgradeEnabled,
                new UpgradeImageInfo(image,
                        StatedImage.statedImage(Image.builder().withVersion(targetRuntimeVersion).build(), null, null)), false);
    }

    private ClouderaManagerProduct createCdhProduct(String cdhVersion) {
        return new ClouderaManagerProduct().withVersion(cdhVersion);
    }
}