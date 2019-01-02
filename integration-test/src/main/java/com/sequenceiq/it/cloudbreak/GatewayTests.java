package com.sequenceiq.it.cloudbreak;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.Credential;
import com.sequenceiq.it.cloudbreak.newway.Gateway;
import com.sequenceiq.it.cloudbreak.newway.cloud.CloudProvider;
import com.sequenceiq.it.cloudbreak.newway.cloud.CloudProviderHelper;
import com.sequenceiq.it.cloudbreak.newway.cloud.OpenstackCloudProvider;

public class GatewayTests extends CloudbreakTest {
    private static final String INVALID_CRED_NAME = "invalid-cred";

    private static final String INVALID_AV_ZONE = "invalid-avzone";

    private static final String INVALID_REGION = "invalid-region";

    private static final long INVALID_CRED_ID = 9999L;

    private static final Logger LOGGER = LoggerFactory.getLogger(GatewayTests.class);

    private Long credentialId;

    private CloudProvider cloudProvider;

    @BeforeTest
    @Parameters("provider")
    public void beforeTest(@Optional(OpenstackCloudProvider.OPENSTACK) String provider) {
        LOGGER.info("before cluster test set provider: " + provider);
        if (cloudProvider == null) {
            cloudProvider = CloudProviderHelper.providerFactory(provider, getTestParameter());
        } else {
            LOGGER.info("cloud provider already set - running from factory test");
        }
    }

    @BeforeTest
    public void setup() throws  Exception {
        given(CloudbreakClient.created());
        given(cloudProvider.aValidCredential());

        IntegrationTestContext it = getItContext();
        credentialId  = Credential.getTestContextCredential().apply(it).getResponse().getId();
    }

    @Test
    public void testGetGatewaysWithCredendtialId() throws Exception {
        given(cloudProvider.aValidCredential());
        given(Gateway.request()
                .withCredentialName("")
                .withRegion(cloudProvider.region()), "with credential id"
        );
        when(Gateway.get(), "get the request");
        then(Gateway.assertValidGateways(), "available gateways should be listed");
    }

    @Test
    public void testGetGatewaysWithCredendtialName() throws Exception {
        given(cloudProvider.aValidCredential());
        given(Gateway.request()
                .withCredentialName(cloudProvider.getCredentialName())
                .withRegion(cloudProvider.region()), "with credential name"
        );
        when(Gateway.get(), "get the request");
        then(Gateway.assertValidGateways(), "available gateways should be listed");
    }

    @Test
    public void testGetGatewaysWithAvZone() throws Exception {
        given(Gateway.request()
                .withCredentialName(cloudProvider.getCredentialName())
                .withRegion(cloudProvider.region())
                .withAvailabilityZone(cloudProvider.availabilityZone()), "with availability zone "
        );
        when(Gateway.get(), "get the request");
        then(Gateway.assertValidGateways(), "available gateways should be listed");
    }

    @Test
    public void testGetGatewaysWithInvalidAvZone() throws Exception {
        given(Gateway.request()
                .withCredentialName(cloudProvider.getCredentialName())
                .withRegion(cloudProvider.region())
                .withAvailabilityZone(INVALID_AV_ZONE), "with invalid availability zone "
        );
        when(Gateway.get(), "get the request");
        then(Gateway.assertValidGateways(), "available gateways should be listed");
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void testGetGatewaysInvalidCredName() throws Exception {
        given(Gateway.request()
                .withCredentialName(INVALID_CRED_NAME), "with invalid credential name"
        );
        when(Gateway.get(), "get the request");
        then(Gateway.assertValidGateways());
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void testGetGatewaysWithoutCred() throws Exception {
        given(Gateway.request()
                .withCredentialName(null)
                .withRegion(cloudProvider.region()), "without credential"
        );
        when(Gateway.get(), "get the request");
        then(Gateway.assertValidGateways());
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void testGetGatewaysInvalidCredId() throws Exception {
        given(Gateway.request()
                .withCredentialName(null)
                .withRegion(cloudProvider.region()),  "with invalid credential id"
        );
        when(Gateway.get(), "get the request");
        then(Gateway.assertValidGateways());
    }

    @Test
    public void testGetGatewaysInvalidRegion() throws Exception {
        given(Gateway.request()
                .withRegion(INVALID_REGION), "with invalid credential id"
        );
        when(Gateway.get(), "get the request");
        then(Gateway.assertValidGateways(), "available gateways should be listed");
    }

    @AfterTest
    public void cleanUp() throws Exception {
        try {
            given(Credential.request()
                    .withName(cloudProvider.getCredentialName()));
            when(Credential.delete());
        } catch (ForbiddenException e) {
            String errorMessage;
            errorMessage = e.getResponse().readEntity(String.class).substring(e.getResponse().readEntity(String.class).lastIndexOf(':') + 1);
            LOGGER.info("ForbiddenException message ::: " + errorMessage);
        }
    }
}