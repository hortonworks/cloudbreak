package com.sequenceiq.cloudbreak.service.upgrade.validation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateService;
import com.sequenceiq.cloudbreak.common.exception.UpgradeValidationFailedException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.upgrade.UpgradeImageInfo;
import com.sequenceiq.cloudbreak.view.ClusterView;

@ExtendWith(MockitoExtension.class)
class Spark2UpgradeValidatorTest {

    public static final String EXPECTED = "Your current cluster configuration includes Spark2, " +
            "which will be deprecated in the upcoming 7.3.x release. " +
            "As a result, your cluster will only support the 7.2.x line" +
            " and you will not be able to upgrade to the 7.3.x line. " +
            "To ensure a smooth transition and continued support, " +
            "please start planning to migrate to Spark3 by recreating your DH cluster. " +
            "This will involve setting up a new cluster with Spark3 " +
            "alongside your existing cluster.";

    @InjectMocks
    private Spark2UpgradeValidator underTest;

    @Mock
    private CmTemplateService cmTemplateService;

    @Mock
    private StackDto stack;

    @Mock
    private ClusterView clusterView;

    @Mock
    private Blueprint blueprint;

    @Mock
    private Image image;

    @Mock
    private StatedImage targetStatedImage;

    @Mock
    private com.sequenceiq.cloudbreak.cloud.model.catalog.Image targetImage;

    @Test
    public void testValidateShouldThrowExceptionWhenLockComponentsIsFalseAndSpark2ClusterGoesTo730() {
        setupMockWhenException("7.3.0", true);

        Exception exception = assertThrows(UpgradeValidationFailedException.class,
                () -> underTest.validate(getServiceUpgradeValidationRequest(false)));

        assertEquals(EXPECTED, exception.getMessage());
    }

    @Test
    public void testValidateShouldThrowExceptionWhenLockComponentsIsFalseAndSpark2ClusterGoesTo731() {
        setupMockWhenException("7.3.1", true);

        Exception exception = assertThrows(UpgradeValidationFailedException.class,
                () -> underTest.validate(getServiceUpgradeValidationRequest(false)));

        assertEquals(EXPECTED, exception.getMessage());
    }

    @Test
    public void testValidateWhenLockComponentsIsTrue() {
        underTest.validate(getServiceUpgradeValidationRequest(true));
    }

    @Test
    public void testValidateWhenLockComponentsIsFalseAndGoesTo719AndSpark2PresentedShouldNotThrowException() {
        setupMockWhenException("7.1.9", true);

        underTest.validate(getServiceUpgradeValidationRequest(false));
    }

    @Test
    public void testValidateWhenLockComponentsIsFalseAndGoesTo719AndSpark2NotPresentedShouldNotThrowException() {
        when(clusterView.getExtendedBlueprintText()).thenReturn("");
        when(stack.getCluster()).thenReturn(clusterView);
        when(cmTemplateService.isServiceTypePresent(any(), any())).thenReturn(false);

        underTest.validate(getServiceUpgradeValidationRequest(false));
    }

    private ServiceUpgradeValidationRequest getServiceUpgradeValidationRequest(boolean lockComponent) {
        ServiceUpgradeValidationRequest serviceUpgradeValidationRequest = new ServiceUpgradeValidationRequest(
                stack,
                lockComponent,
                false,
                new UpgradeImageInfo(image, targetStatedImage),
                false
        );
        return serviceUpgradeValidationRequest;
    }

    private void setupMockWhenException(String version, boolean spark2Presented) {
        when(targetImage.getVersion()).thenReturn(version);
        when(targetStatedImage.getImage()).thenReturn(targetImage);
        when(clusterView.getExtendedBlueprintText()).thenReturn("");
        when(stack.getCluster()).thenReturn(clusterView);
        when(cmTemplateService.isServiceTypePresent(any(), any())).thenReturn(spark2Presented);
    }
}