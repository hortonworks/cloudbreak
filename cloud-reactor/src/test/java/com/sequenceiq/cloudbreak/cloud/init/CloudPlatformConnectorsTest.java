package com.sequenceiq.cloudbreak.cloud.init;

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
        CloudConnector conn = c.getDefault("MULTIWITHDEFAULT");
        assertEquals("ONE", conn.variant());
    }

    @Test
    public void getDefaultForGcp() {
        CloudConnector conn = c.getDefault("SINGLE");
        assertEquals("SINGLE", conn.variant());
    }

    @Test
    public void getOpenstackNative() {
        CloudConnector conn = c.get("MULTIWITHDEFAULT", "TWO");
        assertEquals("TWO", conn.variant());
    }

    @Test
    public void getWithNullVariant() {
        CloudConnector conn = c.get("MULTIWITHDEFAULT", null);
        //should fall back to default
        assertEquals("ONE", conn.variant());
    }

    @Test
    public void getWithEmptyVariant() {
        CloudConnector conn = c.get("MULTIWITHDEFAULT", "");
        //should fall back to default
        assertEquals("ONE", conn.variant());
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
            public String variant() {
                return variant;
            }

            @Override
            public String platform() {
                return platform;
            }
        };
    }
}