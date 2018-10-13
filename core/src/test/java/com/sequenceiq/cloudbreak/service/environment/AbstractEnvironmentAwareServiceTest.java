package com.sequenceiq.cloudbreak.service.environment;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.hamcrest.core.IsSame;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.domain.environment.EnvironmentAwareResource;
import com.sequenceiq.cloudbreak.domain.view.EnvironmentView;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.repository.environment.EnvironmentResourceRepository;

@RunWith(MockitoJUnitRunner.class)
public class AbstractEnvironmentAwareServiceTest {
    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Mock
    private EnvironmentViewService environmentViewService;

    @Mock
    private EnvironmentResourceRepository repository;

    @InjectMocks
    private AbstractEnvironmentAwareService<TestResource> underTest = new TestResourceService();

    @Test
    public void testFindAllInWorkspaceAndEnvironmentShouldReturnAllResourcesInWorkspaceIfEnvAndAttachGlobalNotProvided() {
        // GIVEN
        Set<TestResource> globalResources = createTestResources(5);
        Mockito.when(repository.findAllByWorkspaceId(1L)).thenReturn(globalResources);
        // WHEN
        Set<TestResource> actualResult = underTest.findAllInWorkspaceAndEnvironment(1L, null, null);
        // THEN
        Assert.assertEquals(5, actualResult.size());
        Assert.assertTrue(actualResult.containsAll(globalResources));
    }

    @Test
    public void testFindAllInWorkspaceAndEnvironmentShouldReturnAllResourcesInWorkspaceIfEnvNotProvidedAndAttachGlobalIsTrue() {
        // GIVEN
        Set<TestResource> globalResources = createTestResources(5);
        Mockito.when(repository.findAllByWorkspaceId(1L)).thenReturn(globalResources);
        // WHEN
        Set<TestResource> actualResult = underTest.findAllInWorkspaceAndEnvironment(1L, null, Boolean.TRUE);
        // THEN
        Assert.assertEquals(5, actualResult.size());
        Assert.assertTrue(actualResult.containsAll(globalResources));
    }

    @Test
    public void testFindAllInWorkspaceAndEnvironmentShouldReturnAllEnvResourcesInWorkspaceIfEnvNotProvidedAndAttachGlobalIsFalse() {
        // GIVEN
        Set<TestResource> globalResources = createTestResources(5);
        Mockito.when(repository.findAllByWorkspaceIdAndEnvironmentsIsNotNull(1L)).thenReturn(globalResources);
        // WHEN
        Set<TestResource> actualResult = underTest.findAllInWorkspaceAndEnvironment(1L, null, Boolean.FALSE);
        // THEN
        Assert.assertEquals(5, actualResult.size());
        Assert.assertTrue(actualResult.containsAll(globalResources));
    }

    @Test
    public void testFindAllInWorkspaceAndEnvironmentShouldReturnResourcesAttachedToEnvInWorkspaceIfEnvIsProvidedAndAttachGlobalIsFalse() {
        // GIVEN
        Set<TestResource> envResources = createTestResources(5);
        EnvironmentView envView = new EnvironmentView();
        envView.setId(1L);
        envView.setName("env");
        Mockito.when(environmentViewService.getByNameForWorkspaceId(envView.getName(), 1L)).thenReturn(envView);
        Mockito.when(repository.findAllByWorkspaceIdAndEnvironments(1L, envView)).thenReturn(envResources);
        // WHEN
        Set<TestResource> actualResult = underTest.findAllInWorkspaceAndEnvironment(1L, envView.getName(), Boolean.FALSE);
        // THEN
        Assert.assertEquals(5, actualResult.size());
        Assert.assertTrue(actualResult.containsAll(envResources));
    }

    @Test
    public void testFindAllInWorkspaceAndEnvironmentShouldReturnAllResourcesAttachedToEnvPlusGlobalResourcesInWorkspaceIfEnvIsProvidedAndAttachGlobalIsTrue() {
        // GIVEN
        Set<TestResource> globalResources = createTestResources(3);
        Set<TestResource> envResources = createTestResources(5);
        EnvironmentView envView = new EnvironmentView();
        envView.setId(1L);
        envView.setName("env");
        Mockito.when(environmentViewService.getByNameForWorkspaceId(envView.getName(), 1L)).thenReturn(envView);
        Mockito.when(repository.findAllByWorkspaceIdAndEnvironments(1L, envView)).thenReturn(envResources);
        Mockito.when(repository.findAllByWorkspaceIdAndEnvironmentsIsNull(1L)).thenReturn(globalResources);
        // WHEN
        Set<TestResource> actualResult = underTest.findAllInWorkspaceAndEnvironment(1L, envView.getName(), Boolean.TRUE);
        // THEN
        Assert.assertEquals(8, actualResult.size());
        Assert.assertTrue(actualResult.containsAll(envResources));
        Assert.assertTrue(actualResult.containsAll(globalResources));
    }

    @Test
    public void testFindAllInWorkspaceAndEnvironmentShouldReturnAllResourcesAttachedToEnvPlusGlobalResourcesInWorkspaceIfEnvIsProvidedButAttachGlobalIsNot() {
        // GIVEN
        Set<TestResource> globalResources = createTestResources(3);
        Set<TestResource> envResources = createTestResources(5);
        EnvironmentView envView = new EnvironmentView();
        envView.setId(1L);
        envView.setName("env");
        Mockito.when(environmentViewService.getByNameForWorkspaceId(envView.getName(), 1L)).thenReturn(envView);
        Mockito.when(repository.findAllByWorkspaceIdAndEnvironments(1L, envView)).thenReturn(envResources);
        Mockito.when(repository.findAllByWorkspaceIdAndEnvironmentsIsNull(1L)).thenReturn(globalResources);
        // WHEN
        Set<TestResource> actualResult = underTest.findAllInWorkspaceAndEnvironment(1L, envView.getName(), null);
        // THEN
        Assert.assertEquals(8, actualResult.size());
        Assert.assertTrue(actualResult.containsAll(envResources));
        Assert.assertTrue(actualResult.containsAll(globalResources));
    }

    @Test
    public void testFindAllInWorkspaceAndEnvironmentShouldThrowExceptinoWhenEnvIsNotFound() {
        // GIVEN
        RuntimeException expectedException = new RuntimeException();
        Mockito.when(environmentViewService.getByNameForWorkspaceId("env", 1L)).thenThrow(expectedException);
        thrown.expect(new IsSame<>(expectedException));
        // WHEN
        underTest.findAllInWorkspaceAndEnvironment(1L, "env", Boolean.TRUE);
    }

    private Set<TestResource> createTestResources(int num) {
        Set<TestResource> resources = new HashSet<>();
        for (int i = 0; i < num; i++) {
            resources.add(new TestResource());
        }
        return resources;
    }

    private static class TestResource implements  EnvironmentAwareResource {
        private Set<EnvironmentView> environments;

        @Override
        public Set<EnvironmentView> getEnvironments() {
            return environments;
        }

        @Override
        public void setEnvironments(Set<EnvironmentView> environments) {
            this.environments = environments;
        }

        @Override
        public Long getId() {
            return null;
        }

        @Override
        public Workspace getWorkspace() {
            return null;
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public void setWorkspace(Workspace workspace) {

        }

        @Override
        public WorkspaceResource getResource() {
            return null;
        }
    }

    private static class TestResourceService extends AbstractEnvironmentAwareService<TestResource> {
        @Inject
        private EnvironmentResourceRepository repository;

        @Override
        protected EnvironmentResourceRepository repository() {
            return repository;
        }

        @Override
        protected void prepareDeletion(TestResource resource) {

        }

        @Override
        protected void prepareCreation(TestResource resource) {

        }

        @Override
        public WorkspaceResource resource() {
            return null;
        }
    }
}
