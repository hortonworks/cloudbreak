package com.sequenceiq.it.cloudbreak;

import java.util.Set;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.model.CredentialResponse;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.Credential;
import com.sequenceiq.it.cloudbreak.newway.Networks;
import com.sequenceiq.it.cloudbreak.newway.cloud.CloudProvider;
import com.sequenceiq.it.cloudbreak.newway.cloud.CloudProviderHelper;
import com.sequenceiq.it.cloudbreak.newway.cloud.OpenstackCloudProvider;

public class NetworksTest extends CloudbreakTest {
    private static final String VALID_CRED_NAME = "valid-cred-";

    private static final String INVALID_CRED_NAME = "invalid-cred";

    private static final String INVALID_AV_ZONE = "invalid-avzone";

    private static final long INVALID_CRED_ID = 9999L;

    private static final Logger LOGGER = LoggerFactory.getLogger(NetworksTest.class);

    private String credentialName = "";

    private Long credentialId;

    private CloudProvider cloudProvider;

    @BeforeTest
    @Parameters("provider")
    public void beforeTest(@Optional(OpenstackCloudProvider.OPENSTACK) String provider) {
        LOGGER.info("before cluster test set provider: " + provider);
        if (cloudProvider != null) {
            LOGGER.info("cloud provider: "  + cloudProvider + " already set - running from factory test");
            return;
        }
        cloudProvider = CloudProviderHelper.providerFactory(provider, getTestParameter())[0];
    }

    @BeforeTest
    public void setup() throws  Exception {
        credentialName = VALID_CRED_NAME + cloudProvider.getPlatform().toLowerCase() + "-nets";
        given(CloudbreakClient.isCreated());
        given(cloudProvider.aValidCredential()
                .withName(credentialName)
                .withCloudPlatform(cloudProvider.getPlatform()), credentialName + " credential is given");
        getCredentialId();
    }

    @Test
    public void testGetNetworksWithCredName() throws Exception {
        given(Networks.request()
                .withCredentialName(credentialName)
                .withRegion(cloudProvider.region()), "with credential name"
        );
        when(Networks.post(), "post the request");
        then(Networks.assertNameNotEmpty(), "available networks should be listed");
    }

    @Test
    public void testGetNetworksWithCredId() throws Exception {
        given(Networks.request()
                        .withCredentialId(credentialId)
                        .withRegion(cloudProvider.region()), "with credential id"
        );
        when(Networks.post(), "post the request");
        then(Networks.assertNameNotEmpty(), "available networks should be listed");
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void testGetNetworksCredIsMissing() throws Exception {
        given(Networks.request()
                        .withCredentialName("")
                        .withRegion(cloudProvider.region()), "with no credential"
        );
        when(Networks.post());
        then(Networks.assertNameEmpty());
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void testGetNetworksInvalidCredName() throws Exception {
        given(Networks.request()
                        .withCredentialName(INVALID_CRED_NAME)
                        .withRegion(cloudProvider.region()), "with invalid credential name"
        );
        when(Networks.post());
        then(Networks.assertNameEmpty());
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void testGetNetworksInvalidCredId() throws Exception {
        given(Networks.request()
                .withCredentialId(INVALID_CRED_ID)
                .withRegion(cloudProvider.region()), "with no existing credential id"
        );
        when(Networks.post(), "post the request");
        then(Networks.assertNameEmpty());
    }

    @Test
    public void testGetNetworksWithInvalidAvZone() throws Exception {
        given(Networks.request()
                .withCredentialId(credentialId)
                .withRegion(cloudProvider.region())
                .withAvailabilityZone(INVALID_AV_ZONE), "get networks with invalid availability zone"
        );
        when(Networks.post(), "post the request");
        then(Networks.assertNameNotEmpty(), "available networks should be listed");
    }

    @Test
    public void testGetNetworksWithAvZoneEmpty() throws Exception {
        given(Networks.request()
                .withCredentialId(credentialId)
                .withRegion(cloudProvider.region())
                .withAvailabilityZone(""), "get networks with empty availability zone"
        );
        when(Networks.post(), "post the request");
        then(Networks.assertNameNotEmpty(), "available networks should be listed");
    }

    private void getCredentialId() throws Exception {
        given(Credential.request()
                .withName(credentialName)
        );

        when(Credential.getAll());
        then(Credential.assertThis(
                (credential, t) -> {
                    Set<CredentialResponse>  credentialResponses = credential.getResponses();
                    for (CredentialResponse response : credentialResponses) {
                        if (response.getName().equals(credentialName)) {
                            credentialId = response.getId();
                        }
                    }
                })
        );
    }

    @AfterTest
    public void cleanUp() throws Exception {
        try {
            given(Credential.request()
                    .withName(VALID_CRED_NAME + cloudProvider.getPlatform().toLowerCase() + "-nets"));
            when(Credential.delete());
        } catch (ForbiddenException e) {
            String errorMessage;
            errorMessage = e.getResponse().readEntity(String.class).substring(e.getResponse().readEntity(String.class).lastIndexOf(':') + 1);
            LOGGER.info("ForbiddenException message ::: " + errorMessage);
        }
    }
}