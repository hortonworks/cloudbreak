package com.sequenceiq.cloudbreak.service.template;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.verification.VerificationMode;

import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.ClusterTemplateV4Type;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.DatalakeRequired;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.projection.ClusterTemplateStatusView;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterTemplate;
import com.sequenceiq.cloudbreak.domain.view.ClusterTemplateView;
import com.sequenceiq.cloudbreak.init.clustertemplate.DefaultClusterTemplateCache;
import com.sequenceiq.cloudbreak.repository.cluster.ClusterTemplateViewRepository;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintListFilters;
import com.sequenceiq.cloudbreak.util.TestConstants;
import com.sequenceiq.distrox.v1.distrox.service.HybridClusterTemplateValidator;
import com.sequenceiq.distrox.v1.distrox.service.InternalClusterTemplateValidator;

@ExtendWith(MockitoExtension.class)
public class ClusterTemplateViewServiceTest {

    private static final VerificationMode ONCE = times(1);

    private static final String TEST_RUNTIME = "someRuntimeVersion";

    private static final String TEST_ENV_CRN = "someEnvCrn";

    private static final Long WORKSPACE_ID = 0L;

    @Mock
    private ClusterTemplateViewRepository repository;

    @Mock
    private InternalClusterTemplateValidator internalClusterTemplateValidator;

    @Mock
    private HybridClusterTemplateValidator hybridClusterTemplateValidator;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private DefaultClusterTemplateCache defaultClusterTemplateCache;

    @Mock
    private BlueprintListFilters blueprintListFilters;

    @InjectMocks
    private ClusterTemplateViewService underTest;

    @Test
    public void testPrepareCreation() {
        BadRequestException expectedException = assertThrows(BadRequestException.class, () -> underTest.prepareCreation(new ClusterTemplateView()));

        assertEquals("Cluster template creation is not supported from ClusterTemplateViewService", expectedException.getMessage());
    }

    @Test
    public void testPrepareDeletion() {
        BadRequestException expectedException = assertThrows(BadRequestException.class, () -> underTest.prepareDeletion(new ClusterTemplateView()));

        assertEquals("Cluster template deletion is not supported from ClusterTemplateViewService", expectedException.getMessage());
    }

    @Test
    public void testWhenFindAllAvailableViewInWorkspaceIsCalledThenItsResultSetShouldBeReturnedWithoutFiltering() {
        ClusterTemplateView repositoryResult = new ClusterTemplateView();
        Set<ClusterTemplateView> resultSetFromRepository = Set.of(repositoryResult);
        when(repository.findAllActive(WORKSPACE_ID)).thenReturn(resultSetFromRepository);
        when(internalClusterTemplateValidator.shouldPopulate(repositoryResult, false)).thenReturn(true);

        Set<ClusterTemplateView> result = ThreadBasedUserCrnProvider.doAs(TestConstants.CRN, () -> underTest.findAllActive(WORKSPACE_ID, false));

        assertNotNull(result);
        assertEquals(resultSetFromRepository.size(), result.size());
        assertTrue(resultSetFromRepository.containsAll(result));

        verify(repository, times(1)).findAllActive(anyLong());
        verify(repository, times(1)).findAllActive(WORKSPACE_ID);
    }

    @Test
    public void testWhenFindAllAvailableViewInWorkspaceIsCalledThenItsResultSetShouldBeNotReturned() {
        ClusterTemplateView repositoryResult = new ClusterTemplateView();
        Set<ClusterTemplateView> resultSetFromRepository = Set.of(repositoryResult);
        when(repository.findAllActive(WORKSPACE_ID)).thenReturn(resultSetFromRepository);
        when(internalClusterTemplateValidator.shouldPopulate(repositoryResult, false)).thenReturn(false);

        Set<ClusterTemplateView> result = ThreadBasedUserCrnProvider.doAs(TestConstants.CRN, () -> underTest.findAllActive(WORKSPACE_ID, false));

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @ParameterizedTest
    @EnumSource(CloudPlatform.class)
    public void testFindAllUserManagedAndDefaultByEnvironmentCrnWhenRepositoryCantFindAnyThenEmptySetShouldReturn(CloudPlatform cloudPlatform) {
        Set<ClusterTemplateView> expectedEmptyResultSet = new LinkedHashSet<>();
        when(repository.findAllUserManagedAndDefaultByEnvironmentCrn(any(), any(), any(), any())).thenReturn(expectedEmptyResultSet);

        Set<ClusterTemplateView> result = ThreadBasedUserCrnProvider.doAs(TestConstants.CRN,
                () -> underTest.findAllUserManagedAndDefaultByEnvironmentCrn(WORKSPACE_ID, TEST_ENV_CRN, cloudPlatform.name(), TEST_RUNTIME, false, null));

        assertEquals(expectedEmptyResultSet, result);

        verify(repository, ONCE).findAllUserManagedAndDefaultByEnvironmentCrn(any(), any(), any(), any());
        verify(repository, ONCE).findAllUserManagedAndDefaultByEnvironmentCrn(WORKSPACE_ID, TEST_ENV_CRN, cloudPlatform.name(), TEST_RUNTIME);
    }

    @ParameterizedTest
    @EnumSource(CloudPlatform.class)
    public void testFindAllUserManagedAndDefaultByEnvironmentCrnWhenRepositoryCanFindClusterTemplatesThenNonEmptySetShouldReturn(CloudPlatform cloudPlatform) {
        ClusterTemplateView match = new ClusterTemplateView();
        match.setType(ClusterTemplateV4Type.DATAENGINEERING);
        Set<ClusterTemplateView> expectedNonEmptyResultSet = Set.of(match);
        when(repository.findAllUserManagedAndDefaultByEnvironmentCrn(any(), any(), any(), any())).thenReturn(expectedNonEmptyResultSet);
        when(internalClusterTemplateValidator.shouldPopulate(match, false)).thenReturn(true);
        when(hybridClusterTemplateValidator.shouldPopulate(match, null)).thenReturn(true);

        Set<ClusterTemplateView> result = ThreadBasedUserCrnProvider.doAs(TestConstants.CRN,
                () -> underTest.findAllUserManagedAndDefaultByEnvironmentCrn(WORKSPACE_ID, TEST_ENV_CRN, cloudPlatform.name(), TEST_RUNTIME, false, null));

        assertEquals(expectedNonEmptyResultSet, result);
        assertEquals(expectedNonEmptyResultSet.size(), result.size());
        assertEquals(match, result.stream().findFirst().get());

        verify(repository, ONCE).findAllUserManagedAndDefaultByEnvironmentCrn(any(), any(), any(), any());
        verify(repository, ONCE).findAllUserManagedAndDefaultByEnvironmentCrn(WORKSPACE_ID, TEST_ENV_CRN, cloudPlatform.name(), TEST_RUNTIME);
    }

    @Test
    public void testGetStatusViewByResourceCrnWhenShouldGetFromGlobalDefaultByCrnIsTrue() {
        String resourceCrn = "crn:cdp:iam:us-west-1:acc:template:default-template";
        ClusterTemplate globalDefaultTemplate = mock(ClusterTemplate.class);
        when(globalDefaultTemplate.getStatus()).thenReturn(ResourceStatus.DEFAULT);

        when(entitlementService.isGlobalDefaultTemplateEnabled(anyString())).thenReturn(true);
        when(defaultClusterTemplateCache.getDefaultClusterTemplateByResourceCrn(resourceCrn)).thenReturn(Optional.of(globalDefaultTemplate));

        ClusterTemplateStatusView result = ThreadBasedUserCrnProvider.doAs(TestConstants.CRN, () ->
                underTest.getStatusViewByResourceCrn(resourceCrn)
        );

        assertNotNull(result);
        assertEquals(ResourceStatus.DEFAULT, result.getStatus());
        verifyNoInteractions(repository);
    }

    @Test
    public void testGetStatusViewByResourceCrnWhenGlobalDefaultTemplateIsDisabled() {
        String resourceCrn = "crn:cdp:iam:us-west-1:acc:template:some-template";
        ClusterTemplateStatusView dbStatusView = mock(ClusterTemplateStatusView.class);

        when(entitlementService.isGlobalDefaultTemplateEnabled(anyString())).thenReturn(false);
        when(repository.findViewByResourceCrn(resourceCrn)).thenReturn(dbStatusView);

        ClusterTemplateStatusView result = ThreadBasedUserCrnProvider.doAs(TestConstants.CRN, () ->
                underTest.getStatusViewByResourceCrn(resourceCrn)
        );

        assertNotNull(result);
        assertEquals(dbStatusView, result);
        verifyNoInteractions(defaultClusterTemplateCache);
    }

    @Test
    public void testGetStatusViewByResourceCrnWhenCrnIsNotInGlobalDefaults() {
        String resourceCrn = "crn:cdp:iam:us-west-1:acc:template:some-template";
        ClusterTemplateStatusView dbStatusView = mock(ClusterTemplateStatusView.class);

        when(entitlementService.isGlobalDefaultTemplateEnabled(anyString())).thenReturn(true);
        when(defaultClusterTemplateCache.getDefaultClusterTemplateByResourceCrn(resourceCrn)).thenReturn(Optional.empty());
        when(repository.findViewByResourceCrn(resourceCrn)).thenReturn(dbStatusView);

        ClusterTemplateStatusView result = ThreadBasedUserCrnProvider.doAs(TestConstants.CRN, () ->
                underTest.getStatusViewByResourceCrn(resourceCrn)
        );

        assertNotNull(result);
        assertEquals(dbStatusView, result);
        verify(repository, ONCE).findViewByResourceCrn(resourceCrn);
    }

    @Test
    public void testGetStatusViewByResourceCrnWhenNotFoundInGlobalDefaultsAndDatabaseThenThrowsNotFound() {
        String resourceCrn = "crn:cdp:iam:us-west-1:acc:template:missing-template";

        when(entitlementService.isGlobalDefaultTemplateEnabled(anyString())).thenReturn(true);
        when(defaultClusterTemplateCache.getDefaultClusterTemplateByResourceCrn(resourceCrn)).thenReturn(Optional.empty());
        when(repository.findViewByResourceCrn(resourceCrn)).thenReturn(null);

        assertThrows(NotFoundException.class, () -> ThreadBasedUserCrnProvider.doAs(TestConstants.CRN, () ->
                underTest.getStatusViewByResourceCrn(resourceCrn)
        ));
    }

    @Test
    public void testFindAllActiveWhenGlobalDefaultEnabled() {
        when(entitlementService.isGlobalDefaultTemplateEnabled(anyString())).thenReturn(true);
        when(entitlementService.isLakehouseOptimizerEnabled(anyString())).thenReturn(true);

        ClusterTemplate firstGlobal = createClusterTemplate("first-global", "crn:g1", ResourceStatus.DEFAULT, "AWS", "7.2.10");
        ClusterTemplate secondGlobal = createClusterTemplate("second-global", "crn:g2", ResourceStatus.DEFAULT, "AWS", "7.2.10");

        List<ClusterTemplate> templates = List.of(firstGlobal, secondGlobal);
        when(defaultClusterTemplateCache.getDefaultClusterTemplates()).thenReturn(templates);

        ClusterTemplateView dbUserTemplate = new ClusterTemplateView();
        dbUserTemplate.setName("db-user-template");
        dbUserTemplate.setStatus(ResourceStatus.USER_MANAGED);

        ClusterTemplateView dbDefaultTemplate = new ClusterTemplateView();
        dbDefaultTemplate.setName("db-default-template");
        dbDefaultTemplate.setStatus(ResourceStatus.DEFAULT);

        when(repository.findAllActive(WORKSPACE_ID)).thenReturn(Set.of(dbUserTemplate, dbDefaultTemplate));
        when(internalClusterTemplateValidator.shouldPopulate(any(ClusterTemplateView.class), any(Boolean.class))).thenReturn(true);

        Set<ClusterTemplateView> result = ThreadBasedUserCrnProvider.doAs(TestConstants.CRN, () ->
                underTest.findAllActive(WORKSPACE_ID, false)
        );

        assertNotNull(result);
        assertEquals(3, result.size());

        // Assert elements exist in the collection by querying properties via Hamcrest matchers
        assertThat(result, hasItem(hasProperty("name", is("second-global"))));
        assertThat(result, hasItem(hasProperty("name", is("first-global"))));
        assertThat(result, hasItem(hasProperty("name", is("db-user-template"))));
    }

    @Test
    public void testFindAllUserManagedAndDefaultByEnvironmentCrnWhenGlobalDefaultEnabledAndLakehouseOptimizerDisabled() {
        String envCrn = "envCrn";
        String cloudPlatform = "AWS";
        String runtime = "7.2.10";

        when(entitlementService.isGlobalDefaultTemplateEnabled(anyString())).thenReturn(true);
        when(entitlementService.isLakehouseOptimizerEnabled(anyString())).thenReturn(false);

        ClusterTemplate globalOptimized = createClusterTemplate("lakehouse-opt", "crn:opt", ResourceStatus.DEFAULT, "AWS", "7.2.10");
        ClusterTemplate globalStandard = createClusterTemplate("standard-template", "crn:std", ResourceStatus.DEFAULT, "AWS", "7.2.10");
        ClusterTemplate globalMismatchedPlatform = createClusterTemplate("mismatched-platform", "crn:mis", ResourceStatus.DEFAULT, "AZURE", "7.2.10");

        List<ClusterTemplate> templates = List.of(globalOptimized, globalStandard, globalMismatchedPlatform);
        when(defaultClusterTemplateCache.getDefaultClusterTemplates()).thenReturn(templates);

        when(blueprintListFilters.isLakehouseOptimizer("lakehouse-opt")).thenReturn(true);
        when(blueprintListFilters.isLakehouseOptimizer("standard-template")).thenReturn(false);
        when(blueprintListFilters.isLakehouseOptimizer("mismatched-platform")).thenReturn(false);

        ClusterTemplateView userTemplate = new ClusterTemplateView();
        userTemplate.setName("user-template");
        userTemplate.setCloudPlatform("AWS");
        userTemplate.setClouderaRuntimeVersion("7.2.10");

        when(repository.findAllUserManagedByEnvironmentCrn(WORKSPACE_ID, envCrn, cloudPlatform, runtime))
                .thenReturn(Set.of(userTemplate));

        when(internalClusterTemplateValidator.shouldPopulate(any(ClusterTemplateView.class), any(Boolean.class)))
                .thenReturn(true);
        when(hybridClusterTemplateValidator.shouldPopulate(any(ClusterTemplateView.class), any(Boolean.class)))
                .thenReturn(true);

        Set<ClusterTemplateView> result = ThreadBasedUserCrnProvider.doAs(TestConstants.CRN, () ->
                underTest.findAllUserManagedAndDefaultByEnvironmentCrn(WORKSPACE_ID, envCrn, cloudPlatform, runtime, false, false)
        );

        assertNotNull(result);
        assertEquals(2, result.size());

        ClusterTemplateView convertedGlobal = result.stream()
                .filter(view -> "standard-template".equals(view.getName()))
                .findFirst()
                .orElse(null);

        assertNotNull(convertedGlobal);
        assertEquals("crn:std", convertedGlobal.getResourceCrn());
        assertEquals("standard-template description", convertedGlobal.getDescription());
        assertEquals(123456L, convertedGlobal.getCreated());
        assertEquals(ClusterTemplateV4Type.DATAENGINEERING, convertedGlobal.getType());
        assertEquals(ResourceStatus.DEFAULT, convertedGlobal.getStatus());
        assertEquals(DatalakeRequired.REQUIRED, convertedGlobal.getDatalakeRequired());
        assertEquals("content", convertedGlobal.getTemplateContent());

        verify(repository, ONCE).findAllUserManagedByEnvironmentCrn(WORKSPACE_ID, envCrn, cloudPlatform, runtime);
    }

    @Test
    public void testFindAllUserManagedAndDefaultByEnvironmentCrnWhenGlobalDefaultEnabledAndLakehouseOptimizerEnabled() {
        String envCrn = "envCrn";
        String cloudPlatform = "AWS";
        String runtime = "7.2.10";

        when(entitlementService.isGlobalDefaultTemplateEnabled(anyString())).thenReturn(true);
        when(entitlementService.isLakehouseOptimizerEnabled(anyString())).thenReturn(true);

        ClusterTemplate globalOptimized = createClusterTemplate("lakehouse-opt", "crn:opt", ResourceStatus.DEFAULT, "AWS", "7.2.10");
        when(defaultClusterTemplateCache.getDefaultClusterTemplates()).thenReturn(List.of(globalOptimized));

        when(repository.findAllUserManagedByEnvironmentCrn(WORKSPACE_ID, envCrn, cloudPlatform, runtime))
                .thenReturn(Collections.emptySet());

        when(internalClusterTemplateValidator.shouldPopulate(any(ClusterTemplateView.class), any(Boolean.class)))
                .thenReturn(true);
        when(hybridClusterTemplateValidator.shouldPopulate(any(ClusterTemplateView.class), any(Boolean.class)))
                .thenReturn(true);

        Set<ClusterTemplateView> result = ThreadBasedUserCrnProvider.doAs(TestConstants.CRN, () ->
                underTest.findAllUserManagedAndDefaultByEnvironmentCrn(WORKSPACE_ID, envCrn, cloudPlatform, runtime, false, false)
        );

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("lakehouse-opt", result.iterator().next().getName());
        verifyNoInteractions(blueprintListFilters);
    }

    private ClusterTemplate createClusterTemplate(String name, String crn, ResourceStatus status, String cloudPlatform, String runtime) {
        ClusterTemplate template = new ClusterTemplate();
        template.setName(name);
        template.setResourceCrn(crn);
        template.setDescription(name + " description");
        template.setStatus(status);
        template.setCloudPlatform(cloudPlatform);
        template.setClouderaRuntimeVersion(runtime);
        template.setCreated(123456L);
        template.setType(ClusterTemplateV4Type.DATAENGINEERING);
        template.setDatalakeRequired(DatalakeRequired.REQUIRED);
        template.setTemplateContent("content");
        return template;
    }
}