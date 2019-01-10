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
import com.sequenceiq.it.cloudbreak.newway.SshKey;
import com.sequenceiq.it.cloudbreak.newway.cloud.CloudProvider;
import com.sequenceiq.it.cloudbreak.newway.cloud.CloudProviderHelper;
import com.sequenceiq.it.cloudbreak.newway.cloud.OpenstackCloudProvider;

public class SshKeysTests extends CloudbreakTest {
    private static final String INVALID_CRED_NAME = "invalid-cred";

    private static final String INVALID_AV_ZONE = "invalid-avzone";

    private static final String INVALID_REGION = "invalid-region";

    private static final long INVALID_CRED_ID = 9999L;

    private static final Logger LOGGER = LoggerFactory.getLogger(SshKeysTests.class);

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
    public void testGetSshKeysWithCredendtialId() throws Exception {
        given(cloudProvider.aValidCredential());
        given(SshKey.request()
                .withCredentialName(credentialName)
                .withCredentialName("")
                .withRegion(cloudProvider.region()), "with credential id"
        );
        when(SshKey.get(), "get the request");
        then(SshKey.assertValidSshKeys(), "available sshkeys should be listed");
    }

    @Test
    public void testGetSshKeysWithCredendtialName() throws Exception {
        given(cloudProvider.aValidCredential());
        given(SshKey.request()
                .withCredentialName(cloudProvider.getCredentialName())
                .withRegion(cloudProvider.region()), "with credential name"
        );
        when(SshKey.get(), "get the request");
        then(SshKey.assertValidSshKeys(), "available sshkeys should be listed");
    }

    @Test
    public void testGetSshKeysWithAvZone() throws Exception {
        given(SshKey.request()
                .withCredentialName(cloudProvider.getCredentialName())
                .withRegion(cloudProvider.region())
                .withAvailabilityZone(cloudProvider.availabilityZone()), "with availability zone "
        );
        when(SshKey.get(), "get the request");
        then(SshKey.assertValidSshKeys(), "available sshkeys should be listed");
    }

    @Test
    public void testGetSshKeysWithInvalidAvZone() throws Exception {
        given(SshKey.request()
                .withCredentialName(cloudProvider.getCredentialName())
                .withRegion(cloudProvider.region())
                .withAvailabilityZone(INVALID_AV_ZONE), "with invalid availability zone "
        );
        when(SshKey.get(), "get the request");
        then(SshKey.assertValidSshKeys(), "available sshkeys should be listed");
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void testGetSshKeysInvalidCredName() throws Exception {
        given(SshKey.request()
                .withCredentialName(INVALID_CRED_NAME), "with invalid credential name"
        );
        when(SshKey.get(), "get the request");
        then(SshKey.assertValidSshKeys());
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void testGetSshKeysWithoutCred() throws Exception {
        given(SshKey.request()
                .withCredentialName(null)
                .withRegion(cloudProvider.region()), "without credential"
        );
        when(SshKey.get(), "get the request");
        then(SshKey.assertValidSshKeys());
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void testGetSshKeysInvalidCredId() throws Exception {
        given(SshKey.request()
                .withCredentialName(null)
                .withRegion(cloudProvider.region()),  "with invalid credential id"
        );
        when(SshKey.get(), "get the request");
        then(SshKey.assertValidSshKeys());
    }

    @Test
    public void testSshKeysInvalidRegion() throws Exception {
        given(SshKey.request()
                .withCredentialName(credentialName)
                .withRegion(INVALID_REGION), "with invalid credential id"
        );
        when(SshKey.get(), "get the request");
        then(SshKey.assertValidSshKeys(), "available sshkeys should be listed");
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