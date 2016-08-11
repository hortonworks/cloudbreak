package com.sequenceiq.it.cloudbreak;

import static com.sequenceiq.it.cloudbreak.GcpCreateVirtualNetworkTest.NetworkType.EXISTING_SUBNET_IN_EXISTING_NETWORK;
import static com.sequenceiq.it.cloudbreak.GcpCreateVirtualNetworkTest.NetworkType.LAGACY_NETWORK;
import static com.sequenceiq.it.cloudbreak.GcpCreateVirtualNetworkTest.NetworkType.NEW_SUBNET_IN_EXISTING_NETWORK;

import java.io.ByteArrayInputStream;
import java.security.PrivateKey;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.SecurityUtils;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.ComputeScopes;
import com.google.api.services.compute.model.Network;
import com.google.api.services.compute.model.Operation;
import com.google.api.services.compute.model.Subnetwork;
import com.sequenceiq.cloudbreak.api.model.NetworkJson;
import com.sequenceiq.it.util.ResourceUtil;

public class GcpCreateVirtualNetworkTest extends AbstractCloudbreakIntegrationTest {

    private static final int MAX_TRY = 30;
    private static final Logger LOGGER = LoggerFactory.getLogger(GcpCreateVirtualNetworkTest.class);

    @Value("${integrationtest.gcpcredential.name}")
    private String defaultName;
    @Value("${integrationtest.gcpcredential.projectId}")
    private String defaultProjectId;
    @Value("${integrationtest.gcpcredential.serviceAccountId}")
    private String defaultServiceAccountId;
    @Value("${integrationtest.gcpcredential.p12File}")
    private String defaultP12File;
    private JacksonFactory jsonFactory;

    @Test
    @Parameters({"networkName", "description", "publicInAccount", "resourceGroupName", "vpcName", "vpcSubnet", "subnetCIDR", "networkType"})
    public void createNetwork(String networkName, @Optional("") String description, @Optional("false") boolean publicInAccount,
            @Optional("europe-west1") String subnetRegion, @Optional("it-vpc") String vpcName,
            @Optional("it-vpc-subnet") String vpcSubnet, @Optional("10.0.36.0/24") String subnetCIDR, NetworkType networkType) throws Exception {
        String serviceAccountPrivateKey = ResourceUtil.readBase64EncodedContentFromResource(applicationContext, defaultP12File);
        HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        PrivateKey privateKey = SecurityUtils.loadPrivateKeyFromKeyStore(SecurityUtils.getPkcs12KeyStore(),
                new ByteArrayInputStream(Base64.decodeBase64(serviceAccountPrivateKey)), "notasecret", "privatekey", "notasecret");
        jsonFactory = JacksonFactory.getDefaultInstance();
        GoogleCredential googleCredential = new GoogleCredential.Builder().setTransport(httpTransport)
                .setJsonFactory(jsonFactory)
                .setServiceAccountId(defaultServiceAccountId)
                .setServiceAccountScopes(Collections.singletonList(ComputeScopes.COMPUTE))
                .setServiceAccountPrivateKey(privateKey)
                .build();

        Compute compute = new Compute.Builder(httpTransport, jsonFactory, null)
                .setApplicationName(defaultName)
                .setHttpRequestInitializer(googleCredential)
                .build();

        Network gcpNetwork = new Network();
        gcpNetwork.setName(vpcName);
        if (!LAGACY_NETWORK.equals(networkType)) {
            gcpNetwork.setAutoCreateSubnetworks(false);
        }

        Compute.Networks.Insert networkInsert = compute.networks().insert(defaultProjectId, gcpNetwork);

        Operation networkInsertResponse = networkInsert.execute();

        if (networkInsertResponse.getHttpErrorStatusCode() != null) {
            throw new IllegalStateException("gcp network operation failed: " + networkInsertResponse.getHttpErrorMessage());
        }

        waitOperation(compute, networkInsertResponse);

        if (EXISTING_SUBNET_IN_EXISTING_NETWORK.equals(networkType)) {
            Subnetwork gcpSubnet = new Subnetwork();
            gcpSubnet.setName(vpcSubnet);
            gcpSubnet.setIpCidrRange(subnetCIDR);
            gcpSubnet.setNetwork(String.format("https://www.googleapis.com/compute/v1/projects/%s/global/networks/%s", defaultProjectId, vpcName));
            Compute.Subnetworks.Insert subNetworkInsert = compute.subnetworks().insert(defaultProjectId, subnetRegion, gcpSubnet);
            Operation subNetInsertResponse = subNetworkInsert.execute();
            if (subNetInsertResponse.getHttpErrorStatusCode() != null) {
                throw new IllegalStateException("gcp subnetwork operation failed: " + subNetInsertResponse.getHttpErrorMessage());
            }
        }

        NetworkJson networkJson = new NetworkJson();
        networkJson.setName(networkName);
        networkJson.setDescription(description);
        if (NEW_SUBNET_IN_EXISTING_NETWORK.equals(networkType)) {
            networkJson.setSubnetCIDR(subnetCIDR);
        }
        Map<String, Object> map = new HashMap<>();
        map.put("networkId", vpcName);
        if (EXISTING_SUBNET_IN_EXISTING_NETWORK.equals(networkType)) {
            map.put("subnetId", vpcSubnet);
        }
        networkJson.setParameters(map);
        networkJson.setCloudPlatform("GCP");
        networkJson.setPublicInAccount(publicInAccount);
        String id = getCloudbreakClient().networkEndpoint().postPrivate(networkJson).getId().toString();
        getItContext().putContextParam(CloudbreakITContextConstants.NETWORK_ID, id, true);
    }

    private void waitOperation(Compute compute, Operation operation) throws java.io.IOException {
        int tried = 0;
        while (tried < MAX_TRY) {
            LOGGER.info("check operation: " + operation.getName() + ", tried: " + tried);
            Operation checkResponse = compute.globalOperations().get(defaultProjectId, operation.getName()).execute();
            if ("DONE".equals(checkResponse.getStatus())) {
                break;
            }
            tried++;
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                LOGGER.error("thread sleep interrupted", e);
            }
        }
        if (tried == MAX_TRY) {
            throw new RuntimeException("wait for operation exceeded maximum retry number, operation: " + operation.getName());
        }
    }

    public enum NetworkType {
        NEW_SUBNET_IN_EXISTING_NETWORK,
        EXISTING_SUBNET_IN_EXISTING_NETWORK,
        LAGACY_NETWORK
    }
}
