package com.sequenceiq.cloudbreak.reactor.handler.kerberos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.ServiceKeytabRequest;

class CmServiceKeytabRequestFactoryTest {

    private CmServiceKeytabRequestFactory underTest = new CmServiceKeytabRequestFactory();

    @Test
    void testAliasCreationWithFQDN() {
        Stack stack = TestUtil.stack();
        GatewayConfig primaryGatewayConfig = create("fqdn.stest.cloudera.site");

        ServiceKeytabRequest request = underTest.create(stack, primaryGatewayConfig, false);

        assertEquals("CM", request.getServiceName());
        assertEquals("fqdn.stest.cloudera.site", request.getServerHostName());
        assertEquals("fqdn", request.getServerHostNameAlias());
        assertTrue(request.getDoNotRecreateKeytab());
    }

    @Test
    void testAliasCreationWithFQDNWithRepairFlag() {
        Stack stack = TestUtil.stack();
        GatewayConfig primaryGatewayConfig = create("fqdn.stest.cloudera.site");

        ServiceKeytabRequest request = underTest.create(stack, primaryGatewayConfig, true);

        assertEquals("CM", request.getServiceName());
        assertEquals("fqdn.stest.cloudera.site", request.getServerHostName());
        assertEquals("fqdn", request.getServerHostNameAlias());
        assertFalse(request.getDoNotRecreateKeytab());
    }

    @Test
    void testAliasCreationWithHostname() {
        Stack stack = TestUtil.stack();
        GatewayConfig primaryGatewayConfig = create("fqdn");

        ServiceKeytabRequest request = underTest.create(stack, primaryGatewayConfig, false);

        assertEquals("CM", request.getServiceName());
        assertEquals("fqdn", request.getServerHostName());
        assertNull(request.getServerHostNameAlias(), "Alias shall be null if it is identical with fqdn");

    }

    private GatewayConfig create(String fqdn) {
        return GatewayConfig.builder()
                .withConnectionAddress("172.16.252.43")
                .withPublicAddress("1.1.1.1")
                .withPrivateAddress("10.0.0.1")
                .withServerCert("servercert")
                .withClientCert("clientcert")
                .withClientKey("clientkey")
                .withSaltBootPassword("saltbootpassword")
                .withSaltPassword("saltpasswd")
                .withSaltSignPublicKey("signkey")
                .withSaltMasterPublicKey("masterPublicKey")
                .withSaltMasterPrivateKey("masterPrivateKey")
                .withSaltMasterPrivateKey("privatekey")
                .withSaltMasterPublicKey("publickey")
                .withGatewayPort(9443)
                .withInstanceId("instanceid")
                .withHostname(fqdn)
                .withKnoxGatewayEnabled(false)
                .build();

    }
}
