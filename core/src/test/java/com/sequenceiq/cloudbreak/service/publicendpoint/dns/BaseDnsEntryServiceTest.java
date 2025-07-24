package com.sequenceiq.cloudbreak.service.publicendpoint.dns;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.PemDnsEntryCreateOrUpdateException;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.certificate.service.CertificateCreationService;
import com.sequenceiq.cloudbreak.certificate.service.DnsManagementService;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.nifi.NifiConfigProvider;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.blueprint.ComponentLocatorService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.cloudbreak.service.publicendpoint.EnvironmentBasedDomainNameProvider;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@ExtendWith(MockitoExtension.class)
class BaseDnsEntryServiceTest {

    private static final String ACCOUNT_ID = UUID.randomUUID().toString();

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:"
            + ACCOUNT_ID + ":user:" + UUID.randomUUID().toString();

    private static final String ENVIRONMENT_CRN = "envCrn";

    private static final String NIFI_NODE = "NIFI_NODE";

    private static final String FQDN_1 = "fqdn1";

    private static final String FQDN_2 = "fqdn2";

    private static final String FQDN_3 = "fqdn3";

    private static final String PUBLIC_IP_1 = "publicIp1";

    private static final String PUBLIC_IP_2 = "publicIp2";

    private static final String PUBLIC_IP_3 = "publicIp3";

    private static final String ENVIRONMENT_NAME = "envName";

    @Mock
    private DnsManagementService dnsManagementService;

    @Mock
    private EnvironmentBasedDomainNameProvider domainNameProvider;

    @Mock
    private GrpcUmsClient grpcUmsClient;

    @Mock
    private CertificateCreationService certificateCreationService;

    @Mock
    private EnvironmentService environmentClientService;

    @Mock
    private ComponentLocatorService componentLocatorService;

    @Spy
    private NifiConfigProvider nifiConfigProvider = new NifiConfigProvider();

    @InjectMocks
    private NifiHostPublicDnsEntryService underTest;

    @BeforeEach
    void setupEach() {
        underTest.setCertGenerationEnabled(true);
    }

    @Test
    void testGetComponentLocation() {
        Stack stack = new Stack();
        Cluster cluster = new Cluster();
        stack.setCluster(cluster);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.getComponentLocation(stack));

        verify(componentLocatorService).getComponentLocation(stack, nifiConfigProvider.getRoleTypes());
    }

    @Test
    void testCreateOrUpdate() throws PemDnsEntryCreateOrUpdateException {
        Stack stack = new Stack();
        stack.setEnvironmentCrn(ENVIRONMENT_CRN);
        Cluster cluster = new Cluster();
        stack.setCluster(cluster);
        setInstanceGroups(stack);
        DetailedEnvironmentResponse environmentResponse = new DetailedEnvironmentResponse();
        environmentResponse.setName(ENVIRONMENT_NAME);

        Map<String, List<String>> componentLocation = Map.of(NIFI_NODE, List.of(FQDN_1, FQDN_2, FQDN_3));

        when(environmentClientService.getByCrn(stack.getEnvironmentCrn())).thenReturn(environmentResponse);
        when(componentLocatorService.getComponentLocation(stack, nifiConfigProvider.getRoleTypes()))
                .thenReturn(componentLocation);

        Map<String, String> result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.createOrUpdate(stack));

        verify(dnsManagementService, times(1)).createOrUpdateDnsEntryWithIp(ACCOUNT_ID, FQDN_1, ENVIRONMENT_NAME, false, List.of(PUBLIC_IP_1));
        verify(dnsManagementService, times(1)).createOrUpdateDnsEntryWithIp(ACCOUNT_ID, FQDN_2, ENVIRONMENT_NAME, false, List.of(PUBLIC_IP_2));
        verify(dnsManagementService, times(1)).createOrUpdateDnsEntryWithIp(ACCOUNT_ID, FQDN_3, ENVIRONMENT_NAME, false, List.of(PUBLIC_IP_3));
        assertEquals(3, result.size());
        assertTrue(result.containsKey(FQDN_1));
        assertTrue(result.containsKey(FQDN_2));
        assertTrue(result.containsKey(FQDN_3));
        assertEquals(PUBLIC_IP_1, result.get(FQDN_1));
        assertEquals(PUBLIC_IP_2, result.get(FQDN_2));
        assertEquals(PUBLIC_IP_3, result.get(FQDN_3));
    }

    @Test
    void testCreateOrUpdateWhenTheLastDnsEntryRegistrationFails() throws PemDnsEntryCreateOrUpdateException {
        Stack stack = new Stack();
        stack.setEnvironmentCrn(ENVIRONMENT_CRN);
        Cluster cluster = new Cluster();
        stack.setCluster(cluster);
        setInstanceGroups(stack);
        DetailedEnvironmentResponse environmentResponse = new DetailedEnvironmentResponse();
        environmentResponse.setName(ENVIRONMENT_NAME);
        Map<String, List<String>> componentLocation = Map.of(NIFI_NODE, List.of(FQDN_1, FQDN_2, FQDN_3));
        when(environmentClientService.getByCrn(stack.getEnvironmentCrn())).thenReturn(environmentResponse);
        when(componentLocatorService.getComponentLocation(stack, nifiConfigProvider.getRoleTypes()))
                .thenReturn(componentLocation);
        doNothing()
                .doNothing()
                .doThrow(new PemDnsEntryCreateOrUpdateException("Uh-Oh"))
                .when(dnsManagementService).createOrUpdateDnsEntryWithIp(eq(ACCOUNT_ID), any(), eq(ENVIRONMENT_NAME), eq(false), anyList());

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class, () -> underTest.createOrUpdate(stack));
            Assertions.assertEquals("Failed to create DNS entry: 'Uh-Oh'", exception.getMessage());
        });

        verify(dnsManagementService, times(2)).deleteDnsEntryWithIp(eq(ACCOUNT_ID), any(), eq(ENVIRONMENT_NAME), eq(false), anyList());
    }

    @Test
    void testCreateOrUpdateCandidates() throws PemDnsEntryCreateOrUpdateException {
        Stack stack = new Stack();
        stack.setEnvironmentCrn(ENVIRONMENT_CRN);
        Cluster cluster = new Cluster();
        stack.setCluster(cluster);
        setInstanceGroups(stack);
        DetailedEnvironmentResponse environmentResponse = new DetailedEnvironmentResponse();
        environmentResponse.setName(ENVIRONMENT_NAME);

        Map<String, List<String>> componentLocation = Map.of(NIFI_NODE, List.of(FQDN_1, FQDN_2, FQDN_3));

        when(environmentClientService.getByCrn(stack.getEnvironmentCrn())).thenReturn(environmentResponse);
        when(componentLocatorService.getComponentLocation(stack, nifiConfigProvider.getRoleTypes()))
                .thenReturn(componentLocation);

        Map<String, String> result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.createOrUpdateCandidates(stack, Map.of(FQDN_1, PUBLIC_IP_1)));

        verify(dnsManagementService, times(1)).createOrUpdateDnsEntryWithIp(ACCOUNT_ID, FQDN_1, ENVIRONMENT_NAME, false, List.of(PUBLIC_IP_1));
        assertEquals(1, result.size());
        assertTrue(result.containsKey(FQDN_1));
        assertEquals(PUBLIC_IP_1, result.get(FQDN_1));
    }

    @Test
    void testCreateOrUpdateCandidatesWhenTheLastDnsEntryRegistrationFails() throws PemDnsEntryCreateOrUpdateException {
        Stack stack = new Stack();
        stack.setEnvironmentCrn(ENVIRONMENT_CRN);
        Cluster cluster = new Cluster();
        stack.setCluster(cluster);
        setInstanceGroups(stack);
        DetailedEnvironmentResponse environmentResponse = new DetailedEnvironmentResponse();
        environmentResponse.setName(ENVIRONMENT_NAME);

        Map<String, List<String>> componentLocation = Map.of(NIFI_NODE, List.of(FQDN_1, FQDN_2, FQDN_3));

        when(environmentClientService.getByCrn(stack.getEnvironmentCrn())).thenReturn(environmentResponse);
        when(componentLocatorService.getComponentLocation(stack, nifiConfigProvider.getRoleTypes()))
                .thenReturn(componentLocation);
        doNothing()
                .doThrow(new PemDnsEntryCreateOrUpdateException("Uh-Oh"))
                .when(dnsManagementService).createOrUpdateDnsEntryWithIp(eq(ACCOUNT_ID), any(), eq(ENVIRONMENT_NAME), eq(false), anyList());

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class,
                    () -> underTest.createOrUpdateCandidates(stack, Map.of(FQDN_1, PUBLIC_IP_1, FQDN_2, PUBLIC_IP_2)));
            Assertions.assertEquals("Failed to create DNS entry: 'Uh-Oh'", exception.getMessage());
        });

        verify(dnsManagementService, times(1)).deleteDnsEntryWithIp(eq(ACCOUNT_ID), any(), eq(ENVIRONMENT_NAME), eq(false), anyList());
    }

    @Test
    void testDeregister() {
        Stack stack = new Stack();
        stack.setEnvironmentCrn(ENVIRONMENT_CRN);
        Cluster cluster = new Cluster();
        stack.setCluster(cluster);
        setInstanceGroups(stack);
        DetailedEnvironmentResponse environmentResponse = new DetailedEnvironmentResponse();
        environmentResponse.setName(ENVIRONMENT_NAME);

        Map<String, List<String>> componentLocation = Map.of(NIFI_NODE, List.of(FQDN_1, FQDN_2, FQDN_3));

        when(environmentClientService.getByCrn(stack.getEnvironmentCrn())).thenReturn(environmentResponse);
        when(componentLocatorService.getComponentLocation(stack, nifiConfigProvider.getRoleTypes()))
                .thenReturn(componentLocation);
        when(dnsManagementService.deleteDnsEntryWithIp(ACCOUNT_ID, FQDN_1, ENVIRONMENT_NAME, false, List.of(PUBLIC_IP_1))).thenReturn(true);
        when(dnsManagementService.deleteDnsEntryWithIp(ACCOUNT_ID, FQDN_2, ENVIRONMENT_NAME, false, List.of(PUBLIC_IP_2))).thenReturn(true);
        when(dnsManagementService.deleteDnsEntryWithIp(ACCOUNT_ID, FQDN_3, ENVIRONMENT_NAME, false, List.of(PUBLIC_IP_3))).thenReturn(true);

        Map<String, String> result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.deregister(stack));

        assertEquals(3, result.size());
        assertTrue(result.containsKey(FQDN_1));
        assertTrue(result.containsKey(FQDN_2));
        assertTrue(result.containsKey(FQDN_3));
        assertEquals(PUBLIC_IP_1, result.get(FQDN_1));
        assertEquals(PUBLIC_IP_2, result.get(FQDN_2));
        assertEquals(PUBLIC_IP_3, result.get(FQDN_3));
    }

    @Test
    void testDeregisterWithCandidates() {
        Stack stack = new Stack();
        stack.setEnvironmentCrn(ENVIRONMENT_CRN);
        Cluster cluster = new Cluster();
        stack.setCluster(cluster);
        setInstanceGroups(stack);
        DetailedEnvironmentResponse environmentResponse = new DetailedEnvironmentResponse();
        environmentResponse.setName(ENVIRONMENT_NAME);

        Map<String, List<String>> componentLocation = Map.of(NIFI_NODE, List.of(FQDN_1, FQDN_2, FQDN_3));

        when(environmentClientService.getByCrn(stack.getEnvironmentCrn())).thenReturn(environmentResponse);
        when(componentLocatorService.getComponentLocation(stack, nifiConfigProvider.getRoleTypes()))
                .thenReturn(componentLocation);
        when(dnsManagementService.deleteDnsEntryWithIp(ACCOUNT_ID, FQDN_1, ENVIRONMENT_NAME, false, List.of(PUBLIC_IP_1))).thenReturn(true);

        Map<String, String> result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.deregister(stack, Map.of(FQDN_1, PUBLIC_IP_1)));

        assertEquals(1, result.size());
        assertTrue(result.containsKey(FQDN_1));
        assertEquals(PUBLIC_IP_1, result.get(FQDN_1));
    }

    @Test
    void testCreateOrUpdateCreateCdpConsoleDomainEntry() {
        // --- Setup common objects ---
        Stack stack = new Stack();
        stack.setEnvironmentCrn(ENVIRONMENT_CRN);
        Cluster cluster = new Cluster();
        stack.setCluster(cluster);

        // Mocked ECS master FQDN and IP
        String ecsMasterFqdn = "ecs-master-1.example.com";
        String ecsMasterShortHostname = "ecs-master-1";
        String ecsMasterIp = "1.2.3.4";

        // Prepare instance metadata for ECS master
        InstanceMetaData ecsMasterInstance = new InstanceMetaData();
        ecsMasterInstance.setPrivateId(10L);
        ecsMasterInstance.setInstanceStatus(InstanceStatus.SERVICES_HEALTHY);
        ecsMasterInstance.setDiscoveryFQDN(ecsMasterFqdn);
        ecsMasterInstance.setPublicIp(ecsMasterIp);

        // Primary gateway instance (excluded from candidate IPs)
        InstanceMetaData gatewayInstance = new InstanceMetaData();
        gatewayInstance.setPrivateId(99L);
        gatewayInstance.setInstanceMetadataType(InstanceMetadataType.GATEWAY_PRIMARY);
        gatewayInstance.setInstanceStatus(InstanceStatus.SERVICES_HEALTHY);
        gatewayInstance.setDiscoveryFQDN("gateway-fqdn");

        // Set instance groups
        InstanceGroup ig1 = new InstanceGroup();
        ig1.setInstanceGroupType(InstanceGroupType.CORE);
        ig1.setInstanceMetaData(Set.of(ecsMasterInstance));
        InstanceGroup ig2 = new InstanceGroup();
        ig2.setInstanceGroupType(InstanceGroupType.GATEWAY);
        ig2.setInstanceMetaData(Set.of(gatewayInstance));
        stack.setInstanceGroups(Set.of(ig1, ig2));

        // Mock environment response
        DetailedEnvironmentResponse envResponse = new DetailedEnvironmentResponse();
        envResponse.setName(ENVIRONMENT_NAME);

        // --- Branch 1: HYBRID_BASE, ecs_master present, candidate IP found ---
        envResponse.setEnvironmentType("HYBRID_BASE");
        when(environmentClientService.getByCrn(stack.getEnvironmentCrn())).thenReturn(envResponse);
        when(componentLocatorService.getComponentLocation(stack, nifiConfigProvider.getRoleTypes()))
                .thenReturn(Map.of("ecs_master", List.of(ecsMasterFqdn)));

        Map<String, String> result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.createOrUpdate(stack));
        assertTrue(result.containsKey("console-cdp.apps"));
        assertEquals(ecsMasterIp, result.get("console-cdp.apps"));

        // --- Branch 2: HYBRID_BASE, ecs_master present, candidate IP not found ---
        when(componentLocatorService.getComponentLocation(stack, nifiConfigProvider.getRoleTypes()))
                .thenReturn(Map.of("ecs_master", List.of("ecs-master-2.example.com")));
        result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.createOrUpdate(stack));
        assertTrue(!result.containsKey("console-cdp.apps"));

        // --- Branch 3: HYBRID_BASE, ecs_master not present ---
        when(componentLocatorService.getComponentLocation(stack, nifiConfigProvider.getRoleTypes()))
                .thenReturn(Map.of("other_component", List.of("host1")));
        result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.createOrUpdate(stack));
        assertTrue(!result.containsKey("console-cdp.apps"));

        // --- Branch 4: Not HYBRID_BASE ---
        envResponse.setEnvironmentType("SOMETHING_ELSE");
        when(componentLocatorService.getComponentLocation(stack, nifiConfigProvider.getRoleTypes()))
                .thenReturn(Map.of("ecs_master", List.of(ecsMasterFqdn)));
        result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.createOrUpdate(stack));
        assertTrue(!result.containsKey("console-cdp.apps"));
    }

    private void setInstanceGroups(Stack stack) {
        InstanceGroup ig1 = getInstanceGroup1();
        InstanceGroup ig2 = getInstanceGroup2();
        InstanceGroup ig3 = getInstanceGroup3();

        Set<InstanceGroup> instanceGroups = Set.of(ig1, ig2, ig3);
        stack.setInstanceGroups(instanceGroups);
    }

    private InstanceGroup getInstanceGroup1() {
        InstanceGroup ig1 = new InstanceGroup();
        ig1.setInstanceGroupType(InstanceGroupType.CORE);
        InstanceMetaData imd1 = new InstanceMetaData();
        imd1.setPrivateId(1L);
        imd1.setInstanceStatus(InstanceStatus.SERVICES_HEALTHY);
        imd1.setDiscoveryFQDN(FQDN_1);
        imd1.setPublicIp(PUBLIC_IP_1);
        InstanceMetaData imd2 = new InstanceMetaData();
        imd2.setPrivateId(2L);
        imd2.setInstanceStatus(InstanceStatus.SERVICES_HEALTHY);
        imd2.setDiscoveryFQDN(FQDN_2);
        imd2.setPublicIp(PUBLIC_IP_2);
        ig1.setInstanceMetaData(Set.of(imd1, imd2));
        return ig1;
    }

    private InstanceGroup getInstanceGroup2() {
        InstanceGroup ig2 = new InstanceGroup();
        ig2.setInstanceGroupType(InstanceGroupType.CORE);
        InstanceMetaData imd3 = new InstanceMetaData();
        imd3.setPrivateId(3L);
        imd3.setInstanceStatus(InstanceStatus.SERVICES_HEALTHY);
        imd3.setDiscoveryFQDN(FQDN_3);
        imd3.setPublicIp(PUBLIC_IP_3);
        ig2.setInstanceMetaData(Set.of(imd3));
        return ig2;
    }

    private InstanceGroup getInstanceGroup3() {
        InstanceGroup ig3 = new InstanceGroup();
        ig3.setInstanceGroupType(InstanceGroupType.GATEWAY);
        InstanceMetaData imd4 = new InstanceMetaData();
        imd4.setPrivateId(4L);
        imd4.setInstanceMetadataType(InstanceMetadataType.GATEWAY_PRIMARY);
        imd4.setInstanceStatus(InstanceStatus.SERVICES_HEALTHY);
        imd4.setDiscoveryFQDN("fqdn4");
        InstanceMetaData imd5 = new InstanceMetaData();
        imd5.setPrivateId(5L);
        imd5.setInstanceStatus(InstanceStatus.SERVICES_HEALTHY);
        imd5.setDiscoveryFQDN("fqdn5");
        ig3.setInstanceMetaData(Set.of(imd4, imd5));
        return ig3;
    }

}