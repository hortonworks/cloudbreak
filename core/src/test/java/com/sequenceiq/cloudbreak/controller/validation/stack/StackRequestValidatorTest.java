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
import com.sequenceiq.cloudbreak.api.model.TemplateRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.requests.LdapV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.requests.DatabaseV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.api.model.stack.StackRequest;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.ClusterRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.HostGroupV4Request;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceGroupRequest;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult.State;
import com.sequenceiq.cloudbreak.controller.validation.template.TemplateRequestValidator;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;

@RunWith(MockitoJUnitRunner.class)
public class StackRequestValidatorTest extends StackRequestValidatorTestBase {

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
    private final TemplateRequestValidator templateRequestValidator = new TemplateRequestValidator();

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

    public StackRequestValidatorTest() {
        super(LoggerFactory.getLogger(StackRequestValidatorTest.class));
    }

    @Before
    public void setUp() {
        when(blueprintService.getByNameForWorkspaceId(anyString(), eq(WORKSPACE_ID))).thenReturn(blueprint);
        when(restRequestThreadLocalService.getRequestedWorkspaceId()).thenReturn(WORKSPACE_ID);
    }

    @Test
    public void testWithZeroRootVolumeSize() {
        assertNotNull(templateRequestValidator);
        StackRequest stackRequest = stackRequestWithRootVolumeSize(0);
        ValidationResult validationResult = underTest.validate(stackRequest);
        assertEquals(State.ERROR, validationResult.getState());
    }

    @Test
    public void testWithNegativeRootVolumeSize() {
        StackRequest stackRequest = stackRequestWithRootVolumeSize(-1);
        ValidationResult validationResult = underTest.validate(stackRequest);
        assertEquals(State.ERROR, validationResult.getState());
    }

    @Test
    public void testNullValueIsAllowedForRootVolumeSize() {
        StackRequest stackRequest = stackRequestWithRootVolumeSize(null);
        ValidationResult validationResult = underTest.validate(stackRequest);
        assertEquals(State.VALID, validationResult.getState());
    }

    @Test
    public void testWithPositiveRootVolumeSize() {
        StackRequest stackRequest = stackRequestWithRootVolumeSize(1);
        ValidationResult validationResult = underTest.validate(stackRequest);
        assertEquals(State.VALID, validationResult.getState());
    }

    @Test
    public void testWithLargerInstanceGroupSetThanHostGroups() {
        String plusOne = "very master";
        StackRequest stackRequest = stackRequestWithInstanceAndHostGroups(
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
        StackRequest stackRequest = stackRequestWithInstanceAndHostGroups(
                Sets.newHashSet("master", "worker", "compute"),
                Sets.newHashSet("super master", "master", "worker", "compute")
        );

        ValidationResult validationResult = underTest.validate(stackRequest);
        assertEquals(State.ERROR, validationResult.getState());
        assertTrue(validationResult.getErrors().get(0).startsWith("There are host groups in the request that do not have a corresponding instance group"));
    }

    @Test
    public void testWithBothGroupContainsDifferentValues() {
        StackRequest stackRequest = stackRequestWithInstanceAndHostGroups(
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
        StackRequest request = stackRequest();
        request.getClusterRequest().setRdsConfigNames(Set.of(TEST_HIVE_NAME));
        request.getClusterRequest().setRdsConfigJsons(Collections.emptySet());
        request.getClusterRequest().setRdsConfigIds(Collections.emptySet());
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
        StackRequest request = stackRequest();
        request.getClusterRequest().setRdsConfigNames(Set.of(TEST_RANGER_NAME));
        request.getClusterRequest().setRdsConfigJsons(Collections.emptySet());
        request.getClusterRequest().setRdsConfigIds(Collections.emptySet());
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
        StackRequest request = stackRequest();
        request.getClusterRequest().setRdsConfigNames(Set.of(TEST_RANGER_NAME, TEST_HIVE_NAME));
        request.getClusterRequest().setRdsConfigJsons(Collections.emptySet());
        request.getClusterRequest().setRdsConfigIds(Collections.emptySet());
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
        StackRequest request = stackRequest();
        request.getClusterRequest().setRdsConfigNames(Set.of(TEST_RANGER_NAME, TEST_HIVE_NAME));
        request.getClusterRequest().setRdsConfigJsons(Collections.emptySet());
        request.getClusterRequest().setRdsConfigIds(Collections.emptySet());
        request.getClusterRequest().setLdapConfigName(TEST_LDAP_NAME);
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
    public void testWhenBlueprintIsSharedServiceReadyAndThereAreRangerAndHiveRdsWithNameAndLdapWithIdThenEverythingIsFine() {
        when(blueprintService.isDatalakeBlueprint(any())).thenReturn(true);
        StackRequest request = stackRequest();
        request.getClusterRequest().setRdsConfigNames(Set.of(TEST_RANGER_NAME, TEST_HIVE_NAME));
        request.getClusterRequest().setRdsConfigJsons(Collections.emptySet());
        request.getClusterRequest().setRdsConfigIds(Collections.emptySet());
        request.getClusterRequest().setLdapConfigId(1234L);
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
    public void testWhenBlueprintIsSharedServiceReadyAndThereAreRangerAndHiveRdsWithNameAndLdapWithRequestThenEverythingIsFine() {
        when(blueprintService.isDatalakeBlueprint(any())).thenReturn(true);
        StackRequest request = stackRequest();
        request.getClusterRequest().setRdsConfigNames(Set.of(TEST_RANGER_NAME, TEST_HIVE_NAME));
        request.getClusterRequest().setRdsConfigJsons(Collections.emptySet());
        request.getClusterRequest().setRdsConfigIds(Collections.emptySet());
        request.getClusterRequest().setLdapConfig(new LdapV4Request());
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
        StackRequest request = stackRequest();
        request.getClusterRequest().setRdsConfigIds(Set.of(TEST_RANGER_ID, TEST_HIVE_ID));
        request.getClusterRequest().setRdsConfigJsons(Collections.emptySet());
        request.getClusterRequest().setRdsConfigNames(Collections.emptySet());
        request.getClusterRequest().setLdapConfigName(TEST_LDAP_NAME);
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
        StackRequest request = stackRequest();
        request.getClusterRequest().setRdsConfigJsons(Set.of(rdsConfigRequest(HIVE), rdsConfigRequest(RANGER)));
        request.getClusterRequest().setRdsConfigIds(Collections.emptySet());
        request.getClusterRequest().setRdsConfigNames(Collections.emptySet());
        request.getClusterRequest().setLdapConfigName(TEST_LDAP_NAME);

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
        StackRequest request = stackRequest();
        request.getClusterRequest().setRdsConfigNames(Set.of(TEST_HIVE_NAME));
        request.getClusterRequest().setRdsConfigIds(Set.of(TEST_RANGER_ID));
        request.getClusterRequest().setRdsConfigJsons(Collections.emptySet());
        request.getClusterRequest().setLdapConfigName(TEST_LDAP_NAME);
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
        StackRequest request = stackRequest();
        request.getClusterRequest().setRdsConfigNames(Set.of(TEST_RANGER_NAME));
        request.getClusterRequest().setRdsConfigIds(Set.of(TEST_HIVE_ID));
        request.getClusterRequest().setRdsConfigJsons(Collections.emptySet());
        request.getClusterRequest().setLdapConfigName(TEST_LDAP_NAME);
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
        StackRequest request = stackRequest();
        request.getClusterRequest().setRdsConfigNames(Set.of(TEST_HIVE_NAME));
        request.getClusterRequest().setRdsConfigIds(Collections.emptySet());
        request.getClusterRequest().setRdsConfigJsons(Set.of(rdsConfigRequest(RANGER)));
        request.getClusterRequest().setLdapConfigName(TEST_LDAP_NAME);
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
        StackRequest request = stackRequest();
        request.getClusterRequest().setRdsConfigNames(Set.of(TEST_RANGER_NAME));
        request.getClusterRequest().setRdsConfigIds(Collections.emptySet());
        request.getClusterRequest().setRdsConfigJsons(Set.of(rdsConfigRequest(HIVE)));
        request.getClusterRequest().setLdapConfigName(TEST_LDAP_NAME);
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
        StackRequest request = stackRequest();
        request.getClusterRequest().setRdsConfigNames(Collections.emptySet());
        request.getClusterRequest().setRdsConfigIds(Set.of(TEST_RANGER_ID));
        request.getClusterRequest().setRdsConfigJsons(Set.of(rdsConfigRequest(HIVE)));
        request.getClusterRequest().setLdapConfigName(TEST_LDAP_NAME);
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
        StackRequest request = stackRequest();
        request.getClusterRequest().setRdsConfigNames(Collections.emptySet());
        request.getClusterRequest().setRdsConfigIds(Collections.emptySet());
        request.getClusterRequest().setRdsConfigJsons(Collections.emptySet());
        request.getClusterRequest().setLdapConfigName(null);
        request.getClusterRequest().setLdapConfig(null);
        request.getClusterRequest().setLdapConfigId(null);

        ValidationResult validationResult = underTest.validate(request);

        assertEquals(3L, validationResult.getErrors().size());

        verify(blueprintService, times(1)).getByNameForWorkspaceId(TEST_BP_NAME, WORKSPACE_ID);
        verify(blueprintService, times(1)).getByNameForWorkspaceId(anyString(), anyLong());
        verify(rdsConfigService, times(0)).get(anyLong());
        verify(rdsConfigService, times(0)).getByNameForWorkspaceId(anyString(), anyLong());
    }

    private StackRequest stackRequestWithInstanceAndHostGroups(Set<String> instanceGroups, Set<String> hostGroups) {
        List<InstanceGroupRequest> instanceGroupList = instanceGroups.stream()
                .map(ig -> getInstanceGroupRequest(new TemplateRequest(), ig))
                .collect(Collectors.toList());

        Set<HostGroupV4Request> hostGroupSet = hostGroups.stream()
                .map(hg -> {
                    HostGroupV4Request hostGroupRequest = new HostGroupV4Request();
                    hostGroupRequest.setName(hg);
                    return hostGroupRequest;
                })
                .collect(Collectors.toSet());

        ClusterRequest clusterRequest = new ClusterRequest();
        clusterRequest.setHostGroups(hostGroupSet);
        BlueprintV4Request bpRequest = new BlueprintV4Request();
        bpRequest.setName(TEST_BP_NAME);
        clusterRequest.setBlueprint(bpRequest);
        clusterRequest.setBlueprintName(TEST_BP_NAME);
        return getStackRequest(instanceGroupList, clusterRequest);
    }

    private StackRequest stackRequest() {
        TemplateRequest templateRequest = new TemplateRequest();
        InstanceGroupRequest instanceGroupRequest = getInstanceGroupRequest(templateRequest, "master");
        ClusterRequest clusterRequest = getClusterRequest();
        return getStackRequest(Collections.singletonList(instanceGroupRequest), clusterRequest);
    }

    private StackRequest stackRequestWithRootVolumeSize(Integer rootVolumeSize) {
        TemplateRequest templateRequest = new TemplateRequest();
        templateRequest.setRootVolumeSize(rootVolumeSize);
        InstanceGroupRequest instanceGroupRequest = getInstanceGroupRequest(templateRequest, "master");
        ClusterRequest clusterRequest = getClusterRequest();
        return getStackRequest(Collections.singletonList(instanceGroupRequest), clusterRequest);
    }

    private InstanceGroupRequest getInstanceGroupRequest(TemplateRequest templateRequest, String master) {
        InstanceGroupRequest instanceGroupRequest = new InstanceGroupRequest();
        instanceGroupRequest.setGroup(master);
        instanceGroupRequest.setTemplate(templateRequest);
        return instanceGroupRequest;
    }

    private ClusterRequest getClusterRequest() {
        HostGroupV4Request hostGroupRequest = new HostGroupV4Request();
        ClusterRequest clusterRequest = new ClusterRequest();
        hostGroupRequest.setName("master");
        clusterRequest.setHostGroups(Sets.newHashSet(hostGroupRequest));
        BlueprintV4Request bpRequest = new BlueprintV4Request();
        bpRequest.setName(TEST_BP_NAME);
        clusterRequest.setBlueprint(bpRequest);
        clusterRequest.setBlueprintName(TEST_BP_NAME);
        return clusterRequest;
    }

    private StackRequest getStackRequest(List<InstanceGroupRequest> instanceGroupRequests, ClusterRequest clusterRequest) {
        StackRequest stackRequest = new StackRequest();
        stackRequest.setClusterRequest(clusterRequest);
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