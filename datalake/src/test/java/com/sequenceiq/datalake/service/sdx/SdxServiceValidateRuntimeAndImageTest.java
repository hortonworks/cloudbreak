package com.sequenceiq.datalake.service.sdx;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.image.ImageSettingsV4Request;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.sdx.api.model.SdxClusterRequest;

@ExtendWith(MockitoExtension.class)
public class SdxServiceValidateRuntimeAndImageTest {

    @InjectMocks
    private SdxService underTest;

    private SdxClusterRequest clusterRequest;

    private DetailedEnvironmentResponse environment;

    private ImageSettingsV4Request imageSettingsV4Request;

    private ImageV4Response imageV4Response;

    @BeforeEach
    void setUp() {
        clusterRequest = new SdxClusterRequest();
        environment = new DetailedEnvironmentResponse();
        environment.setCloudPlatform("AWS");
        imageSettingsV4Request = new ImageSettingsV4Request();
        imageSettingsV4Request.setId("image-id");
        imageV4Response = new ImageV4Response();
    }

    @Test
    void baseImageWithRuntimeSpecified() {
        clusterRequest.setRuntime("7.2.16");
        underTest.validateRuntimeAndImage(clusterRequest, environment, imageSettingsV4Request, imageV4Response);
    }

    @Test
    void baseImageWithoutRuntimeSpecified() {
        underTest.validateRuntimeAndImage(clusterRequest, environment, imageSettingsV4Request, imageV4Response);
    }

    @Test
    void runtimeImageWithNoRuntimeSpecified() {
        imageV4Response.setVersion("7.2.17");
        underTest.validateRuntimeAndImage(clusterRequest, environment, imageSettingsV4Request, imageV4Response);
    }

    @Test
    void runtimeImageWithEmptyRuntimeSpecified() {
        clusterRequest.setRuntime("");
        imageV4Response.setVersion("7.2.17");
        underTest.validateRuntimeAndImage(clusterRequest, environment, imageSettingsV4Request, imageV4Response);
    }

    @Test
    void runtimeImageWithSameRuntimeSpecified() {
        String version = "7.2.16";
        clusterRequest.setRuntime(version);
        imageV4Response.setVersion(version);
        underTest.validateRuntimeAndImage(clusterRequest, environment, imageSettingsV4Request, imageV4Response);
    }

    @Test
    void runtimeImageWithDifferentRuntimeSpecified() {
        clusterRequest.setRuntime("7.2.16");
        imageV4Response.setVersion("7.2.17");
        assertThatThrownBy(() -> underTest.validateRuntimeAndImage(clusterRequest, environment, imageSettingsV4Request, imageV4Response))
                .hasMessage("SDX cluster request must not specify both runtime version and image at the same time because image " +
                        "decides runtime version.");
    }

    @Test
    void noImageWithRuntimeSpecified() {
        clusterRequest.setRuntime("7.2.16");
        underTest.validateRuntimeAndImage(clusterRequest, environment, null, null);
    }

    @Test
    void noImageWithNoRuntimeSpecified() {
        underTest.validateRuntimeAndImage(clusterRequest, environment, null, null);
    }

    @Test
    void noImageWithEmptyRuntimeSpecified() {
        clusterRequest.setRuntime("");
        underTest.validateRuntimeAndImage(clusterRequest, environment, null, null);
    }

    @Test
    void imageNotFoundWithNoRuntime() {
        assertThatThrownBy(() -> underTest.validateRuntimeAndImage(clusterRequest, environment, imageSettingsV4Request, null))
                .hasMessage("SDX cluster request has null runtime version and null image response. It cannot " +
                        "determine the runtime version.");
    }

    @Test
    void imageNotFoundWithEmptyRuntime() {
        clusterRequest.setRuntime("");
        assertThatThrownBy(() -> underTest.validateRuntimeAndImage(clusterRequest, environment, imageSettingsV4Request, null))
                .hasMessage("SDX cluster request has null runtime version and null image response. It cannot " +
                        "determine the runtime version.");
    }

    @Test
    void imageNotFoundForMock() {
        environment.setCloudPlatform("MOCK");
        underTest.validateRuntimeAndImage(clusterRequest, environment, imageSettingsV4Request, null);
    }

    @Test
    void imageIdAndOsSetCorrectly() {
        imageSettingsV4Request.setOs("os");
        imageV4Response.setOs("os");

        assertThatCode(() -> underTest.validateRuntimeAndImage(clusterRequest, environment, imageSettingsV4Request, imageV4Response))
                .doesNotThrowAnyException();
    }

    @Test
    void imageIdAndOsSetIncorrectly() {
        imageSettingsV4Request.setOs("os");
        imageV4Response.setOs("otheros");

        assertThatThrownBy(() -> underTest.validateRuntimeAndImage(clusterRequest, environment, imageSettingsV4Request, imageV4Response))
                .hasMessage("Image with requested id has different os than requested.");
    }
}
