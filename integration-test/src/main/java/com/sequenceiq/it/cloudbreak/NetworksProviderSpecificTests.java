package com.sequenceiq.it.cloudbreak;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.model.CredentialResponse;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.Credential;
import com.sequenceiq.it.cloudbreak.newway.Networks;
import com.sequenceiq.it.cloudbreak.newway.cloud.AwsCloudProvider;
import com.sequenceiq.it.cloudbreak.newway.cloud.CloudProvider;
import com.sequenceiq.it.cloudbreak.newway.cloud.CloudProviderHelper;
import com.sequenceiq.it.cloudbreak.newway.cloud.GcpCloudProvider;

public class NetworksProviderSpecificTests extends CloudbreakTest {

    private static final String VALID_CRED_NAME = "cred-";

    private static final String INVALID_REGION = "invalid-region";

    private static final Logger LOGGER = LoggerFactory.getLogger(NetworksProviderSpecificTests.class);

    private final Map<String, String> credentialIdMap = new HashMap<>();

    @BeforeTest
    public void beforeTest() throws Exception {
        CloudProvider cloudProvider;
        String[] providers = {"aws", "azure", "openstack", "gcp"};
        for (String provider : providers) {
            cloudProvider = CloudProviderHelper.providerFactory(provider, getTestParameter());
            given(CloudbreakClient.created());
            given(cloudProvider.aValidCredential()
                    .withName(VALID_CRED_NAME + provider + "-spec")
                    .withDescription("")
                    .withCloudPlatform(cloudProvider.getPlatform()), provider + " credential is created.");
            getCredentialId(provider, VALID_CRED_NAME + provider + "-spec");
        }
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void testGetNetworksInvalidRegionAWS() throws Exception {
        given(Networks.request()
                .withCredentialName(credentialIdMap.get("aws"))
                .withRegion(INVALID_REGION), "get networks with invalid region"
        );
        when(Networks.post(), "post the request");
        then(Networks.assertNetworksEmpty());
    }

    @Test
    public void testGetNetworksInvalidRegionAZURE() throws Exception {
        given(Networks.request()
                .withCredentialName(credentialIdMap.get("azure"))
                .withRegion(INVALID_REGION), "get networks with invalid region"
        );
        when(Networks.post(), "post the request");
        then(Networks.assertNetworksEmpty());
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void testGetNetworksInvalidRegionGCP() throws Exception {
        given(Networks.request()
                .withCredentialName(credentialIdMap.get("gcp"))
                .withRegion(INVALID_REGION), "get networks with invalid region"
        );
        when(Networks.post(), "post the request");
        then(Networks.assertNetworksEmpty());
    }

    @Test
    public void testGetNetworksInvalidRegionOs() throws Exception {
        given(Networks.request()
                .withCredentialName(credentialIdMap.get("openstack"))
                .withRegion(INVALID_REGION), "get networks with invalid region"
        );
        when(Networks.post(), "post the request");
        then(Networks.assertNameNotEmpty());
    }

    @Test
    public void testGetNetworksInvalidAvZoneGCP() throws Exception {
        GcpCloudProvider gcpCloudProvider = new GcpCloudProvider(getTestParameter());
        given(Networks.request()
                .withCredentialName(credentialIdMap.get("gcp"))
                .withRegion(gcpCloudProvider.region())
                .withAvailabilityZone(null), "get networks with availability zone"
        );
        when(Networks.post(), "post the request");
        then(Networks.assertNameNotEmpty());
    }

    @Test
    public void testGetNetworksInvalidAvZoneAzure() throws Exception {
        given(Networks.request()
                .withCredentialName(credentialIdMap.get("azure"))
                .withAvailabilityZone(null), "get networks with availability zone"
        );
        when(Networks.post(), "post the request");
        then(Networks.assertNameNotEmpty());
    }

    @Test
    public void testGetNetworksInvalidAvZoneAWS() throws Exception {
        AwsCloudProvider awsCloudProvider = new AwsCloudProvider(getTestParameter());
        given(Networks.request()
                .withCredentialName(credentialIdMap.get("aws"))
                .withRegion(awsCloudProvider.region())
                .withAvailabilityZone(null), "get networks with availability zone"
        );
        when(Networks.post(), "post the request");
        then(Networks.assertNameNotEmpty());
    }

    @Test
    public void testGetNetworksInvalidAvZonOS() throws Exception {
        given(Networks.request()
                .withCredentialName(credentialIdMap.get("openstack"))
                .withAvailabilityZone(null), "get networks with availability zone"
        );
        when(Networks.post(), "post the request");
        then(Networks.assertNameNotEmpty());
    }

    private void getCredentialId(String provider, String credentialName) throws Exception {
        given(Credential.request()
                .withName(credentialName)
        );
        when(Credential.getAll());
        then(Credential.assertThis(
                (credential, t) -> {
                    Set<CredentialResponse> credentialResponses = credential.getResponses();
                    for (CredentialResponse response : credentialResponses) {
                        if (response.getName().equals(credentialName)) {
                            credentialIdMap.put(provider, response.getName());
                        }
                    }
                })
        );
    }

    @AfterTest
    public void cleanUp() throws Exception {
        String[] nameArray = {VALID_CRED_NAME + "azure-spec", VALID_CRED_NAME + "aws-spec", VALID_CRED_NAME + "gcp-spec",
                VALID_CRED_NAME + "openstack-spec"};
        for (String name : nameArray) {
            try {
                given(CloudbreakClient.created());
                given(Credential.request()
                        .withName(name));
                when(Credential.delete());
            } catch (ForbiddenException e) {
                String errorMessage;
                errorMessage = e.getResponse().readEntity(String.class).substring(e.getResponse().readEntity(String.class).lastIndexOf(':') + 1);
                LOGGER.info("ForbiddenException message ::: " + errorMessage);
            }
        }
    }
}