package com.sequenceiq.datalake.service.upgrade;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.ImageInfoV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.UpgradeOptionsV4Response;
import com.sequenceiq.datalake.controller.exception.BadRequestException;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.sdx.api.model.SdxClusterShape;

@ExtendWith(MockitoExtension.class)
public class SdxClusterUpgradeServiceTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    private static final String STACK_CRN = "crn:cdp:sdx:us-west-1:1234:sdxcluster:mystack";

    private static final String IMAGE_ID = "image-id";

    private static final String ANOTHER_IMAGE_ID = "another-image-id";

    @Mock
    private StackV4Endpoint stackV4Endpoint;

    @Mock
    private SdxService sdxService;

    @Mock
    private SdxReactorFlowManager sdxReactorFlowManager;

    @InjectMocks
    private SdxClusterUpgradeService underTest;

    private UpgradeOptionsV4Response response;

    private SdxCluster sdxCluster;

    @BeforeEach
    public void setUp() {
        response = new UpgradeOptionsV4Response();
        sdxCluster = getValidSdxCluster();

        when(sdxService.getByCrn(anyString(), anyString())).thenReturn(sdxCluster);
        when(stackV4Endpoint.checkForClusterUpgradeByName(anyLong(), anyString())).thenReturn(response);
    }

    @Test
    public void testNoImageFound() {
        response.setUpgradeCandidates(new ArrayList<>());
        BadRequestException exception = Assertions.assertThrows(
                BadRequestException.class,
                () -> underTest.triggerClusterUpgradeByCrn(USER_CRN, STACK_CRN, IMAGE_ID));

        assertEquals("There is no compatible image to upgrade for stack " + sdxCluster.getClusterName(), exception.getMessage());
    }

    @Test
    public void testNoCompatibleImageFound() {
        ImageInfoV4Response imageInfo = new ImageInfoV4Response();
        imageInfo.setImageId(ANOTHER_IMAGE_ID);
        response.setUpgradeCandidates(List.of(imageInfo));
        BadRequestException exception = Assertions.assertThrows(
                BadRequestException.class,
                () -> underTest.triggerClusterUpgradeByCrn(USER_CRN, STACK_CRN, IMAGE_ID));

        assertEquals(String.format("The given image (%s) is not eligible for upgrading the cluster. "
                + "Please choose an id from the following image(s): %s", IMAGE_ID, ANOTHER_IMAGE_ID), exception.getMessage());
    }

    @Test
    public void testOtherError() {
        ImageInfoV4Response imageInfo = new ImageInfoV4Response();
        imageInfo.setImageId(IMAGE_ID);
        response.setUpgradeCandidates(List.of(imageInfo));
        response.setReason("error reason");
        BadRequestException exception = Assertions.assertThrows(
                BadRequestException.class,
                () -> underTest.triggerClusterUpgradeByCrn(USER_CRN, STACK_CRN, IMAGE_ID));

        assertEquals(String.format("The following error prevents the cluster upgrade process, please fix it and try again %s.",
                "error reason"), exception.getMessage());
    }

    @Test
    public void testNoError() {
        ImageInfoV4Response imageInfo = new ImageInfoV4Response();
        imageInfo.setImageId(IMAGE_ID);
        response.setUpgradeCandidates(List.of(imageInfo));
        assertDoesNotThrow(() -> underTest.triggerClusterUpgradeByCrn(USER_CRN, STACK_CRN, IMAGE_ID));
    }

    private SdxCluster getValidSdxCluster() {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setClusterName("test-sdx-cluster");
        sdxCluster.setClusterShape(SdxClusterShape.MEDIUM_DUTY_HA);
        sdxCluster.setEnvName("test-env");
        sdxCluster.setCrn("crn:sdxcluster");
        return sdxCluster;
    }
}