package com.sequenceiq.cloudbreak.common.imdupdate;

import static com.sequenceiq.cloudbreak.common.imdupdate.InstanceMetadataUpdateType.IMDS_HTTP_TOKEN_OPTIONAL;
import static com.sequenceiq.cloudbreak.common.imdupdate.InstanceMetadataUpdateType.IMDS_HTTP_TOKEN_REQUIRED;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AWS;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AZURE;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;

class InstanceMetadataUpdatePropertiesTest {

    @Test
    void testValidations() {
        assertThrows(CloudbreakServiceException.class,
                () -> properties(Map.of(IMDS_HTTP_TOKEN_REQUIRED, property(AWS, "v2"))).validateUpdateType(IMDS_HTTP_TOKEN_OPTIONAL, AWS));
        assertThrows(CloudbreakServiceException.class,
                () -> properties(Map.of(IMDS_HTTP_TOKEN_REQUIRED, property(AWS, "v2"))).validateUpdateType(IMDS_HTTP_TOKEN_REQUIRED, AZURE));
        assertThrows(CloudbreakServiceException.class,
                () -> properties(Map.of(IMDS_HTTP_TOKEN_REQUIRED, falseProperty(AWS))).validateUpdateType(IMDS_HTTP_TOKEN_REQUIRED, AZURE));
    }

    private InstanceMetadataUpdateTypeProperty property(CloudPlatform cloudPlatform, String imdsVersion) {
        InstanceMetadataUpdateTypeMetadata metadata = new InstanceMetadataUpdateTypeMetadata(imdsVersion);
        return new InstanceMetadataUpdateTypeProperty(cloudPlatform.name(), Map.of(cloudPlatform, metadata));
    }

    private InstanceMetadataUpdateTypeProperty falseProperty(CloudPlatform cloudPlatform) {
        return new InstanceMetadataUpdateTypeProperty(cloudPlatform.name(), Map.of());
    }

    private InstanceMetadataUpdateProperties properties(Map<InstanceMetadataUpdateType, InstanceMetadataUpdateTypeProperty> types) {
        InstanceMetadataUpdateProperties instanceMetadataUpdateProperties = new InstanceMetadataUpdateProperties();
        instanceMetadataUpdateProperties.setTypes(types);
        return instanceMetadataUpdateProperties;
    }
}
