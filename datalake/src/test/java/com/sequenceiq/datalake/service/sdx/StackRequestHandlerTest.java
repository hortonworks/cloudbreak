package com.sequenceiq.datalake.service.sdx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack.GcpStackV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.image.ImageSettingsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.constant.GcpConstants;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.datalake.configuration.CDPConfigService;
import com.sequenceiq.sdx.api.model.SdxClusterShape;

@ExtendWith(MockitoExtension.class)
class StackRequestHandlerTest {

    @Mock
    private CDPConfigService cdpConfigService;

    @InjectMocks
    private StackRequestHandler underTest;

    @Test
    void refreshDatahubsWithoutName() throws IOException {
        String lightDutyJson = FileReaderUtils.readFileFromClasspath("/duties/7.2.7/aws/light_duty.json");
        StackV4Request stackV4Request = JsonUtil.readValue(lightDutyJson, StackV4Request.class);
        ImageSettingsV4Request imageSettingsV4Request = new ImageSettingsV4Request();
        imageSettingsV4Request.setCatalog("cdp-default");
        imageSettingsV4Request.setId("imageId_1");

        when(cdpConfigService.getConfigForKey(any())).thenReturn(stackV4Request);

        StackV4Request response = underTest.getStackRequest(SdxClusterShape.LIGHT_DUTY, stackV4Request, CloudPlatform.AWS,
                "7.2.7", imageSettingsV4Request, Architecture.X86_64);

        assertNotNull(response.getImage());
        assertEquals("cdp-default", response.getImage().getCatalog());
        assertEquals("imageId_1", response.getImage().getId());
    }

    @Test
    void setStackRequestParamsSetsGcpRazFromResponseWhenGcp() {
        StackV4Request stackV4Request = new StackV4Request();
        stackV4Request.setCluster(new ClusterV4Request());
        StackV4Response stackV4Response = mock(StackV4Response.class);
        GcpStackV4Parameters gcpParams = new GcpStackV4Parameters();
        gcpParams.setRazAuthenticationType(GcpConstants.RAZ_AUTHENTICATION_TYPE_CAB);

        when(stackV4Response.getCloudPlatform()).thenReturn(CloudPlatform.GCP);
        when(stackV4Response.getGcp()).thenReturn(gcpParams);

        underTest.setStackRequestParams(stackV4Request, stackV4Response, 21, true, false, null);

        assertNotNull(stackV4Request.getGcp());
        assertEquals(GcpConstants.RAZ_AUTHENTICATION_TYPE_CAB, stackV4Request.getGcp().getRazAuthenticationType());
    }

    @Test
    void setStackRequestParamsNotSetWhenGcpResponseHasNoRazType() {
        StackV4Request stackV4Request = new StackV4Request();
        stackV4Request.setCluster(new ClusterV4Request());
        StackV4Response stackV4Response = mock(StackV4Response.class);

        when(stackV4Response.getCloudPlatform()).thenReturn(CloudPlatform.GCP);
        when(stackV4Response.getGcp()).thenReturn(null);

        underTest.setStackRequestParams(stackV4Request, stackV4Response, 21, true, false, null);

        assertNull(stackV4Request.getGcp());
    }

    @Test
    void testSetStackRequestParamsDefaultsToNullWhenNotGcp() {
        StackV4Request stackV4Request = new StackV4Request();
        stackV4Request.setCluster(new ClusterV4Request());
        StackV4Response stackV4Response = mock(StackV4Response.class);

        when(stackV4Response.getCloudPlatform()).thenReturn(CloudPlatform.AWS);

        underTest.setStackRequestParams(stackV4Request, stackV4Response, 21, true, false, null);

        assertNull(stackV4Request.getGcp());
    }

    @Test
    void setStackRequestParamsSetsJavaVersionAndRazFlags() {
        StackV4Request stackV4Request = new StackV4Request();
        stackV4Request.setCluster(new ClusterV4Request());
        StackV4Response stackV4Response = mock(StackV4Response.class);
        when(stackV4Response.getCloudPlatform()).thenReturn(CloudPlatform.AWS);

        underTest.setStackRequestParams(stackV4Request, stackV4Response, 21, true, true, "encProfileCrn");

        assertEquals(21, stackV4Request.getJavaVersion());
        assertEquals(true, stackV4Request.getCluster().isRangerRazEnabled());
        assertEquals(true, stackV4Request.getCluster().isRangerRmsEnabled());
        assertEquals("encProfileCrn", stackV4Request.getCluster().getEncryptionProfileCrn());
    }

    @Test
    void testSetStackRequestParamsWhenResponseIsNull() {
        StackV4Request stackV4Request = new StackV4Request();
        stackV4Request.setCluster(new ClusterV4Request());

        underTest.setStackRequestParams(stackV4Request, null, 21, true, false, null);

        assertNull(stackV4Request.getGcp());
    }
}
