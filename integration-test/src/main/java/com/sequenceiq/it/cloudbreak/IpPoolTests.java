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
import com.sequenceiq.it.cloudbreak.newway.IpPool;
import com.sequenceiq.it.cloudbreak.newway.cloud.CloudProvider;
import com.sequenceiq.it.cloudbreak.newway.cloud.CloudProviderHelper;
import com.sequenceiq.it.cloudbreak.newway.cloud.OpenstackCloudProvider;

public class IpPoolTests extends CloudbreakTest {
    private static final String INVALID_CRED_NAME = "invalid-cred";

    private static final String INVALID_AV_ZONE = "invalid-avzone";

    private static final long INVALID_CRED_ID = 9999L;

    private static final Logger LOGGER = LoggerFactory.getLogger(IpPoolTests.class);

    private String credentialName;

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
        credentialName  = Credential.getTestContextCredential().apply(it).getResponse().getName();
    }

    @Test
    public void testGetIpPoolsWithCredentialId() throws Exception {
        given(IpPool.request()
                .withCredentialName(credentialName)
                .withCredentialName("")
                .withRegion(cloudProvider.region()), "with credential id"
        );
        when(IpPool.get(), "get the request");
        then(IpPool.assertValidIpPool(), "available ipPools should be listed");
    }

    @Test
    public void testGetIpPoolsWithCredentialName() throws Exception {
        given(IpPool.request()
                .withCredentialName(cloudProvider.getCredentialName()), "with credential name"
        );
        when(IpPool.get(), "get the request");
        then(IpPool.assertValidIpPool(), "available ipPools should be listed");
    }

    @Test
    public void testGetIpPoolsWithAvZone() throws Exception {
        given(IpPool.request()
                .withCredentialName(cloudProvider.getCredentialName())
                .withAvailabilityZone(cloudProvider.availabilityZone()), "with availability zone "
        );
        when(IpPool.get(), "get the request");
        then(IpPool.assertValidIpPool(), "available ipPools should be listed");
    }

    @Test
    public void testGetIpPoolsWithInvalidAvZone() throws Exception {
        given(IpPool.request()
                .withCredentialName(cloudProvider.getCredentialName())
                .withAvailabilityZone(INVALID_AV_ZONE), "with invalid availability zone "
        );
        when(IpPool.get(), "get the request");
        then(IpPool.assertValidIpPool(), "available ipPools should be listed");
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void testGetIpPoolsCredIsMissing() throws Exception {
        given(IpPool.request()
                .withCredentialName(null), "without credential"
        );
        when(IpPool.get(), "get the request");
        then(IpPool.assertValidIpPool());
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void testGetIpPoolsInvalidCredId() throws Exception {
        given(IpPool.request()
                .withCredentialName(null), "with invalid credential id"
        );
        when(IpPool.get(), "get the request");
        then(IpPool.assertValidIpPool());
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void testGetIpPoolsInvalidCredName() throws Exception {
        given(IpPool.request()
                .withCredentialName(INVALID_CRED_NAME), "with invalid credential name"
        );
        when(IpPool.get(), "get the request");
        then(IpPool.assertValidIpPool());
    }

    @Test
    public void testGetIpPoolsInvalidRegion() throws Exception {
        given(IpPool.request()
                .withCredentialName(cloudProvider.getCredentialName()), "with invalid region"
        );
        when(IpPool.get(), "get the request");
        then(IpPool.assertValidIpPool(), "available ipPools should be listed");
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