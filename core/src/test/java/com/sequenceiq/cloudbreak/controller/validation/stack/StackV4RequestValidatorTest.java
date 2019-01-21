package com.sequenceiq.cloudbreak.controller.validation.stack;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType.HIVE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType.RANGER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprints.requests.BlueprintV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.requests.DatabaseV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.HostGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.InstanceTemplateV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.volume.VolumeV4Request;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult.State;
import com.sequenceiq.cloudbreak.controller.validation.template.InstanceTemplateV4RequestValidator;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;

@RunWith(MockitoJUnitRunner.class)
public class StackV4RequestValidatorTest extends StackRequestValidatorTestBase {

    private static final Long WORKSPACE_ID = 1L;

    private static final Long TEST_HIVE_ID = 1111L;

    private static final Long TEST_RANGER_ID = 2222L;

    private static final String TEST_LDAP_NAME = "ldap";

    private static final String TEST_HIVE_NAME = "hive";

    private static final String TEST_RANGER_NAME = "ranger";

    private static final String TEST_BP_NAME = "testBpName";

    private static final String RDS_ERROR_MESSAGE_FORMAT = "For a Datalake cluster (since you have selected a datalake ready blueprint) "
            + "you should provide at least one %s rds/database configuration to the Cluster request";

    private static final String LACK_OF_LDAP_MESSAGE = "For a Datalake cluster (since you have selected a datalake ready blueprint) you should provide an "
            + "LDAP configuration or its name/id to the Cluster request";

    @Spy
    private final InstanceTemplateV4RequestValidator templateRequestValidator = new InstanceTemplateV4RequestValidator();

    @Mock
    private BlueprintService blueprintService;

    @Mock
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @InjectMocks
    private StackRequestValidator underTest;

    @Mock
    private Blueprint blueprint;

    @Mock
    private RdsConfigService rdsConfigService;

    public StackV4RequestValidatorTest() {
        super(LoggerFactory.getLogger(StackV4RequestValidatorTest.class));
    }

    @Before
    public void setUp() {
        when(blueprintService.getByNameForWorkspaceId(anyString(), eq(WORKSPACE_ID))).thenReturn(blueprint);
        when(restRequestThreadLocalService.getRequestedWorkspaceId()).thenReturn(WORKSPACE_ID);
    }

    @Test
    public void testWithZeroRootVolumeSize() {
        assertNotNull(templateRequestValidator);
        StackV4Request stackRequest = stackRequestWithRootVolumeSize(0);
        ValidationResult validationResult = underTest.validate(stackRequest);
        assertEquals(State.ERROR, validationResult.getState());
    }

    @Test
    public void testWithNegativeRootVolumeSize() {
        StackV4Request stackRequest = stackRequestWithRootVolumeSize(-1);
        ValidationResult validationResult = underTest.validate(stackRequest);
        assertEquals(State.ERROR, validationResult.getState());
    }

    @Test
    public void testNullValueIsAllowedForRootVolumeSize() {
        StackV4Request stackRequest = stackRequestWithRootVolumeSize(null);
        ValidationResult validationResult = underTest.validate(stackRequest);
        assertEquals(State.VALID, validationResult.getState());
    }

    @Test
    public void testWithPositiveRootVolumeSize() {
        StackV4Request stackRequest = stackRequestWithRootVolumeSize(1);
        ValidationResult validationResult = underTest.validate(stackRequest);
        assertEquals(State.VALID, validationResult.getState());
    }

    @Test
    public void testWithLargerInstanceGroupSetThanHostGroups() {
        String plusOne = "very master";
        StackV4Request stackRequest = stackRequestWithInstanceAndHostGroups(
                Sets.newHashSet(plusOne, "master", "worker", "compute"),
                Sets.newHashSet("master", "worker", "compute")
        );

        ValidationResult validationResult = underTest.validate(stackRequest);
        assertEquals(State.ERROR, validationResult.getState());
        assertTrue(validationResult.getErrors().get(0).startsWith("There are instance groups in the request that do not have a corresponding host group: "
                + plusOne));
    }

    @Test
    public void testWithLargerHostGroupSetThanInstanceGroups() {
        StackV4Request stackRequest = stackRequestWithInstanceAndHostGroups(
                Sets.newHashSet("master", "worker", "compute"),
                Sets.newHashSet("super master", "master", "worker", "compute")
        );

        ValidationResult validationResult = underTest.validate(stackRequest);
        assertEquals(State.ERROR, validationResult.getState());
        assertTrue(validationResult.getErrors().get(0).startsWith("There are host groups in the request that do not have a corresponding instance group"));
    }

    @Test
    public void testWithBothGroupContainsDifferentValues() {
        StackV4Request stackRequest = stackRequestWithInstanceAndHostGroups(
                Sets.newHashSet("worker", "compute"),
                Sets.newHashSet("master", "worker")
        );

        ValidationResult validationResult = underTest.validate(stackRequest);
        assertEquals(State.ERROR, validationResult.getState());
        assertEquals(2L, validationResult.getErrors().size());
    }

    @Test
    public void testWhenBlueprintIsSharedServiceReadyAndThereIsOnlyHiveRdsWithNameThenErrorComes() {
        when(blueprintService.isDatalakeBlueprint(any())).thenReturn(true);
        StackV4Request request = stackRequest();
        request.getCluster().setDatabases(Set.of(TEST_HIVE_NAME));
        when(rdsConfigService.getByNameForWorkspaceId(TEST_HIVE_NAME, WORKSPACE_ID)).thenReturn(rdsConfig(HIVE));

        ValidationResult validationResult = underTest.validate(request);

        assertEquals(2L, validationResult.getErrors().size());
        assertTrue(validationResult.getErrors().stream().anyMatch(s -> s.equals(String.format(RDS_ERROR_MESSAGE_FORMAT, "Ranger"))));
        assertTrue(validationResult.getErrors().stream().anyMatch(s -> s.equals(LACK_OF_LDAP_MESSAGE)));

        verify(blueprintService, times(1)).getByNameForWorkspaceId(TEST_BP_NAME, WORKSPACE_ID);
        verify(blueprintService, times(1)).getByNameForWorkspaceId(anyString(), anyLong());
        verify(rdsConfigService, times(1)).getByNameForWorkspaceId(TEST_HIVE_NAME, WORKSPACE_ID);
        verify(rdsConfigService, times(1)).getByNameForWorkspaceId(anyString(), anyLong());
        verify(rdsConfigService, times(0)).get(anyLong());
    }

    @Test
    public void testWhenBlueprintIsSharedServiceReadyAndThereIsOnlyRangerRdsWithNameThenErrorComes() {
        when(blueprintService.isDatalakeBlueprint(any())).thenReturn(true);
        StackV4Request request = stackRequest();
        request.getCluster().setDatabases(Set.of(TEST_RANGER_NAME));
        when(rdsConfigService.getByNameForWorkspaceId(TEST_RANGER_NAME, WORKSPACE_ID)).thenReturn(rdsConfig(RANGER));

        ValidationResult validationResult = underTest.validate(request);

        assertEquals(2L, validationResult.getErrors().size());
        assertTrue(validationResult.getErrors().stream().anyMatch(s -> s.equals(String.format(RDS_ERROR_MESSAGE_FORMAT, "Hive"))));
        assertTrue(validationResult.getErrors().stream().anyMatch(s -> s.equals(LACK_OF_LDAP_MESSAGE)));

        verify(blueprintService, times(1)).getByNameForWorkspaceId(TEST_BP_NAME, WORKSPACE_ID);
        verify(blueprintService, times(1)).getByNameForWorkspaceId(anyString(), anyLong());
        verify(rdsConfigService, times(1)).getByNameForWorkspaceId(TEST_RANGER_NAME, WORKSPACE_ID);
        verify(rdsConfigService, times(1)).getByNameForWorkspaceId(anyString(), anyLong());
        verify(rdsConfigService, times(0)).get(anyLong());
    }

    @Test
    public void testWhenBlueprintIsSharedServiceReadyAndThereAreRangerAndHiveRdsWithNameButLdapDoesntThenErrorComes() {
        when(blueprintService.isDatalakeBlueprint(any())).thenReturn(true);
        StackV4Request request = stackRequest();
        request.getCluster().setDatabases(Set.of(TEST_RANGER_NAME, TEST_HIVE_NAME));
        when(rdsConfigService.getByNameForWorkspaceId(TEST_RANGER_NAME, WORKSPACE_ID)).thenReturn(rdsConfig(RANGER));
        when(rdsConfigService.getByNameForWorkspaceId(TEST_HIVE_NAME, WORKSPACE_ID)).thenReturn(rdsConfig(HIVE));

        ValidationResult validationResult = underTest.validate(request);

        assertEquals(1L, validationResult.getErrors().size());
        assertFalse(validationResult.getErrors().stream().anyMatch(s -> s.equals(String.format(RDS_ERROR_MESSAGE_FORMAT, "Hive"))));
        assertFalse(validationResult.getErrors().stream().anyMatch(s -> s.equals(String.format(RDS_ERROR_MESSAGE_FORMAT, "Ranger"))));
        assertTrue(validationResult.getErrors().stream().anyMatch(s -> s.equals(LACK_OF_LDAP_MESSAGE)));

        verify(blueprintService, times(1)).getByNameForWorkspaceId(TEST_BP_NAME, WORKSPACE_ID);
        verify(blueprintService, times(1)).getByNameForWorkspaceId(anyString(), anyLong());
        verify(rdsConfigService, times(1)).getByNameForWorkspaceId(TEST_RANGER_NAME, WORKSPACE_ID);
        verify(rdsConfigService, times(1)).getByNameForWorkspaceId(TEST_HIVE_NAME, WORKSPACE_ID);
        verify(rdsConfigService, times(2)).getByNameForWorkspaceId(anyString(), anyLong());
        verify(rdsConfigService, times(0)).get(anyLong());
    }

    @Test
    public void testWhenBlueprintIsSharedServiceReadyAndThereAreRangerAndHiveRdsWithNameAndLdapWithNameThenEverythingIsFine() {
        when(blueprintService.isDatalakeBlueprint(any())).thenReturn(true);
        StackV4Request request = stackRequest();
        request.getCluster().setDatabases(Set.of(TEST_RANGER_NAME, TEST_HIVE_NAME));
        request.getCluster().setLdapName(TEST_LDAP_NAME);
        when(rdsConfigService.getByNameForWorkspaceId(TEST_RANGER_NAME, WORKSPACE_ID)).thenReturn(rdsConfig(RANGER));
        when(rdsConfigService.getByNameForWorkspaceId(TEST_HIVE_NAME, WORKSPACE_ID)).thenReturn(rdsConfig(HIVE));

        ValidationResult validationResult = underTest.validate(request);

        assertValidationErrorIsEmpty(validationResult.getErrors());

        verify(blueprintService, times(1)).getByNameForWorkspaceId(TEST_BP_NAME, WORKSPACE_ID);
        verify(blueprintService, times(1)).getByNameForWorkspaceId(anyString(), anyLong());
        verify(rdsConfigService, times(1)).getByNameForWorkspaceId(TEST_RANGER_NAME, WORKSPACE_ID);
        verify(rdsConfigService, times(1)).getByNameForWorkspaceId(TEST_HIVE_NAME, WORKSPACE_ID);
        verify(rdsConfigService, times(2)).getByNameForWorkspaceId(anyString(), anyLong());
        verify(rdsConfigService, times(0)).get(anyLong());
    }

    @Test
    public void testWhenBlueprintIsSharedServiceReadyAndThereAreRangerAndHiveRdsWithBothIdAndLdapWithNameThenEverythingIsFine() {
        when(blueprintService.isDatalakeBlueprint(any())).thenReturn(true);
        StackV4Request request = stackRequest();
        request.getCluster().setDatabases(Collections.emptySet());
        request.getCluster().setLdapName(TEST_LDAP_NAME);
        when(rdsConfigService.get(TEST_RANGER_ID)).thenReturn(rdsConfig(RANGER));
        when(rdsConfigService.get(TEST_HIVE_ID)).thenReturn(rdsConfig(HIVE));

        ValidationResult validationResult = underTest.validate(request);

        assertValidationErrorIsEmpty(validationResult.getErrors());

        verify(blueprintService, times(1)).getByNameForWorkspaceId(TEST_BP_NAME, WORKSPACE_ID);
        verify(blueprintService, times(1)).getByNameForWorkspaceId(anyString(), anyLong());
        verify(rdsConfigService, times(1)).get(TEST_RANGER_ID);
        verify(rdsConfigService, times(1)).get(TEST_HIVE_ID);
        verify(rdsConfigService, times(2)).get(anyLong());
        verify(rdsConfigService, times(0)).getByNameForWorkspaceId(anyString(), anyLong());
    }

    @Test
    public void testWhenBlueprintIsSharedServiceReadyAndThereAreRangerAndHiveRdsWithBothRequestAndLdapWithNameThenEverythingIsFine() {
        when(blueprintService.isDatalakeBlueprint(any())).thenReturn(true);
        StackV4Request request = stackRequest();
        request.getCluster().setDatabases(Collections.emptySet());
        request.getCluster().setLdapName(TEST_LDAP_NAME);

        ValidationResult validationResult = underTest.validate(request);

        assertValidationErrorIsEmpty(validationResult.getErrors());

        verify(blueprintService, times(1)).getByNameForWorkspaceId(TEST_BP_NAME, WORKSPACE_ID);
        verify(blueprintService, times(1)).getByNameForWorkspaceId(anyString(), anyLong());
        verify(rdsConfigService, times(0)).get(anyLong());
        verify(rdsConfigService, times(0)).getByNameForWorkspaceId(anyString(), anyLong());
    }

    @Test
    public void testWhenBlueprintIsSharedServiceReadyAndThereAreRangerAndHiveRdsWithNameAndIdAndLdapWithNameThenEverythingIsFine() {
        when(blueprintService.isDatalakeBlueprint(any())).thenReturn(true);
        StackV4Request request = stackRequest();
        request.getCluster().setDatabases(Set.of(TEST_HIVE_NAME));
        request.getCluster().setLdapName(TEST_LDAP_NAME);
        when(rdsConfigService.getByNameForWorkspaceId(TEST_HIVE_NAME, WORKSPACE_ID)).thenReturn(rdsConfig(HIVE));
        when(rdsConfigService.get(TEST_RANGER_ID)).thenReturn(rdsConfig(RANGER));

        ValidationResult validationResult = underTest.validate(request);

        assertValidationErrorIsEmpty(validationResult.getErrors());

        verify(blueprintService, times(1)).getByNameForWorkspaceId(TEST_BP_NAME, WORKSPACE_ID);
        verify(blueprintService, times(1)).getByNameForWorkspaceId(anyString(), anyLong());
        verify(rdsConfigService, times(1)).get(TEST_RANGER_ID);
        verify(rdsConfigService, times(1)).get(anyLong());
        verify(rdsConfigService, times(1)).getByNameForWorkspaceId(TEST_HIVE_NAME, WORKSPACE_ID);
        verify(rdsConfigService, times(1)).getByNameForWorkspaceId(anyString(), anyLong());
    }

    @Test
    public void testWhenBlueprintIsSharedServiceReadyAndThereAreRangerAndHiveRdsWithIdAndNameAndLdapWithNameThenEverythingIsFine() {
        when(blueprintService.isDatalakeBlueprint(any())).thenReturn(true);
        StackV4Request request = stackRequest();
        request.getCluster().setDatabases(Set.of(TEST_RANGER_NAME));
        request.getCluster().setLdapName(TEST_LDAP_NAME);
        when(rdsConfigService.getByNameForWorkspaceId(TEST_RANGER_NAME, WORKSPACE_ID)).thenReturn(rdsConfig(RANGER));
        when(rdsConfigService.get(TEST_HIVE_ID)).thenReturn(rdsConfig(HIVE));

        ValidationResult validationResult = underTest.validate(request);

        assertValidationErrorIsEmpty(validationResult.getErrors());

        verify(blueprintService, times(1)).getByNameForWorkspaceId(TEST_BP_NAME, WORKSPACE_ID);
        verify(blueprintService, times(1)).getByNameForWorkspaceId(anyString(), anyLong());
        verify(rdsConfigService, times(1)).get(TEST_HIVE_ID);
        verify(rdsConfigService, times(1)).get(anyLong());
        verify(rdsConfigService, times(1)).getByNameForWorkspaceId(TEST_RANGER_NAME, WORKSPACE_ID);
        verify(rdsConfigService, times(1)).getByNameForWorkspaceId(anyString(), anyLong());
    }

    @Test
    public void testWhenBlueprintIsSharedServiceReadyAndThereAreRangerAndHiveRdsWithNameAndRequestAndLdapWithNameThenEverythingIsFine() {
        when(blueprintService.isDatalakeBlueprint(any())).thenReturn(true);
        StackV4Request request = stackRequest();
        request.getCluster().setDatabases(Set.of(TEST_HIVE_NAME));
        request.getCluster().setLdapName(TEST_LDAP_NAME);
        when(rdsConfigService.getByNameForWorkspaceId(TEST_HIVE_NAME, WORKSPACE_ID)).thenReturn(rdsConfig(HIVE));

        ValidationResult validationResult = underTest.validate(request);

        assertValidationErrorIsEmpty(validationResult.getErrors());

        verify(blueprintService, times(1)).getByNameForWorkspaceId(TEST_BP_NAME, WORKSPACE_ID);
        verify(blueprintService, times(1)).getByNameForWorkspaceId(anyString(), anyLong());
        verify(rdsConfigService, times(0)).get(anyLong());
        verify(rdsConfigService, times(1)).getByNameForWorkspaceId(TEST_HIVE_NAME, WORKSPACE_ID);
        verify(rdsConfigService, times(1)).getByNameForWorkspaceId(anyString(), anyLong());
    }

    @Test
    public void testWhenBlueprintIsSharedServiceReadyAndThereAreRangerAndHiveRdsWithRequestAndNameAndLdapWithNameThenEverythingIsFine() {
        when(blueprintService.isDatalakeBlueprint(any())).thenReturn(true);
        StackV4Request request = stackRequest();
        request.getCluster().setDatabases(Set.of(TEST_RANGER_NAME));
        request.getCluster().setLdapName(TEST_LDAP_NAME);
        when(rdsConfigService.getByNameForWorkspaceId(TEST_RANGER_NAME, WORKSPACE_ID)).thenReturn(rdsConfig(RANGER));

        ValidationResult validationResult = underTest.validate(request);

        assertValidationErrorIsEmpty(validationResult.getErrors());

        verify(blueprintService, times(1)).getByNameForWorkspaceId(TEST_BP_NAME, WORKSPACE_ID);
        verify(blueprintService, times(1)).getByNameForWorkspaceId(anyString(), anyLong());
        verify(rdsConfigService, times(0)).get(anyLong());
        verify(rdsConfigService, times(1)).getByNameForWorkspaceId(TEST_RANGER_NAME, WORKSPACE_ID);
        verify(rdsConfigService, times(1)).getByNameForWorkspaceId(anyString(), anyLong());
    }

    @Test
    public void testWhenBlueprintIsSharedServiceReadyAndThereAreRangerAndHiveRdsWithRequestAndIdAndLdapWithNameThenEverythingIsFine() {
        when(blueprintService.isDatalakeBlueprint(any())).thenReturn(true);
        StackV4Request request = stackRequest();
        request.getCluster().setDatabases(Collections.emptySet());
        request.getCluster().setLdapName(TEST_LDAP_NAME);
        when(rdsConfigService.get(TEST_RANGER_ID)).thenReturn(rdsConfig(RANGER));

        ValidationResult validationResult = underTest.validate(request);

        assertValidationErrorIsEmpty(validationResult.getErrors());

        verify(blueprintService, times(1)).getByNameForWorkspaceId(TEST_BP_NAME, WORKSPACE_ID);
        verify(blueprintService, times(1)).getByNameForWorkspaceId(anyString(), anyLong());
        verify(rdsConfigService, times(1)).get(TEST_RANGER_ID);
        verify(rdsConfigService, times(1)).get(anyLong());
        verify(rdsConfigService, times(0)).getByNameForWorkspaceId(anyString(), anyLong());
    }

    @Test
    public void testWhenBlueprintIsSharedServiceReadyAndThereIsNoLdapAndRdsConfigGivenThenErrorComes() {
        when(blueprintService.isDatalakeBlueprint(any())).thenReturn(true);
        StackV4Request request = stackRequest();
        request.getCluster().setDatabases(Collections.emptySet());
        request.getCluster().setLdapName(null);

        ValidationResult validationResult = underTest.validate(request);

        assertEquals(3L, validationResult.getErrors().size());

        verify(blueprintService, times(1)).getByNameForWorkspaceId(TEST_BP_NAME, WORKSPACE_ID);
        verify(blueprintService, times(1)).getByNameForWorkspaceId(anyString(), anyLong());
        verify(rdsConfigService, times(0)).get(anyLong());
        verify(rdsConfigService, times(0)).getByNameForWorkspaceId(anyString(), anyLong());
    }

    private StackV4Request stackRequestWithInstanceAndHostGroups(Set<String> instanceGroups, Set<String> hostGroups) {
        List<InstanceGroupV4Request> instanceGroupList = instanceGroups.stream()
                .map(ig -> getInstanceGroupV4Request(new InstanceTemplateV4Request(), ig))
                .collect(Collectors.toList());

        ClusterV4Request clusterRequest = new ClusterV4Request();
        BlueprintV4Request bpRequest = new BlueprintV4Request();
        bpRequest.setName(TEST_BP_NAME);
//        AmbariV4Request
//        clusterRequest.setAmbari(TEST_BP_NAME);
        return getStackV4Request(instanceGroupList, clusterRequest);
    }

    private StackV4Request stackRequest() {
        InstanceTemplateV4Request templateRequest = new InstanceTemplateV4Request();
        InstanceGroupV4Request instanceGroupRequest = getInstanceGroupV4Request(templateRequest, "master");
        ClusterV4Request clusterRequest = getCluster();
        return getStackV4Request(Collections.singletonList(instanceGroupRequest), clusterRequest);
    }

    private StackV4Request stackRequestWithRootVolumeSize(Integer rootVolumeSize) {
        InstanceTemplateV4Request templateRequest = new InstanceTemplateV4Request();
        templateRequest.setRootVolume(getRootVolume());
        InstanceGroupV4Request instanceGroupRequest = getInstanceGroupV4Request(templateRequest, "master");
        ClusterV4Request clusterRequest = getCluster();
        return getStackV4Request(Collections.singletonList(instanceGroupRequest), clusterRequest);
    }

    private VolumeV4Request getRootVolume() {
        VolumeV4Request root = new VolumeV4Request();
        root.setSize(100);
        return root;
    }

    private InstanceGroupV4Request getInstanceGroupV4Request(InstanceTemplateV4Request templateRequest, String master) {
        InstanceGroupV4Request instanceGroupRequest = new InstanceGroupV4Request();
        instanceGroupRequest.setName(master);
        instanceGroupRequest.setTemplate(templateRequest);
        return instanceGroupRequest;
    }

    private ClusterV4Request getCluster() {
        HostGroupV4Request hostGroupRequest = new HostGroupV4Request();
        ClusterV4Request clusterRequest = new ClusterV4Request();
        hostGroupRequest.setName("master");
        BlueprintV4Request bpRequest = new BlueprintV4Request();
        bpRequest.setName(TEST_BP_NAME);
//        clusterRequest.setBlueprintName(TEST_BP_NAME);
        return clusterRequest;
    }

    private StackV4Request getStackV4Request(List<InstanceGroupV4Request> instanceGroupRequests, ClusterV4Request clusterRequest) {
        StackV4Request stackRequest = new StackV4Request();
        stackRequest.setCluster(clusterRequest);
        stackRequest.setInstanceGroups(instanceGroupRequests);
        return stackRequest;
    }

    private RDSConfig rdsConfig(DatabaseType type) {
        RDSConfig rds = new RDSConfig();
        rds.setType(type.name());
        return rds;
    }

    private DatabaseV4Request rdsConfigRequest(DatabaseType type) {
        DatabaseV4Request request = new DatabaseV4Request();
        request.setType(type.name());
        return request;
    }
}