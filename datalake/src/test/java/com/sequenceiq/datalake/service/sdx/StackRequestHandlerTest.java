package com.sequenceiq.datalake.service.sdx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.image.ImageSettingsV4Request;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
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

        Assertions.assertNotNull(response.getImage());
        assertEquals("cdp-default", response.getImage().getCatalog());
        assertEquals("imageId_1", response.getImage().getId());
    }
}