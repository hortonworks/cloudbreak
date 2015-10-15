package com.sequenceiq.cloudbreak.cloud.init;

import static com.sequenceiq.cloudbreak.cloud.model.Platform.platform;
import static com.sequenceiq.cloudbreak.cloud.model.Variant.variant;
import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.CredentialConnector;
import com.sequenceiq.cloudbreak.cloud.InstanceConnector;
import com.sequenceiq.cloudbreak.cloud.MetadataCollector;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.Setup;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

public class CloudPlatformConnectorsTest {

    private CloudPlatformConnectors c = new CloudPlatformConnectors();

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

    @Test(expected = IllegalStateException.class)
    public void getConnectorDefaultWithNoDefault() {
        List<CloudConnector> connectorList = Lists.newArrayList();
        connectorList.add(getConnector("NODEFAULT", "ONE"));
        connectorList.add(getConnector("NODEFAULT", "TWO"));
        ReflectionTestUtils.setField(c, "cloudConnectors", connectorList);
        c.cloudPlatformConnectors();
    }

    private CloudConnector getConnector(final String platform, final String variant) {
        return new CloudConnector() {
            @Override
            public Authenticator authentication() {
                return null;
            }

            @Override
            public Setup setup() {
                return null;
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
            public Variant variant() {
                return Variant.variant(variant);
            }

            @Override
            public Platform platform() {
                return Platform.platform(platform);
            }
        };
    }
}