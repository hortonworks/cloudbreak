package com.sequenceiq.cloudbreak.service.freeipa;

import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus.CREATED;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus.REQUESTED;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus.TERMINATED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetaDataResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetadataType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;

@ExtendWith(MockitoExtension.class)
class FreeIpaConfigProviderTest {

    public static final String ENVIRONMENT_CRN = "ENV_CRN";

    @Mock
    private FreeipaClientService freeipaClient;

    @InjectMocks
    private FreeIpaConfigProvider underTest;

    @Test
    public void testCreatedChoosen() {
        DescribeFreeIpaResponse freeIpaResponse = new DescribeFreeIpaResponse();
        InstanceGroupResponse instanceGroupResponse = new InstanceGroupResponse();
        instanceGroupResponse.setMetaData(Set.of(createInstanceMetadata("b", TERMINATED), createInstanceMetadata("a", REQUESTED),
                createInstanceMetadata("f", CREATED), createInstanceMetadata("c", CREATED)));
        freeIpaResponse.setInstanceGroups(List.of(instanceGroupResponse));
        when(freeipaClient.findByEnvironmentCrn(ENVIRONMENT_CRN)).thenReturn(Optional.of(freeIpaResponse));

        Map<String, Object> result = underTest.createFreeIpaConfig(ENVIRONMENT_CRN);

        assertEquals(1, result.size());
        assertEquals("c", result.get("host"));
    }

    @Test
    public void testNonCreatedChoosen() {
        DescribeFreeIpaResponse freeIpaResponse = new DescribeFreeIpaResponse();
        InstanceGroupResponse instanceGroupResponse = new InstanceGroupResponse();
        instanceGroupResponse.setMetaData(Set.of(createInstanceMetadata("b", TERMINATED), createInstanceMetadata("a", REQUESTED)));
        freeIpaResponse.setInstanceGroups(List.of(instanceGroupResponse));
        when(freeipaClient.findByEnvironmentCrn(ENVIRONMENT_CRN)).thenReturn(Optional.of(freeIpaResponse));

        Map<String, Object> result = underTest.createFreeIpaConfig(ENVIRONMENT_CRN);

        assertEquals(1, result.size());
        assertEquals("a", result.get("host"));
    }

    @Test
    public void testNoChoosenIfInsanceMetadataEmpty() {
        DescribeFreeIpaResponse freeIpaResponse = new DescribeFreeIpaResponse();
        InstanceGroupResponse instanceGroupResponse = new InstanceGroupResponse();
        instanceGroupResponse.setMetaData(Set.of());
        freeIpaResponse.setInstanceGroups(List.of(instanceGroupResponse));
        when(freeipaClient.findByEnvironmentCrn(ENVIRONMENT_CRN)).thenReturn(Optional.of(freeIpaResponse));

        Map<String, Object> result = underTest.createFreeIpaConfig(ENVIRONMENT_CRN);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testNoChoosenIfInsanceGroupEmpty() {
        DescribeFreeIpaResponse freeIpaResponse = new DescribeFreeIpaResponse();
        freeIpaResponse.setInstanceGroups(List.of());
        when(freeipaClient.findByEnvironmentCrn(ENVIRONMENT_CRN)).thenReturn(Optional.of(freeIpaResponse));

        Map<String, Object> result = underTest.createFreeIpaConfig(ENVIRONMENT_CRN);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testNoChoosenIfResponseMissing() {
        when(freeipaClient.findByEnvironmentCrn(ENVIRONMENT_CRN)).thenReturn(Optional.empty());

        Map<String, Object> result = underTest.createFreeIpaConfig(ENVIRONMENT_CRN);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testPgwChosen() {
        DescribeFreeIpaResponse freeIpaResponse = new DescribeFreeIpaResponse();
        InstanceGroupResponse instanceGroupResponse = new InstanceGroupResponse();
        InstanceMetaDataResponse pgw = createInstanceMetadata("d", CREATED);
        pgw.setInstanceType(InstanceMetadataType.GATEWAY_PRIMARY);
        instanceGroupResponse.setMetaData(Set.of(createInstanceMetadata("b", TERMINATED), createInstanceMetadata("a", REQUESTED),
                createInstanceMetadata("f", CREATED), pgw, createInstanceMetadata("c", CREATED)));
        freeIpaResponse.setInstanceGroups(List.of(instanceGroupResponse));
        when(freeipaClient.findByEnvironmentCrn(ENVIRONMENT_CRN)).thenReturn(Optional.of(freeIpaResponse));

        Map<String, Object> result = underTest.createFreeIpaConfig(ENVIRONMENT_CRN);

        assertEquals(1, result.size());
        assertEquals("d", result.get("host"));
    }

    @Test
    public void testPgwNotChosenIfNotCreated() {
        DescribeFreeIpaResponse freeIpaResponse = new DescribeFreeIpaResponse();
        InstanceGroupResponse instanceGroupResponse = new InstanceGroupResponse();
        InstanceMetaDataResponse pgw = createInstanceMetadata("d", TERMINATED);
        pgw.setInstanceType(InstanceMetadataType.GATEWAY_PRIMARY);
        instanceGroupResponse.setMetaData(Set.of(createInstanceMetadata("b", TERMINATED), createInstanceMetadata("a", REQUESTED),
                createInstanceMetadata("f", CREATED), pgw, createInstanceMetadata("c", CREATED)));
        freeIpaResponse.setInstanceGroups(List.of(instanceGroupResponse));
        when(freeipaClient.findByEnvironmentCrn(ENVIRONMENT_CRN)).thenReturn(Optional.of(freeIpaResponse));

        Map<String, Object> result = underTest.createFreeIpaConfig(ENVIRONMENT_CRN);

        assertEquals(1, result.size());
        assertEquals("c", result.get("host"));
    }

    private InstanceMetaDataResponse createInstanceMetadata(String fqdn, InstanceStatus status) {
        InstanceMetaDataResponse response = new InstanceMetaDataResponse();
        response.setDiscoveryFQDN(fqdn);
        response.setInstanceStatus(status);
        return response;
    }
}