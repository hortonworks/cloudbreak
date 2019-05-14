package com.sequenceiq.cloudbreak.cloud.init;

import static com.sequenceiq.cloudbreak.cloud.model.Platform.platform;
import static com.sequenceiq.cloudbreak.cloud.model.Variant.variant;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.CloudConstant;
import com.sequenceiq.cloudbreak.cloud.CredentialConnector;
import com.sequenceiq.cloudbreak.cloud.InstanceConnector;
import com.sequenceiq.cloudbreak.cloud.MetadataCollector;
import com.sequenceiq.cloudbreak.cloud.NetworkConnector;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.PlatformResources;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.Setup;
import com.sequenceiq.cloudbreak.cloud.Validator;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

public class CloudPlatformConnectorsTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    private final CloudPlatformConnectors c = new CloudPlatformConnectors();

    @Before
    public void setUp() {
        List<CloudConnector> connectorList = Lists.newArrayList();
        connectorList.add(getConnector("MULTIWITHDEFAULT", "ONE"));
        connectorList.add(getConnector("MULTIWITHDEFAULT", "TWO"));
        connectorList.add(getConnector("SINGLE", "SINGLE"));
        ReflectionTestUtils.setField(c, "cloudConnectors", connectorList);
        ReflectionTestUtils.setField(c, "platformDefaultVariants", "MULTIWITHDEFAULT:ONE");
        c.cloudPlatformConnectors();
    }

    @Test
    public void getDefaultForOpenstack() {
        CloudConnector conn = c.getDefault(platform("MULTIWITHDEFAULT"));
        assertEquals("ONE", conn.variant().value());
    }

    @Test
    public void getDefaultForGcp() {
        CloudConnector conn = c.getDefault(platform("SINGLE"));
        assertEquals("SINGLE", conn.variant().value());
    }

    @Test
    public void getOpenstackNative() {
        CloudConnector conn = c.get(platform("MULTIWITHDEFAULT"), variant("TWO"));
        assertEquals("TWO", conn.variant().value());
    }

    @Test
    public void getWithNullVariant() {
        CloudConnector conn = c.get(platform("MULTIWITHDEFAULT"), variant(null));
        //should fall back to default
        assertEquals("ONE", conn.variant().value());
    }

    @Test
    public void getWithEmptyVariant() {
        CloudConnector conn = c.get(platform("MULTIWITHDEFAULT"), variant(""));
        //should fall back to default
        assertEquals("ONE", conn.variant().value());
    }

    @Test
    public void getConnectorDefaultWithNoDefault() {
        List<CloudConnector> connectorList = Lists.newArrayList();
        connectorList.add(getConnector("NODEFAULT", "ONE"));
        connectorList.add(getConnector("NODEFAULT", "TWO"));
        ReflectionTestUtils.setField(c, "cloudConnectors", connectorList);
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage(is("No default variant is specified for platform: 'StringType{value='NODEFAULT'}'"));
        c.cloudPlatformConnectors();
    }

    private CloudConnector getConnector(String platform, String variant) {
        return new FakeCloudConnector(variant, platform);
    }

    private static class FakeCloudConnector implements CloudConnector<Object> {
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
        public List<Validator> validators() {
            return Collections.emptyList();
        }

        @Override
        public CredentialConnector credentials() {
            return null;
        }

        @Override
        public ResourceConnector<Object> resources() {
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
        public NetworkConnector networkConnector() {
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
    }
}