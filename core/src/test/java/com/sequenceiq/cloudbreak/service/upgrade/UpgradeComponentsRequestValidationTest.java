package com.sequenceiq.cloudbreak.service.upgrade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

import jakarta.validation.ConstraintValidatorContext;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.InternalUpgradeSettings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.ClouderaManagerV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.tags.upgrade.UpgradeV4Request;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.service.image.ImageChangeDto;
import com.sequenceiq.cloudbreak.validation.UpgradeRequestValidator;
import com.sequenceiq.distrox.api.v1.distrox.model.cluster.cm.ClouderaManagerV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroXUpgradeRequestValidator;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroXUpgradeV1Request;

class UpgradeComponentsRequestValidationTest {

    @Test
    void acceptsComponentsWithoutRuntime() throws Exception {
        UpgradeV4Request request = new UpgradeV4Request();
        request.setComponents(validComponents());
        assertTrue(new UpgradeRequestValidator().isValid(request, null));
        DistroXUpgradeV1Request dhRequest = new DistroXUpgradeV1Request();
        dhRequest.setComponents(JsonUtil.readValue(JsonUtil.writeValueAsString(validComponents()), ClouderaManagerV1Request.class));
        assertTrue(new DistroXUpgradeRequestValidator().isValid(dhRequest, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"7.3.1", "7.3.2"})
    void rejectsRuntimeWithComponentsEvenWhenVersionsMatch(String runtime) throws Exception {
        ConstraintValidatorContext context = mock(ConstraintValidatorContext.class, RETURNS_DEEP_STUBS);
        UpgradeV4Request request = new UpgradeV4Request();
        request.setComponents(validComponents());
        request.setRuntime(runtime);
        DistroXUpgradeV1Request dhRequest = new DistroXUpgradeV1Request();
        dhRequest.setComponents(JsonUtil.readValue(JsonUtil.writeValueAsString(validComponents()), ClouderaManagerV1Request.class));
        dhRequest.setRuntime(runtime);

        assertFalse(new UpgradeRequestValidator().isValid(request, context));
        assertFalse(new DistroXUpgradeRequestValidator().isValid(dhRequest, context));
    }

    @ParameterizedTest
    @ValueSource(strings = {"imageId", "lockComponents", "dryRun", "showAvailableImages"})
    void rejectsConflictingComponentsOptions(String option) throws Exception {
        String json = "{\"components\":{},\"" + option + "\":" + ("imageId".equals(option) ? "\"image\"" :
                "showAvailableImages".equals(option) ? "\"SHOW\"" : "true") + "}";
        ConstraintValidatorContext context = mock(ConstraintValidatorContext.class, RETURNS_DEEP_STUBS);
        assertFalse(new UpgradeRequestValidator().isValid(JsonUtil.readValue(json, UpgradeV4Request.class), context));
        assertFalse(new DistroXUpgradeRequestValidator().isValid(JsonUtil.readValue(json, DistroXUpgradeV1Request.class), context));
    }

    @Test
    void reinitiationAllowsOriginallySelectedImageAndRuntime() throws Exception {
        UpgradeV4Request request = new UpgradeV4Request();
        request.setComponents(validComponents());
        request.setImageId("original-image");
        request.setRuntime("7.3.2");
        request.setInternalUpgradeSettings(InternalUpgradeSettings.builder().withUpgradeReinitiation(true).build());

        assertTrue(new UpgradeRequestValidator().isValid(request, null));
    }

    @Test
    void oldImageChangeJsonCanStillBeRead() throws Exception {
        ImageChangeDto dto = JsonUtil.readValue("{\"stackId\":1,\"imageId\":\"prewarm\"}", ImageChangeDto.class);
        assertEquals("prewarm", dto.getImageId());
        assertNull(dto.getComponents());
    }

    @Test
    void oldComponentsRequestJsonCanStillBeRead() throws Exception {
        String json = """
                {"stackId":1,"imageId":"base","components":{"enableAutoTls":true,
                "repository":{"version":"7.13.2.20000-83309838","baseUrl":"https://build/cm/"},
                "products":[{"name":"CDH","version":"7.3.2-1.cdh7.3.2.p20000.83281140","parcel":"https://build/cdh/"}]}}
                """;
        ImageChangeDto dto = JsonUtil.readValue(json, ImageChangeDto.class);
        dto.getComponents().validate();
        ImageChangeDto restored = JsonUtil.readValue(JsonUtil.writeValueAsString(dto), ImageChangeDto.class);

        assertEquals("7.3.2", restored.getComponents().getRuntimeVersion());
        assertEquals("7.13.2.20000-83309838", restored.getComponents().repository().getFullVersion());
    }

    private ClouderaManagerV4Request validComponents() throws Exception {
        return JsonUtil.readValue("""
                {"repository":{"version":"7.13.2.20000-83309838","baseUrl":"https://build/cm/"},
                "products":[{"name":"CDH","version":"7.3.2-1.cdh7.3.2.p20000.83281140","parcel":"https://build/cdh/"}]}
                """, ClouderaManagerV4Request.class);
    }

}
