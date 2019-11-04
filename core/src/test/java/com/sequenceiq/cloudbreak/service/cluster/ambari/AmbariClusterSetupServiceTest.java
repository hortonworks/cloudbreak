package com.sequenceiq.cloudbreak.service.cluster.ambari;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.convert.ConversionService;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.blueprint.CentralBlueprintUpdater;
import com.sequenceiq.cloudbreak.cloud.model.AmbariDatabase;
import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.repository.HostMetadataRepository;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariOperationService;
import com.sequenceiq.cloudbreak.service.cluster.flow.recipe.RecipeEngine;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@RunWith(MockitoJUnitRunner.class)
public class AmbariClusterSetupServiceTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Mock
    private ConversionService conversionService;

    @Mock
    private StackService stackService;

    @Mock
    private ClusterService clusterService;

    @Mock
    private AmbariClientFactory clientFactory;

    @Mock
    private AmbariUserHandler ambariUserHandler;

    @Mock
    private AmbariClusterConnectorPollingResultChecker ambariClusterConnectorPollingResultChecker;

    @Mock
    private HostGroupService hostGroupService;

    @Mock
    private AmbariOperationService ambariOperationService;

    @Mock
    private RecipeEngine recipeEngine;

    @Mock
    private HostMetadataRepository hostMetadataRepository;

    @Mock
    private CloudbreakMessagesService cloudbreakMessagesService;

    @Mock
    private AmbariViewProvider ambariViewProvider;

    @Mock
    private AmbariClusterTemplateSubmitter ambariClusterTemplateSubmitter;

    @Mock
    private AmbariRepositoryVersionService ambariRepositoryVersionService;

    @Mock
    private CloudbreakEventService eventService;

    @Mock
    private HostGroupAssociationBuilder hostGroupAssociationBuilder;

    @Mock
    private AmbariPollingServiceProvider ambariPollingServiceProvider;

    @Mock
    private CentralBlueprintUpdater centralBlueprintUpdater;

    @Mock
    private AmbariClusterCreationSuccessHandler ambariClusterCreationSuccessHandler;

    @Mock
    private AmbariSmartSenseCapturer ambariSmartSenseCapturer;

    @InjectMocks
    private final AmbariClusterSetupService underTest = new AmbariClusterSetupService();

    @Test
    public void testApiAvailableWhenPollerReturnTrueThenApiShouldBeAvailable() throws CloudbreakSecuritySetupException {
        Stack stack = TestUtil.stack();
        Cluster cluster = TestUtil.cluster();
        stack.setCluster(cluster);
        AmbariClient ambariClient = ambariClient();

        when(ambariPollingServiceProvider.isAmbariAvailable(stack, ambariClient)).thenReturn(true);
        when(clientFactory.getAmbariClient(stack, stack.getCluster())).thenReturn(ambariClient);

        boolean available = underTest.available(stack);

        verify(ambariPollingServiceProvider, times(1)).isAmbariAvailable(stack, ambariClient);
        verify(clientFactory, times(1)).getAmbariClient(stack, stack.getCluster());
        Assert.assertTrue(available);
    }

    @Test
    public void testApiAvailableWhenPollerReturnFalseThenApiShouldBeNotAvailable() throws CloudbreakSecuritySetupException {
        Stack stack = TestUtil.stack();
        Cluster cluster = TestUtil.cluster();
        stack.setCluster(cluster);
        AmbariClient ambariClient = ambariClient();

        when(ambariPollingServiceProvider.isAmbariAvailable(stack, ambariClient)).thenReturn(false);
        when(clientFactory.getAmbariClient(stack, stack.getCluster())).thenReturn(ambariClient);

        boolean available = underTest.available(stack);

        verify(ambariPollingServiceProvider, times(1)).isAmbariAvailable(stack, ambariClient);
        verify(clientFactory, times(1)).getAmbariClient(stack, stack.getCluster());
        Assert.assertFalse(available);
    }

    private Map<String, List<String>> createStringListMap() {
        Map<String, List<String>> stringListMap = new HashMap<>();
        stringListMap.put("a1", Arrays.asList("assignment1", "assignment2"));
        return stringListMap;
    }

    private AmbariDatabase ambariDatabase() {
        AmbariDatabase ambariDatabase = new AmbariDatabase();
        ambariDatabase.setFancyName("mysql");
        ambariDatabase.setHost("10.0.0.2");
        ambariDatabase.setName("ambari-database");
        ambariDatabase.setPassword("password123#$@");
        ambariDatabase.setPort(3000);
        ambariDatabase.setUserName("ambari-database-user");
        ambariDatabase.setVendor("mysql");
        return ambariDatabase;
    }

    private AmbariClient ambariClient() {
        return new AmbariClient();
    }
}
