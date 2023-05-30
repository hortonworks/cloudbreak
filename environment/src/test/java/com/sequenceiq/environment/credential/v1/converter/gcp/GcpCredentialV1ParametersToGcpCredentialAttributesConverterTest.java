package com.sequenceiq.environment.credential.v1.converter.gcp;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.environment.api.v1.credential.model.parameters.gcp.GcpCredentialParameters;
import com.sequenceiq.environment.credential.attributes.gcp.GcpCredentialAttributes;
import com.sequenceiq.environment.credential.attributes.gcp.JsonAttributes;
import com.sequenceiq.environment.credential.attributes.gcp.P12Attributes;

class GcpCredentialV1ParametersToGcpCredentialAttributesConverterTest {

    private static final String MY_PROJECT_ID = "myProjectId";

    private static final Json RAW_JSON = new Json("""
            {
                "projectId": "%s"
            }""".formatted(MY_PROJECT_ID));

    private final GcpCredentialV1ParametersToGcpCredentialAttributesConverter underTest = new GcpCredentialV1ParametersToGcpCredentialAttributesConverter();

    @Test
    void testConvertProjectIdJson() {
        GcpCredentialAttributes source = new GcpCredentialAttributes();
        source.setJson(new JsonAttributes());
        GcpCredentialParameters result = underTest.convert(source, RAW_JSON);
        assertThat(result.getJson().getProjectId()).isEqualTo(MY_PROJECT_ID);
    }

    @Test
    void testConvertProjectIdP12() {
        GcpCredentialAttributes source = new GcpCredentialAttributes();
        source.setP12(new P12Attributes());
        GcpCredentialParameters result = underTest.convert(source, RAW_JSON);
        assertThat(result.getP12().getProjectId()).isEqualTo(MY_PROJECT_ID);
    }

}
