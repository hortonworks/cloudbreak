package com.sequenceiq.cloudbreak.cloud.init;

import static com.sequenceiq.cloudbreak.cloud.model.Platform.platform;
import static com.sequenceiq.cloudbreak.cloud.model.Variant.variant;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.CloudConstant;
import com.sequenceiq.cloudbreak.cloud.CredentialConnector;
import com.sequenceiq.cloudbreak.cloud.EncryptionResources;
import com.sequenceiq.cloudbreak.cloud.InstanceConnector;
import com.sequenceiq.cloudbreak.cloud.MetadataCollector;
import com.sequenceiq.cloudbreak.cloud.NetworkConnector;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.PlatformResources;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.ResourceVolumeConnector;
import com.sequenceiq.cloudbreak.cloud.Setup;
import com.sequenceiq.cloudbreak.cloud.Validator;
import com.sequenceiq.cloudbreak.cloud.ValidatorType;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

class CloudPlatformConnectorsTest {

    private final CloudPlatformConnectors c = new CloudPlatformConnectors();

    @BeforeEach
    void setUp() {
        List<CloudConnector> connectorList = Lists.newArrayList();
        connectorList.add(getConnector("MULTIWITHDEFAULT", "ONE"));
        connectorList.add(getConnector("MULTIWITHDEFAULT", "TWO"));
        connectorList.add(getConnector("SINGLE", "SINGLE"));
        ReflectionTestUtils.setField(c, "cloudConnectors", connectorList);
        ReflectionTestUtils.setField(c, "platformDefaultVariants", "MULTIWITHDEFAULT:ONE");
        c.cloudPlatformConnectors();
    }

    @Test
    void getDefaultForGcp() {
        CloudConnector conn = c.getDefault(platform("SINGLE"));
        assertEquals("SINGLE", conn.variant().value());
    }

    @Test
    void getWithNullVariant() {
        CloudConnector conn = c.get(platform("MULTIWITHDEFAULT"), variant(null));
        //should fall back to default
        assertEquals("ONE", conn.variant().value());
    }

    @Test
    void getWithEmptyVariant() {
        CloudConnector conn = c.get(platform("MULTIWITHDEFAULT"), variant(""));
        //should fall back to default
        assertEquals("ONE", conn.variant().value());
    }

    @Test
    void getConnectorDefaultWithNoDefault() {
        List<CloudConnector> connectorList = Lists.newArrayList();
        connectorList.add(getConnector("NODEFAULT", "ONE"));
        connectorList.add(getConnector("NODEFAULT", "TWO"));
        ReflectionTestUtils.setField(c, "cloudConnectors", connectorList);
        assertThrows(IllegalStateException.class, () -> c.cloudPlatformConnectors(),
                "No default variant is specified for platform: 'StringType{value='NODEFAULT'}'");
    }

    private CloudConnector getConnector(String platform, String variant) {
        return new FakeCloudConnector(variant, platform);
    }

    private static class FakeCloudConnector implements CloudConnector {
        private final String variant;

        private final String platform;

        FakeCloudConnector(String variant, String platform) {
            this.variant = variant;
            this.platform = platform;
        }

        @Override
        public Authenticator authentication() {
            return null;
        }

        @Override
        public Setup setup() {
            return null;
        }

        @Override
        public List<Validator> validators(ValidatorType validatorType) {
            return Collections.emptyList();
        }

        @Override
        public CredentialConnector credentials() {
            return null;
        }

        @Override
        public ResourceConnector resources() {
            return null;
        }

        @Override
        public InstanceConnector instances() {
            return null;
        }

        @Override
        public MetadataCollector metadata() {
            return null;
        }

        @Override
        public PlatformParameters parameters() {
            return null;
        }

        @Override
        public PlatformResources platformResources() {
            return null;
        }

        @Override
        public CloudConstant cloudConstant() {
            return null;
        }

        @Override
        public Variant variant() {
            return Variant.variant(variant);
        }

        @Override
        public Platform platform() {
            return Platform.platform(platform);
        }

        @Override
        public NetworkConnector networkConnector() {
            return null;
        }

        @Override
        public String regionToDisplayName(String region) {
            return region;
        }

        @Override
        public ResourceVolumeConnector volumeConnector() {
            return null;
        }

        @Override
        public EncryptionResources encryptionResources() {
            return null;
        }
    }
}
