package com.sequenceiq.cloudbreak.service.stack;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.conf.DatahubOperationConfig;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.sdx.common.model.DistroXOperationValidationView;
import com.sequenceiq.cloudbreak.sdx.common.model.DistroXOperations;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

@ExtendWith(MockitoExtension.class)
class DistroxOperationValidatorServiceTest {

    private static final String TEST_ACCOUNT_ID = "accid";

    private static final String TEST_USER_CRN = String.format("crn:cdp:iam:us-west-1:%s:user:mockuser@cloudera.com", TEST_ACCOUNT_ID);

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private DatahubOperationConfig datahubOperationConfig;

    @InjectMocks
    private DistroxOperationValidatorService underTest;

    @Test
    void testValidateDistroXStartOperationPGHealthy() {
        InstanceMetaData instanceMetadataView1 = new InstanceMetaData();
        instanceMetadataView1.setInstanceMetadataType(InstanceMetadataType.GATEWAY_PRIMARY);
        instanceMetadataView1.setInstanceStatus(InstanceStatus.SERVICES_HEALTHY);
        InstanceMetaData instanceMetadataView2 = new InstanceMetaData();
        instanceMetadataView2.setInstanceMetadataType(InstanceMetadataType.GATEWAY);
        instanceMetadataView2.setInstanceStatus(InstanceStatus.SERVICES_UNHEALTHY);
        List<InstanceMetadataView> instanceMetadataViewList = List.of(instanceMetadataView1, instanceMetadataView2);

        SdxClusterResponse sdxClusterResponse = new SdxClusterResponse();
        sdxClusterResponse.setClusterShape(SdxClusterShape.ENTERPRISE);

        when(entitlementService.isValidateDistroxOperationsBySdxHealthEnabled(anyString())).thenReturn(true);

        List<SdxClusterResponse> sdxClusterResponseList = List.of(sdxClusterResponse);

        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> {
            DistroXOperationValidationView distroXOperationValidationView = underTest.validateDistroXStartOperation("env_crn",
                    DistroXOperations.START, sdxClusterResponseList, instanceMetadataViewList);
            assertTrue(distroXOperationValidationView.isAllowed());
        });
    }

    @Test
    void testValidateDistroXStartOperationPGUnhealthy() {
        InstanceMetaData instanceMetadataView1 = new InstanceMetaData();
        instanceMetadataView1.setInstanceMetadataType(InstanceMetadataType.GATEWAY_PRIMARY);
        instanceMetadataView1.setInstanceStatus(InstanceStatus.SERVICES_UNHEALTHY);
        InstanceMetaData instanceMetadataView2 = new InstanceMetaData();
        instanceMetadataView2.setInstanceMetadataType(InstanceMetadataType.GATEWAY);
        instanceMetadataView2.setInstanceStatus(InstanceStatus.SERVICES_HEALTHY);
        List<InstanceMetadataView> instanceMetadataViewList = List.of(instanceMetadataView1, instanceMetadataView2);

        SdxClusterResponse sdxClusterResponse = new SdxClusterResponse();
        sdxClusterResponse.setClusterShape(SdxClusterShape.ENTERPRISE);

        when(entitlementService.isValidateDistroxOperationsBySdxHealthEnabled(anyString())).thenReturn(true);

        List<SdxClusterResponse> sdxClusterResponseList = List.of(sdxClusterResponse);

        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> {
            DistroXOperationValidationView distroXOperationValidationView = underTest.validateDistroXStartOperation("env_crn",
                    DistroXOperations.START, sdxClusterResponseList, instanceMetadataViewList);
            assertFalse(distroXOperationValidationView.isAllowed());
        });
    }

    @Test
    void testValidateDistroXStartOperationByStackHealth() {
        InstanceMetaData instanceMetadataView1 = new InstanceMetaData();
        instanceMetadataView1.setInstanceMetadataType(InstanceMetadataType.GATEWAY_PRIMARY);
        instanceMetadataView1.setInstanceStatus(InstanceStatus.SERVICES_HEALTHY);
        InstanceMetaData instanceMetadataView2 = new InstanceMetaData();
        instanceMetadataView2.setInstanceMetadataType(InstanceMetadataType.GATEWAY);
        instanceMetadataView2.setInstanceStatus(InstanceStatus.SERVICES_HEALTHY);
        List<InstanceMetadataView> instanceMetadataViewList = List.of(instanceMetadataView1, instanceMetadataView2);

        SdxClusterResponse sdxClusterResponse = new SdxClusterResponse();
        sdxClusterResponse.setClusterShape(SdxClusterShape.ENTERPRISE);
        sdxClusterResponse.setStatus(SdxClusterStatusResponse.NODE_FAILURE);

        when(entitlementService.isValidateDistroxOperationsBySdxHealthEnabled(anyString())).thenReturn(false);

        List<SdxClusterResponse> sdxClusterResponseList = List.of(sdxClusterResponse);

        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> {
            DistroXOperationValidationView distroXOperationValidationView = underTest.validateDistroXStartOperation("env_crn",
                    DistroXOperations.START, sdxClusterResponseList, instanceMetadataViewList);
            assertFalse(distroXOperationValidationView.isAllowed());
        });
    }

    @Test
    void testValidateDistroXStartOperationNoSdxPresent() {
        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> {
            DistroXOperationValidationView distroXOperationValidationView = underTest.validateDistroXStartOperation("env_crn",
                    DistroXOperations.START, Collections.emptyList(), Collections.emptyList());
            assertFalse(distroXOperationValidationView.isAllowed());
        });
    }

    @Test
    void testValidateDistroXCreateOperationNoSdxPresent() {
        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> {
            DistroXOperationValidationView distroXOperationValidationView = underTest.validateDistroXStartOperation("env_crn",
                    DistroXOperations.CREATE, Collections.emptyList(), Collections.emptyList());
            assertFalse(distroXOperationValidationView.isAllowed());
        });
    }

    @Test
    void testValidateDistroXCreateOperationAllHealthy() {
        DatahubOperationConfig.OperationConfig operationConfig = new DatahubOperationConfig.OperationConfig();
        operationConfig.setMandatoryHealthyHostgroups(Set.of("master"));
        operationConfig.setRequiredPartialHostgroups(Set.of("idbroker", "gateway"));
        Map<String, DatahubOperationConfig.OperationConfig> operationsMap = Map.of(
                DistroXOperations.CREATE.name().toLowerCase(), operationConfig
        );
        when(datahubOperationConfig.getOperations()).thenReturn(operationsMap);

        InstanceMetaData instanceMetadataView1 = new InstanceMetaData();
        InstanceGroup master = new InstanceGroup();
        master.setGroupName("master");
        instanceMetadataView1.setInstanceGroup(master);
        instanceMetadataView1.setInstanceStatus(InstanceStatus.SERVICES_HEALTHY);
        instanceMetadataView1.setDiscoveryFQDN("master-fqdn");

        InstanceMetaData instanceMetadataView2 = new InstanceMetaData();
        InstanceGroup gateway = new InstanceGroup();
        gateway.setGroupName("gateway");
        instanceMetadataView2.setInstanceGroup(gateway);
        instanceMetadataView2.setInstanceStatus(InstanceStatus.SERVICES_HEALTHY);
        instanceMetadataView2.setDiscoveryFQDN("gateway-fqdn");

        InstanceMetaData instanceMetadataView3 = new InstanceMetaData();
        InstanceGroup idbroker = new InstanceGroup();
        idbroker.setGroupName("idbroker");
        instanceMetadataView3.setInstanceGroup(idbroker);
        instanceMetadataView3.setInstanceStatus(InstanceStatus.SERVICES_HEALTHY);
        instanceMetadataView3.setDiscoveryFQDN("idbroker-fqdn");

        InstanceMetaData instanceMetadataView4 = new InstanceMetaData();
        instanceMetadataView4.setInstanceGroup(gateway);
        instanceMetadataView4.setInstanceStatus(InstanceStatus.SERVICES_UNHEALTHY);
        instanceMetadataView4.setDiscoveryFQDN("gateway-fqdn1");

        InstanceMetaData instanceMetadataView5 = new InstanceMetaData();
        instanceMetadataView5.setInstanceGroup(idbroker);
        instanceMetadataView5.setInstanceStatus(InstanceStatus.SERVICES_UNHEALTHY);
        instanceMetadataView5.setDiscoveryFQDN("idbroker-fqdn1");

        List<InstanceMetadataView> instanceMetadataViewList = List.of(
                instanceMetadataView1,
                instanceMetadataView2,
                instanceMetadataView3,
                instanceMetadataView4,
                instanceMetadataView5
        );

        SdxClusterResponse sdxClusterResponse = new SdxClusterResponse();
        sdxClusterResponse.setClusterShape(SdxClusterShape.ENTERPRISE);
        sdxClusterResponse.setName("sdx");

        when(entitlementService.isValidateDistroxOperationsBySdxHealthEnabled(anyString())).thenReturn(true);
        List<SdxClusterResponse> sdxClusterResponseList = List.of(sdxClusterResponse);

        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> {
            DistroXOperationValidationView distroXOperationValidationView = underTest.validateDistroXCreateOperation("env_crn",
                    DistroXOperations.CREATE, sdxClusterResponseList, instanceMetadataViewList);
            assertTrue(distroXOperationValidationView.isAllowed());
        });
    }

    @Test
    void testValidateDistroXCreateOperationMasterUnhealthy() {
        DatahubOperationConfig.OperationConfig operationConfig = new DatahubOperationConfig.OperationConfig();
        operationConfig.setMandatoryHealthyHostgroups(Set.of("master"));
        operationConfig.setRequiredPartialHostgroups(Set.of("idbroker", "gateway"));
        Map<String, DatahubOperationConfig.OperationConfig> operationsMap = Map.of(
                DistroXOperations.CREATE.name().toLowerCase(), operationConfig
        );
        when(datahubOperationConfig.getOperations()).thenReturn(operationsMap);

        InstanceMetaData instanceMetadataView1 = new InstanceMetaData();
        InstanceGroup master = new InstanceGroup();
        master.setGroupName("master");
        instanceMetadataView1.setInstanceGroup(master);
        instanceMetadataView1.setInstanceStatus(InstanceStatus.SERVICES_UNHEALTHY);
        instanceMetadataView1.setDiscoveryFQDN("master-fqdn");

        InstanceMetaData instanceMetadataView2 = new InstanceMetaData();
        InstanceGroup gateway = new InstanceGroup();
        gateway.setGroupName("gateway");
        instanceMetadataView2.setInstanceGroup(gateway);
        instanceMetadataView2.setInstanceStatus(InstanceStatus.SERVICES_HEALTHY);
        instanceMetadataView2.setDiscoveryFQDN("gateway-fqdn");

        InstanceMetaData instanceMetadataView3 = new InstanceMetaData();
        InstanceGroup idbroker = new InstanceGroup();
        idbroker.setGroupName("idbroker");
        instanceMetadataView3.setInstanceGroup(idbroker);
        instanceMetadataView3.setInstanceStatus(InstanceStatus.SERVICES_HEALTHY);
        instanceMetadataView3.setDiscoveryFQDN("idbroker-fqdn");

        InstanceMetaData instanceMetadataView4 = new InstanceMetaData();
        instanceMetadataView4.setInstanceGroup(gateway);
        instanceMetadataView4.setInstanceStatus(InstanceStatus.SERVICES_UNHEALTHY);
        instanceMetadataView4.setDiscoveryFQDN("gateway-fqdn1");

        InstanceMetaData instanceMetadataView5 = new InstanceMetaData();
        instanceMetadataView5.setInstanceGroup(idbroker);
        instanceMetadataView5.setInstanceStatus(InstanceStatus.SERVICES_UNHEALTHY);
        instanceMetadataView5.setDiscoveryFQDN("idbroker-fqdn1");

        List<InstanceMetadataView> instanceMetadataViewList = List.of(
                instanceMetadataView1,
                instanceMetadataView2,
                instanceMetadataView3,
                instanceMetadataView4,
                instanceMetadataView5
        );

        SdxClusterResponse sdxClusterResponse = new SdxClusterResponse();
        sdxClusterResponse.setClusterShape(SdxClusterShape.ENTERPRISE);
        sdxClusterResponse.setName("sdx");

        when(entitlementService.isValidateDistroxOperationsBySdxHealthEnabled(anyString())).thenReturn(true);
        List<SdxClusterResponse> sdxClusterResponseList = List.of(sdxClusterResponse);

        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> {
            DistroXOperationValidationView distroXOperationValidationView = underTest.validateDistroXCreateOperation("env_crn",
                    DistroXOperations.CREATE, sdxClusterResponseList, instanceMetadataViewList);
            assertFalse(distroXOperationValidationView.isAllowed());
        });
    }

    @Test
    void testValidateDistroXCreateOperationIdbrokerAllUnhealthy() {
        DatahubOperationConfig.OperationConfig operationConfig = new DatahubOperationConfig.OperationConfig();
        operationConfig.setMandatoryHealthyHostgroups(Set.of("master"));
        operationConfig.setRequiredPartialHostgroups(Set.of("idbroker", "gateway"));
        Map<String, DatahubOperationConfig.OperationConfig> operationsMap = Map.of(
                DistroXOperations.CREATE.name().toLowerCase(), operationConfig
        );
        when(datahubOperationConfig.getOperations()).thenReturn(operationsMap);

        InstanceMetaData instanceMetadataView1 = new InstanceMetaData();
        InstanceGroup master = new InstanceGroup();
        master.setGroupName("master");
        instanceMetadataView1.setInstanceGroup(master);
        instanceMetadataView1.setInstanceStatus(InstanceStatus.SERVICES_HEALTHY);
        instanceMetadataView1.setDiscoveryFQDN("master-fqdn");

        InstanceMetaData instanceMetadataView2 = new InstanceMetaData();
        InstanceGroup gateway = new InstanceGroup();
        gateway.setGroupName("gateway");
        instanceMetadataView2.setInstanceGroup(gateway);
        instanceMetadataView2.setInstanceStatus(InstanceStatus.SERVICES_HEALTHY);
        instanceMetadataView2.setDiscoveryFQDN("gateway-fqdn");

        InstanceMetaData instanceMetadataView3 = new InstanceMetaData();
        InstanceGroup idbroker = new InstanceGroup();
        idbroker.setGroupName("idbroker");
        instanceMetadataView3.setInstanceGroup(idbroker);
        instanceMetadataView3.setInstanceStatus(InstanceStatus.SERVICES_UNHEALTHY);
        instanceMetadataView3.setDiscoveryFQDN("idbroker-fqdn");

        InstanceMetaData instanceMetadataView4 = new InstanceMetaData();
        instanceMetadataView4.setInstanceGroup(gateway);
        instanceMetadataView4.setInstanceStatus(InstanceStatus.SERVICES_UNHEALTHY);
        instanceMetadataView4.setDiscoveryFQDN("gateway-fqdn1");

        InstanceMetaData instanceMetadataView5 = new InstanceMetaData();
        instanceMetadataView5.setInstanceGroup(idbroker);
        instanceMetadataView5.setInstanceStatus(InstanceStatus.SERVICES_UNHEALTHY);
        instanceMetadataView5.setDiscoveryFQDN("idbroker-fqdn1");

        List<InstanceMetadataView> instanceMetadataViewList = List.of(
                instanceMetadataView1,
                instanceMetadataView2,
                instanceMetadataView3,
                instanceMetadataView4,
                instanceMetadataView5
        );

        SdxClusterResponse sdxClusterResponse = new SdxClusterResponse();
        sdxClusterResponse.setClusterShape(SdxClusterShape.ENTERPRISE);
        sdxClusterResponse.setName("sdx");

        when(entitlementService.isValidateDistroxOperationsBySdxHealthEnabled(anyString())).thenReturn(true);
        List<SdxClusterResponse> sdxClusterResponseList = List.of(sdxClusterResponse);

        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> {
            DistroXOperationValidationView distroXOperationValidationView = underTest.validateDistroXCreateOperation("env_crn",
                    DistroXOperations.CREATE, sdxClusterResponseList, instanceMetadataViewList);
            assertFalse(distroXOperationValidationView.isAllowed());
        });
    }

    @Test
    void testValidateDistroXCreateOperationAllGatewayUnhealthy() {
        DatahubOperationConfig.OperationConfig operationConfig = new DatahubOperationConfig.OperationConfig();
        operationConfig.setMandatoryHealthyHostgroups(Set.of("master"));
        operationConfig.setRequiredPartialHostgroups(Set.of("idbroker", "gateway"));
        Map<String, DatahubOperationConfig.OperationConfig> operationsMap = Map.of(
                DistroXOperations.CREATE.name().toLowerCase(), operationConfig
        );
        when(datahubOperationConfig.getOperations()).thenReturn(operationsMap);

        InstanceMetaData instanceMetadataView1 = new InstanceMetaData();
        InstanceGroup master = new InstanceGroup();
        master.setGroupName("master");
        instanceMetadataView1.setInstanceGroup(master);
        instanceMetadataView1.setInstanceStatus(InstanceStatus.SERVICES_HEALTHY);
        instanceMetadataView1.setDiscoveryFQDN("master-fqdn");

        InstanceMetaData instanceMetadataView2 = new InstanceMetaData();
        InstanceGroup gateway = new InstanceGroup();
        gateway.setGroupName("gateway");
        instanceMetadataView2.setInstanceGroup(gateway);
        instanceMetadataView2.setInstanceStatus(InstanceStatus.SERVICES_UNHEALTHY);
        instanceMetadataView2.setDiscoveryFQDN("gateway-fqdn");

        InstanceMetaData instanceMetadataView3 = new InstanceMetaData();
        InstanceGroup idbroker = new InstanceGroup();
        idbroker.setGroupName("idbroker");
        instanceMetadataView3.setInstanceGroup(idbroker);
        instanceMetadataView3.setInstanceStatus(InstanceStatus.SERVICES_HEALTHY);
        instanceMetadataView3.setDiscoveryFQDN("idbroker-fqdn");

        InstanceMetaData instanceMetadataView4 = new InstanceMetaData();
        instanceMetadataView4.setInstanceGroup(gateway);
        instanceMetadataView4.setInstanceStatus(InstanceStatus.SERVICES_UNHEALTHY);
        instanceMetadataView4.setDiscoveryFQDN("gateway-fqdn1");

        InstanceMetaData instanceMetadataView5 = new InstanceMetaData();
        instanceMetadataView5.setInstanceGroup(idbroker);
        instanceMetadataView5.setInstanceStatus(InstanceStatus.SERVICES_UNHEALTHY);
        instanceMetadataView5.setDiscoveryFQDN("idbroker-fqdn1");

        List<InstanceMetadataView> instanceMetadataViewList = List.of(
                instanceMetadataView1,
                instanceMetadataView2,
                instanceMetadataView3,
                instanceMetadataView4,
                instanceMetadataView5
        );

        SdxClusterResponse sdxClusterResponse = new SdxClusterResponse();
        sdxClusterResponse.setClusterShape(SdxClusterShape.ENTERPRISE);
        sdxClusterResponse.setName("sdx");

        when(entitlementService.isValidateDistroxOperationsBySdxHealthEnabled(anyString())).thenReturn(true);
        List<SdxClusterResponse> sdxClusterResponseList = List.of(sdxClusterResponse);

        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> {
            DistroXOperationValidationView distroXOperationValidationView = underTest.validateDistroXCreateOperation("env_crn",
                    DistroXOperations.CREATE, sdxClusterResponseList, instanceMetadataViewList);
            assertFalse(distroXOperationValidationView.isAllowed());
        });
    }

    @Test
    void testValidateDistroXCreateOperationBySdxHealth() {

        InstanceMetaData instanceMetadataView1 = new InstanceMetaData();
        InstanceGroup master = new InstanceGroup();
        master.setGroupName("master");
        instanceMetadataView1.setInstanceGroup(master);
        instanceMetadataView1.setInstanceStatus(InstanceStatus.SERVICES_HEALTHY);
        instanceMetadataView1.setDiscoveryFQDN("master-fqdn");

        InstanceMetaData instanceMetadataView2 = new InstanceMetaData();
        InstanceGroup gateway = new InstanceGroup();
        gateway.setGroupName("gateway");
        instanceMetadataView2.setInstanceGroup(gateway);
        instanceMetadataView2.setInstanceStatus(InstanceStatus.SERVICES_HEALTHY);
        instanceMetadataView2.setDiscoveryFQDN("gateway-fqdn");

        InstanceMetaData instanceMetadataView3 = new InstanceMetaData();
        InstanceGroup idbroker = new InstanceGroup();
        idbroker.setGroupName("idbroker");
        instanceMetadataView3.setInstanceGroup(idbroker);
        instanceMetadataView3.setInstanceStatus(InstanceStatus.SERVICES_HEALTHY);
        instanceMetadataView3.setDiscoveryFQDN("idbroker-fqdn");

        InstanceMetaData instanceMetadataView4 = new InstanceMetaData();
        instanceMetadataView4.setInstanceGroup(gateway);
        instanceMetadataView4.setInstanceStatus(InstanceStatus.SERVICES_HEALTHY);
        instanceMetadataView4.setDiscoveryFQDN("gateway-fqdn1");

        InstanceMetaData instanceMetadataView5 = new InstanceMetaData();
        instanceMetadataView5.setInstanceGroup(idbroker);
        instanceMetadataView5.setInstanceStatus(InstanceStatus.SERVICES_HEALTHY);
        instanceMetadataView5.setDiscoveryFQDN("idbroker-fqdn1");

        List<InstanceMetadataView> instanceMetadataViewList = List.of(
                instanceMetadataView1,
                instanceMetadataView2,
                instanceMetadataView3,
                instanceMetadataView4,
                instanceMetadataView5
        );

        SdxClusterResponse sdxClusterResponse = new SdxClusterResponse();
        sdxClusterResponse.setClusterShape(SdxClusterShape.ENTERPRISE);
        sdxClusterResponse.setStatus(SdxClusterStatusResponse.NODE_FAILURE);
        sdxClusterResponse.setName("sdx");

        when(entitlementService.isValidateDistroxOperationsBySdxHealthEnabled(anyString())).thenReturn(false);
        List<SdxClusterResponse> sdxClusterResponseList = List.of(sdxClusterResponse);

        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> {
            DistroXOperationValidationView distroXOperationValidationView = underTest.validateDistroXCreateOperation("env_crn",
                    DistroXOperations.CREATE, sdxClusterResponseList, instanceMetadataViewList);
            assertFalse(distroXOperationValidationView.isAllowed());
        });
    }
}