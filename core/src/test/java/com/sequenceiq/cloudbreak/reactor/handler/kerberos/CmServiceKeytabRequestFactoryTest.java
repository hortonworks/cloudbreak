package com.sequenceiq.cloudbreak.reactor.handler.kerberos;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.ServiceKeytabRequest;

@RunWith(MockitoJUnitRunner.class)
public class CmServiceKeytabRequestFactoryTest {

    @InjectMocks
    private CmServiceKeytabRequestFactory underTest;

    @Test
    public void testAliasCreationWithFQDN() {
        Stack stack = TestUtil.stack();
        GatewayConfig primaryGatewayConfig = create("fqdn.stest.cloudera.site");

        ServiceKeytabRequest request = underTest.create(stack, primaryGatewayConfig);

        assertEquals("CM", request.getServiceName());
        assertEquals("fqdn.stest.cloudera.site", request.getServerHostName());
        assertEquals("fqdn", request.getServerHostNameAlias());

    }

    @Test
    public void testAliasCreationWithHostname() {
        Stack stack = TestUtil.stack();
        GatewayConfig primaryGatewayConfig = create("fqdn");

        ServiceKeytabRequest request = underTest.create(stack, primaryGatewayConfig);

        assertEquals("CM", request.getServiceName());
        assertEquals("fqdn", request.getServerHostName());
        assertNull("Alias shall be null if it is identical with fqdn",
                request.getServerHostNameAlias());

    }

    private GatewayConfig create(String fqdn) {
        return new GatewayConfig("1.1.1.1", "10.0.0.1",
                "172.16.252.43", fqdn,
                9443, "instanceid", "servercert", "clientcert", "clientkey",
                "saltpasswd", "saltbootpassword", "signkey", false,
                true, "privatekey", "publickey", null, null, null);

    }
}
