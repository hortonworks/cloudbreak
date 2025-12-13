package com.sequenceiq.freeipa.service.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.freeipa.entity.Network;

class GcpNetworkFilterProviderTest {

    private static final String SHARED_PROJECT_ID = "sharedProjectId";

    private static final String NETWORK_ID = "networkId";

    private static final String SUBNET_IDS = "subnetIds";

    private static final String TEST_PROJECT = "testproject";

    private static final String TEST_NETWORK = "testnetwork";

    private static final String TEST_SUBNET_ID1 = "testsubnetid1";

    private static final String TEST_SUBNET_ID2 = "testsubnetid2";

    private GcpNetworkFilterProvider gcpNetworkFilterProvider;

    private Network network;

    private String networkId;

    private Collection<String> subnetIds;

    @BeforeEach
    public void setUp() {
        gcpNetworkFilterProvider = new GcpNetworkFilterProvider();
        network = new Network();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(SHARED_PROJECT_ID, TEST_PROJECT);
        networkId = TEST_NETWORK;
        subnetIds = Collections.singletonList(TEST_SUBNET_ID1);
        network.setAttributes(new Json(parameters));
    }

    @Test
    void provide() {
        Map<String, String> provide = gcpNetworkFilterProvider.provide(network, networkId, subnetIds);
        assertEquals(TEST_SUBNET_ID1, provide.get(SUBNET_IDS));
        assertEquals(TEST_NETWORK, provide.get(NETWORK_ID));
        assertEquals(TEST_PROJECT, provide.get(SHARED_PROJECT_ID));
    }

    @Test
    void provideProjectLocalVPC() {
        Map<String, Object> parameters = new HashMap<>();
        network.setAttributes(new Json(parameters));
        Map<String, String> provide = gcpNetworkFilterProvider.provide(network, networkId, subnetIds);
        assertEquals(TEST_SUBNET_ID1, provide.get(SUBNET_IDS));
        assertEquals(TEST_NETWORK, provide.get(NETWORK_ID));
        assertNull(provide.get(SHARED_PROJECT_ID));
    }

    @Test
    void provideMultipleSubnets() {
        subnetIds = new ArrayList<>();
        subnetIds.add(TEST_SUBNET_ID1);
        subnetIds.add(TEST_SUBNET_ID2);
        Map<String, String> provide = gcpNetworkFilterProvider.provide(network, networkId, subnetIds);
        assertEquals(TEST_SUBNET_ID1 + ',' + TEST_SUBNET_ID2, provide.get(SUBNET_IDS));
        assertEquals(TEST_NETWORK, provide.get(NETWORK_ID));
        assertEquals(TEST_PROJECT, provide.get(SHARED_PROJECT_ID));
    }
}