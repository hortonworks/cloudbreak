package com.sequenceiq.cloudbreak.service.cluster.ambari;

import static com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariMessages.AMBARI_CLUSTER_DISABLE_KERBEROS_FAILED;
import static com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariMessages.AMBARI_CLUSTER_PREPARE_DEKERBERIZING_ERROR;
import static com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariMessages.AMBARI_CLUSTER_PREPARE_DEKERBERIZING_FAILED;
import static com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariOperationType.DISABLE_KERBEROS_STATE;
import static com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariOperationType.PREPARE_DEKERBERIZING;
import static java.util.Collections.singletonMap;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.ambari.client.AmbariConnectionException;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.PollingResult;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariOperationService;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;

@RunWith(MockitoJUnitRunner.class)
public class AmbariClusterSecurityServiceTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Mock
    private ClusterService clusterService;

    @Mock
    private AmbariClientFactory clientFactory;

    @Mock
    private AmbariUserHandler ambariUserHandler;

    @Mock
    private AmbariClusterConnectorPollingResultChecker ambariClusterConnectorPollingResultChecker;

    @Mock
    private AmbariOperationService ambariOperationService;

    @Mock
    private CloudbreakMessagesService cloudbreakMessagesService;

    @Mock
    private AmbariSecurityConfigProvider ambariSecurityConfigProvider;

    @Mock
    private AmbariPollingServiceProvider ambariPollingServiceProvider;

    @InjectMocks
    private final AmbariClusterSecurityService underTest = new AmbariClusterSecurityService();

    @Test
    public void testApiReplaceUserNamePasswordWhenEverythingWorks() throws CloudbreakException {
        Stack stack = TestUtil.stack();
        Cluster cluster = TestUtil.cluster();
        stack.setCluster(cluster);
        AmbariClient ambariClient = Mockito.mock(AmbariClient.class);

        String newUserName = "admin";
        String newPassword = "newadmin";

        when(clientFactory.getAmbariClient(stack, cluster.getUserName().getRaw(), cluster.getPassword().getRaw())).thenReturn(ambariClient);
        when(ambariUserHandler.createAmbariUser(newUserName, newPassword, stack, ambariClient)).thenReturn(ambariClient);
        when(ambariClient.deleteUser(cluster.getUserName().getRaw())).thenReturn(ambariClient);

        underTest.replaceUserNamePassword(stack, newUserName, newPassword);

        verify(clientFactory, times(1)).getAmbariClient(stack, cluster.getUserName().getRaw(), cluster.getPassword().getRaw());
        verify(ambariUserHandler, times(1)).createAmbariUser(newUserName, newPassword, stack, ambariClient);
        verify(ambariClient, times(1)).deleteUser(cluster.getUserName().getRaw());
    }

    @Test
    public void testApiUpdateUserNamePasswordWhenEverythingWorks() throws CloudbreakException {
        Stack stack = TestUtil.stack();
        Cluster cluster = TestUtil.cluster();
        stack.setCluster(cluster);
        AmbariClient ambariClient = Mockito.mock(AmbariClient.class);

        String newPassword = "newadmin";

        when(clusterService.getById(stack.getCluster().getId())).thenReturn(cluster);
        when(clientFactory.getAmbariClient(stack, cluster.getUserName().getRaw(), cluster.getPassword().getRaw())).thenReturn(ambariClient);
        when(ambariUserHandler.changeAmbariPassword(cluster.getUserName().getRaw(), cluster.getPassword().getRaw(), newPassword, stack, ambariClient))
                .thenReturn(ambariClient);

        underTest.updateUserNamePassword(stack, newPassword);

        verify(clientFactory, times(1)).getAmbariClient(stack, cluster.getUserName().getRaw(), cluster.getPassword().getRaw());
        verify(clusterService, times(1)).getById(stack.getCluster().getId());
        verify(ambariUserHandler, times(1))
                .changeAmbariPassword(cluster.getUserName().getRaw(), cluster.getPassword().getRaw(), newPassword, stack, ambariClient);
    }

    @Test
    public void testApiUpdateUserNamePasswordWhenChangePasswordThrowExceptionAndAmbariVersionThrowExceptionThenShouldThrowCloudbreakException()
            throws CloudbreakException {
        Stack stack = TestUtil.stack();
        Cluster cluster = TestUtil.cluster();
        stack.setCluster(cluster);
        AmbariClient ambariClient = Mockito.mock(AmbariClient.class);

        String newPassword = "newadmin";

        when(clusterService.getById(stack.getCluster().getId())).thenReturn(cluster);
        when(clientFactory.getAmbariClient(stack, cluster.getUserName().getRaw(), cluster.getPassword().getRaw())).thenReturn(ambariClient);
        when(ambariUserHandler.changeAmbariPassword(cluster.getUserName().getRaw(), cluster.getPassword().getRaw(), newPassword, stack, ambariClient))
                .thenThrow(new CloudbreakException("test1"));

        thrown.expect(CloudbreakException.class);
        thrown.expectMessage("test1");

        underTest.updateUserNamePassword(stack, newPassword);

        verify(clientFactory, times(1)).getAmbariClient(stack, cluster.getUserName().getRaw(), cluster.getPassword().getRaw());
        verify(clusterService, times(1)).getById(stack.getCluster().getId());
        verify(ambariUserHandler, times(1))
                .changeAmbariPassword(cluster.getUserName().getRaw(), cluster.getPassword().getRaw(), newPassword, stack, ambariClient);
    }

    @Test
    public void testChangeOriginalAmbariCredentialsAndCreateCloudbreakUserWhenEverythingworksFineThenChangeDefaultUserNameAndPassword()
            throws CloudbreakException {
        Stack stack = TestUtil.stack();
        Cluster cluster = TestUtil.cluster();
        cluster.setUserName("admin1");
        stack.setCluster(cluster);
        AmbariClient ambariClient = Mockito.mock(AmbariClient.class);

        when(clientFactory.getDefaultAmbariClient(stack)).thenReturn(ambariClient);
        when(ambariSecurityConfigProvider.getAmbariUserName(cluster)).thenReturn("cloudbreak");
        when(ambariSecurityConfigProvider.getAmbariPassword(cluster)).thenReturn("cloudbreak123");
        when(ambariUserHandler.createAmbariUser("cloudbreak", "cloudbreak123", stack, ambariClient)).thenReturn(ambariClient);
        when(ambariUserHandler.createAmbariUser(cluster.getUserName().getRaw(), cluster.getPassword().getRaw(), stack, ambariClient)).thenReturn(ambariClient);
        when(ambariClient.deleteUser("admin")).thenReturn(ambariClient);

        underTest.changeOriginalAmbariCredentialsAndCreateCloudbreakUser(stack);

        verify(clientFactory, times(1)).getDefaultAmbariClient(stack);
        verify(ambariSecurityConfigProvider, times(1)).getAmbariUserName(stack.getCluster());
        verify(ambariSecurityConfigProvider, times(1)).getAmbariPassword(stack.getCluster());
        verify(ambariUserHandler, times(1)).createAmbariUser("cloudbreak", "cloudbreak123", stack, ambariClient);
        verify(ambariUserHandler, times(1))
                .createAmbariUser(cluster.getUserName().getRaw(), cluster.getPassword().getRaw(), stack, ambariClient);
        verify(ambariClient, times(1)).deleteUser("admin");
        verify(ambariUserHandler, times(0)).changeAmbariPassword(anyString(), anyString(), anyString(), nullable(Stack.class), nullable(AmbariClient.class));

    }

    @Test
    public void testChangeOriginalAmbariCredentialsAndCreateCloudbreakUserWhenAdminIsTheDefinedUserThenDefaultUserDoesNotChange() throws CloudbreakException {
        Stack stack = TestUtil.stack();
        Cluster cluster = TestUtil.cluster();
        cluster.setUserName("admin");
        cluster.setPassword("admin");
        stack.setCluster(cluster);
        AmbariClient ambariClient = Mockito.mock(AmbariClient.class);

        when(clientFactory.getDefaultAmbariClient(stack)).thenReturn(ambariClient);
        when(ambariSecurityConfigProvider.getAmbariUserName(cluster)).thenReturn("cloudbreak");
        when(ambariSecurityConfigProvider.getAmbariPassword(cluster)).thenReturn("cloudbreak123");
        when(ambariUserHandler.createAmbariUser("cloudbreak", "cloudbreak123", stack, ambariClient)).thenReturn(ambariClient);

        underTest.changeOriginalAmbariCredentialsAndCreateCloudbreakUser(stack);

        verify(clientFactory, times(1)).getDefaultAmbariClient(stack);
        verify(ambariSecurityConfigProvider, times(1)).getAmbariUserName(stack.getCluster());
        verify(ambariSecurityConfigProvider, times(1)).getAmbariPassword(stack.getCluster());
        verify(ambariUserHandler, times(1)).createAmbariUser("cloudbreak", "cloudbreak123", stack, ambariClient);
        verify(ambariClient, times(0)).deleteUser("admin");
        verify(ambariUserHandler, times(0)).changeAmbariPassword(anyString(), anyString(), anyString(), any(Stack.class), any(AmbariClient.class));

    }

    @Test
    public void testChangeOriginalAmbariCredentialsAndCreateCloudbreakUserWhenAdminIsTheDefinedUserAndPasswordIsNotAdminThenTryToChangeDefaultUser()
            throws CloudbreakException {
        Stack stack = TestUtil.stack();
        Cluster cluster = TestUtil.cluster();
        cluster.setUserName("admin");
        cluster.setPassword("admin1");
        stack.setCluster(cluster);
        AmbariClient ambariClient = Mockito.mock(AmbariClient.class);

        when(clientFactory.getDefaultAmbariClient(stack)).thenReturn(ambariClient);
        when(ambariSecurityConfigProvider.getAmbariUserName(cluster)).thenReturn("cloudbreak");
        when(ambariSecurityConfigProvider.getAmbariPassword(cluster)).thenReturn("cloudbreak123");
        when(ambariUserHandler.createAmbariUser("cloudbreak", "cloudbreak123", stack, ambariClient)).thenReturn(ambariClient);

        underTest.changeOriginalAmbariCredentialsAndCreateCloudbreakUser(stack);

        verify(clientFactory, times(1)).getDefaultAmbariClient(stack);
        verify(ambariSecurityConfigProvider, times(1)).getAmbariUserName(stack.getCluster());
        verify(ambariSecurityConfigProvider, times(1)).getAmbariPassword(stack.getCluster());
        verify(ambariUserHandler, times(1)).createAmbariUser("cloudbreak", "cloudbreak123", stack, ambariClient);
        verify(ambariClient, times(0)).deleteUser("admin");
        verify(ambariUserHandler, times(1))
                .changeAmbariPassword("admin", "admin", cluster.getPassword().getRaw(), stack, ambariClient);
    }

    @Test
    public void testPrepareSecurityWhenEverythingWorks() throws CloudbreakException {
        Stack stack = TestUtil.stack();
        Cluster cluster = TestUtil.cluster();
        stack.setCluster(cluster);
        AmbariClient ambariClient = Mockito.mock(AmbariClient.class);

        when(clientFactory.getAmbariClient(stack, stack.getCluster())).thenReturn(ambariClient);
        when(ambariClient.startService(anyString())).thenReturn(1);
        when(ambariClient.stopService(anyString())).thenReturn(1);
        Map<String, Integer> operationRequests = new HashMap<>();
        operationRequests.put("ZOOKEEPER_SERVICE_STATE", 1);
        operationRequests.put("HDFS_SERVICE_STATE", 1);
        operationRequests.put("YARN_SERVICE_STATE", 1);
        operationRequests.put("MAPREDUCE2_SERVICE_STATE", 1);
        operationRequests.put("KERBEROS_SERVICE_STATE", 1);
        ImmutablePair<PollingResult, Exception> pair = new ImmutablePair<>(PollingResult.SUCCESS, null);
        String failed = "failed";

        when(ambariOperationService.waitForOperations(stack, ambariClient, operationRequests, PREPARE_DEKERBERIZING)).thenReturn(pair);
        when(cloudbreakMessagesService.getMessage(AMBARI_CLUSTER_PREPARE_DEKERBERIZING_FAILED.code())).thenReturn("failed");
        doNothing().when(ambariClusterConnectorPollingResultChecker).checkPollingResult(pair.getLeft(), failed);

        underTest.prepareSecurity(stack);

        verify(ambariOperationService, times(1)).waitForOperations(stack, ambariClient, operationRequests, PREPARE_DEKERBERIZING);
        verify(cloudbreakMessagesService, times(1)).getMessage(AMBARI_CLUSTER_PREPARE_DEKERBERIZING_FAILED.code());
        verify(ambariClusterConnectorPollingResultChecker, times(1)).checkPollingResult(pair.getLeft(), failed);
    }

    @Test
    public void testPrepareSecurityWhenCancellationExceptionOccursThenShouldThrowCancellationException() throws CloudbreakException {
        Stack stack = TestUtil.stack();
        Cluster cluster = TestUtil.cluster();
        stack.setCluster(cluster);
        AmbariClient ambariClient = Mockito.mock(AmbariClient.class);

        when(clientFactory.getAmbariClient(stack, stack.getCluster())).thenReturn(ambariClient);
        when(ambariClient.startService(anyString())).thenReturn(1);
        when(ambariClient.stopService(anyString())).thenReturn(1);
        Map<String, Integer> operationRequests = new HashMap<>();
        operationRequests.put("ZOOKEEPER_SERVICE_STATE", 1);
        operationRequests.put("HDFS_SERVICE_STATE", 1);
        operationRequests.put("YARN_SERVICE_STATE", 1);
        operationRequests.put("MAPREDUCE2_SERVICE_STATE", 1);
        operationRequests.put("KERBEROS_SERVICE_STATE", 1);
        ImmutablePair<PollingResult, Exception> pair = new ImmutablePair<>(PollingResult.EXIT, null);
        String failed = "failed";

        when(ambariOperationService.waitForOperations(stack, ambariClient, operationRequests, PREPARE_DEKERBERIZING))
                .thenThrow(new CancellationException("cancel"));

        thrown.expect(CancellationException.class);
        thrown.expectMessage("cancel");

        underTest.prepareSecurity(stack);

        verify(ambariOperationService, times(1)).waitForOperations(stack, ambariClient, operationRequests, PREPARE_DEKERBERIZING);
        verify(cloudbreakMessagesService, times(1)).getMessage(AMBARI_CLUSTER_PREPARE_DEKERBERIZING_FAILED.code());
        verify(ambariClusterConnectorPollingResultChecker, times(1)).checkPollingResult(pair.getLeft(), failed);
    }

    @Test
    public void testPrepareSecurityWhenExceptionOccursWhichNotCancellationThenShouldThrowAmbariOperationFailedException() throws CloudbreakException {
        Stack stack = TestUtil.stack();
        Cluster cluster = TestUtil.cluster();
        stack.setCluster(cluster);
        AmbariClient ambariClient = Mockito.mock(AmbariClient.class);

        when(clientFactory.getAmbariClient(stack, stack.getCluster())).thenReturn(ambariClient);
        when(ambariClient.startService(anyString())).thenReturn(1);
        when(ambariClient.stopService(anyString())).thenReturn(1);
        Map<String, Integer> operationRequests = new HashMap<>();
        operationRequests.put("ZOOKEEPER_SERVICE_STATE", 1);
        operationRequests.put("HDFS_SERVICE_STATE", 1);
        operationRequests.put("YARN_SERVICE_STATE", 1);
        operationRequests.put("MAPREDUCE2_SERVICE_STATE", 1);
        operationRequests.put("KERBEROS_SERVICE_STATE", 1);
        ImmutablePair<PollingResult, Exception> pair = new ImmutablePair<>(PollingResult.EXIT, null);
        String failed = "failed";

        when(ambariOperationService.waitForOperations(stack, ambariClient, operationRequests, PREPARE_DEKERBERIZING))
                .thenThrow(new AmbariConnectionException(failed));
        when(cloudbreakMessagesService.getMessage(AMBARI_CLUSTER_PREPARE_DEKERBERIZING_ERROR.code()))
                .thenReturn("The de-registration of Kerberos principals couldn't be done, reason: " + failed);

        thrown.expect(AmbariOperationFailedException.class);
        thrown.expectMessage("The de-registration of Kerberos principals couldn't be done, reason: " + failed);

        underTest.prepareSecurity(stack);

        verify(ambariOperationService, times(1)).waitForOperations(stack, ambariClient, operationRequests, PREPARE_DEKERBERIZING);
        verify(cloudbreakMessagesService, times(1)).getMessage(AMBARI_CLUSTER_PREPARE_DEKERBERIZING_FAILED.code());
        verify(ambariClusterConnectorPollingResultChecker, times(1)).checkPollingResult(pair.getLeft(), failed);
    }

    @Test
    public void testDisableSecurityWhenEverythingWorks() throws CloudbreakException {
        Stack stack = TestUtil.stack();
        Cluster cluster = TestUtil.cluster();
        stack.setCluster(cluster);
        AmbariClient ambariClient = Mockito.mock(AmbariClient.class);

        when(clientFactory.getAmbariClient(stack, stack.getCluster())).thenReturn(ambariClient);
        when(ambariClient.disableKerberos()).thenReturn(1);
        Map<String, Integer> operationRequests = singletonMap("DISABLE_KERBEROS_REQUEST", 1);

        ImmutablePair<PollingResult, Exception> pair = new ImmutablePair<>(PollingResult.SUCCESS, null);
        String failed = "failed";

        when(ambariOperationService.waitForOperations(stack, ambariClient, operationRequests, DISABLE_KERBEROS_STATE)).thenReturn(pair);
        when(cloudbreakMessagesService.getMessage(AMBARI_CLUSTER_DISABLE_KERBEROS_FAILED.code())).thenReturn("failed");
        doNothing().when(ambariClusterConnectorPollingResultChecker).checkPollingResult(pair.getLeft(), failed);

        underTest.disableSecurity(stack);

        verify(ambariOperationService, times(1)).waitForOperations(stack, ambariClient, operationRequests, DISABLE_KERBEROS_STATE);
        verify(cloudbreakMessagesService, times(1)).getMessage(AMBARI_CLUSTER_DISABLE_KERBEROS_FAILED.code());
        verify(ambariClusterConnectorPollingResultChecker, times(1)).checkPollingResult(pair.getLeft(), failed);
    }

    @Test
    public void testDisableSecurityWhenCancellationExceptionOccursThenShouldThrowCancellationException() throws CloudbreakException {
        Stack stack = TestUtil.stack();
        Cluster cluster = TestUtil.cluster();
        stack.setCluster(cluster);
        AmbariClient ambariClient = Mockito.mock(AmbariClient.class);

        when(clientFactory.getAmbariClient(stack, stack.getCluster())).thenReturn(ambariClient);
        when(ambariClient.disableKerberos()).thenReturn(1);
        Map<String, Integer> operationRequests = singletonMap("DISABLE_KERBEROS_REQUEST", 1);
        ImmutablePair<PollingResult, Exception> pair = new ImmutablePair<>(PollingResult.EXIT, null);
        String failed = "failed";

        when(ambariOperationService.waitForOperations(stack, ambariClient, operationRequests, DISABLE_KERBEROS_STATE))
                .thenThrow(new CancellationException("cancel"));
        thrown.expect(CancellationException.class);
        thrown.expectMessage("cancel");

        underTest.disableSecurity(stack);

        verify(ambariOperationService, times(1)).waitForOperations(stack, ambariClient, operationRequests, DISABLE_KERBEROS_STATE);
        verify(cloudbreakMessagesService, times(1)).getMessage(AMBARI_CLUSTER_DISABLE_KERBEROS_FAILED.code());
        verify(ambariClusterConnectorPollingResultChecker, times(1)).checkPollingResult(pair.getLeft(), failed);
    }

    @Test
    public void testDisableSecurityWhenExceptionOccursWhichNotCancellationThenShouldThrowAmbariOperationFailedException() throws CloudbreakException {
        Stack stack = TestUtil.stack();
        Cluster cluster = TestUtil.cluster();
        stack.setCluster(cluster);
        AmbariClient ambariClient = Mockito.mock(AmbariClient.class);

        when(clientFactory.getAmbariClient(stack, stack.getCluster())).thenReturn(ambariClient);
        when(ambariClient.disableKerberos()).thenReturn(1);
        Map<String, Integer> operationRequests = singletonMap("DISABLE_KERBEROS_REQUEST", 1);
        ImmutablePair<PollingResult, Exception> pair = new ImmutablePair<>(PollingResult.EXIT, null);
        String failed = "failed";

        when(ambariOperationService.waitForOperations(stack, ambariClient, operationRequests, DISABLE_KERBEROS_STATE))
                .thenThrow(new AmbariConnectionException("failed"));

        thrown.expect(AmbariOperationFailedException.class);
        thrown.expectMessage("failed");

        underTest.disableSecurity(stack);

        verify(ambariOperationService, times(1)).waitForOperations(stack, ambariClient, operationRequests, DISABLE_KERBEROS_STATE);
        verify(cloudbreakMessagesService, times(1)).getMessage(AMBARI_CLUSTER_DISABLE_KERBEROS_FAILED.code());
        verify(ambariClusterConnectorPollingResultChecker, times(1)).checkPollingResult(pair.getLeft(), failed);
    }

}
