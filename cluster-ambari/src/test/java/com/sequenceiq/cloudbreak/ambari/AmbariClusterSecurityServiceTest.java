package com.sequenceiq.cloudbreak.ambari;

import static com.sequenceiq.cloudbreak.ambari.AmbariMessages.AMBARI_CLUSTER_DISABLE_KERBEROS_FAILED;
import static com.sequenceiq.cloudbreak.ambari.AmbariMessages.AMBARI_CLUSTER_PREPARE_DEKERBERIZING_ERROR;
import static com.sequenceiq.cloudbreak.ambari.AmbariMessages.AMBARI_CLUSTER_PREPARE_DEKERBERIZING_FAILED;
import static com.sequenceiq.cloudbreak.ambari.AmbariOperationType.DISABLE_KERBEROS_STATE;
import static com.sequenceiq.cloudbreak.ambari.AmbariOperationType.PREPARE_DEKERBERIZING;
import static java.util.Collections.singletonMap;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.ambari.client.AmbariConnectionException;
import com.sequenceiq.cloudbreak.ambari.flow.AmbariOperationService;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.cluster.service.ClusterConnectorPollingResultChecker;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.polling.PollingResult;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

@RunWith(MockitoJUnitRunner.class)
public class AmbariClusterSecurityServiceTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Mock
    private AmbariClientFactory clientFactory;

    @Mock
    private AmbariUserHandler ambariUserHandler;

    @Mock
    private ClusterConnectorPollingResultChecker clusterConnectorPollingResultChecker;

    @Mock
    private AmbariOperationService ambariOperationService;

    @Mock
    private CloudbreakMessagesService cloudbreakMessagesService;

    @Mock
    private AmbariSecurityConfigProvider ambariSecurityConfigProvider;

    @Mock
    private AmbariClient ambariClient;

    private Stack stack = TestUtil.stack();

    private HttpClientConfig clientConfig = new HttpClientConfig("1.1.1.1");

    @InjectMocks
    private final AmbariClusterSecurityService underTest = new AmbariClusterSecurityService(stack, clientConfig);

    @Test
    public void testApiReplaceUserNamePasswordWhenEverythingWorks() throws CloudbreakException, IOException, URISyntaxException {
        Cluster cluster = TestUtil.cluster();
        stack.setCluster(cluster);

        String newUserName = "admin";
        String newPassword = "newadmin";

        when(ambariUserHandler.createAmbariUser(newUserName, newPassword, stack, ambariClient, clientConfig)).thenReturn(ambariClient);
        when(ambariClient.deleteUser(cluster.getUserName())).thenReturn(ambariClient);

        underTest.replaceUserNamePassword(newUserName, newPassword);

        verify(ambariUserHandler, times(1)).createAmbariUser(newUserName, newPassword, stack, ambariClient, clientConfig);
        verify(ambariClient, times(1)).deleteUser(cluster.getUserName());
    }

    @Test
    public void testApiUpdateUserNamePasswordWhenEverythingWorks() throws CloudbreakException {
        Cluster cluster = TestUtil.cluster();
        stack.setCluster(cluster);

        String newPassword = "newadmin";

        when(ambariUserHandler.changeAmbariPassword(cluster.getUserName(), cluster.getPassword(), newPassword, stack, ambariClient, clientConfig))
                .thenReturn(ambariClient);

        underTest.updateUserNamePassword(newPassword);

        verify(ambariUserHandler, times(1))
                .changeAmbariPassword(cluster.getUserName(), cluster.getPassword(), newPassword, stack, ambariClient, clientConfig);
    }

    @Test
    public void testApiUpdateUserNamePasswordWhenChangePasswordThrowExceptionAndAmbariVersionThrowExceptionThenShouldThrowCloudbreakException()
            throws CloudbreakException {
        Cluster cluster = TestUtil.cluster();
        stack.setCluster(cluster);

        String newPassword = "newadmin";

        when(ambariUserHandler.changeAmbariPassword(cluster.getUserName(), cluster.getPassword(), newPassword, stack, ambariClient, clientConfig))
                .thenThrow(new CloudbreakException("test1"));

        thrown.expect(CloudbreakException.class);
        thrown.expectMessage("test1");

        underTest.updateUserNamePassword(newPassword);

        verify(ambariUserHandler, times(1))
                .changeAmbariPassword(cluster.getUserName(), cluster.getPassword(), newPassword, stack, ambariClient, clientConfig);
    }

    @Test
    public void testChangeOriginalAmbariCredentialsAndCreateCloudbreakUserWhenEverythingworksFineThenChangeDefaultUserNameAndPassword()
            throws CloudbreakException, IOException, URISyntaxException {
        Cluster cluster = TestUtil.cluster();
        cluster.setUserName("admin1");
        stack.setCluster(cluster);

        when(clientFactory.getDefaultAmbariClient(stack, clientConfig)).thenReturn(ambariClient);
        when(ambariSecurityConfigProvider.getCloudbreakClusterUserName(cluster)).thenReturn("cloudbreak");
        when(ambariSecurityConfigProvider.getCloudbreakClusterPassword(cluster)).thenReturn("cloudbreak123");
        when(ambariUserHandler.createAmbariUser("cloudbreak", "cloudbreak123", stack, ambariClient, clientConfig))
                .thenReturn(ambariClient);
        when(ambariUserHandler.createAmbariUser(cluster.getUserName(), cluster.getPassword(), stack, ambariClient, clientConfig)).thenReturn(ambariClient);
        when(ambariClient.deleteUser("admin")).thenReturn(ambariClient);

        underTest.changeOriginalCredentialsAndCreateCloudbreakUser();

        verify(ambariSecurityConfigProvider, times(1)).getCloudbreakClusterUserName(stack.getCluster());
        verify(ambariSecurityConfigProvider, times(1)).getCloudbreakClusterPassword(stack.getCluster());
        verify(ambariUserHandler, times(1))
                .createAmbariUser("cloudbreak", "cloudbreak123", stack, ambariClient, clientConfig);
        verify(ambariUserHandler, times(1))
                .createAmbariUser(cluster.getUserName(), cluster.getPassword(), stack, ambariClient, clientConfig);
        verify(ambariClient, times(1)).deleteUser("admin");
        verify(ambariUserHandler, times(0))
                .changeAmbariPassword(anyString(), anyString(), anyString(), nullable(Stack.class), nullable(AmbariClient.class), any(HttpClientConfig.class));

    }

    @Test
    public void testChangeOriginalAmbariCredentialsAndCreateCloudbreakUserWhenAdminIsTheDefinedUserThenDefaultUserDoesNotChange() throws CloudbreakException,
            IOException, URISyntaxException {
        Cluster cluster = TestUtil.cluster();
        cluster.setUserName("admin");
        cluster.setPassword("admin");
        stack.setCluster(cluster);

        when(clientFactory.getDefaultAmbariClient(stack, clientConfig)).thenReturn(ambariClient);
        when(ambariSecurityConfigProvider.getCloudbreakClusterUserName(cluster)).thenReturn("cloudbreak");
        when(ambariSecurityConfigProvider.getCloudbreakClusterPassword(cluster)).thenReturn("cloudbreak123");
        when(ambariUserHandler
                .createAmbariUser("cloudbreak", "cloudbreak123", stack, ambariClient, clientConfig)).thenReturn(ambariClient);

        underTest.changeOriginalCredentialsAndCreateCloudbreakUser();

        verify(ambariSecurityConfigProvider, times(1)).getCloudbreakClusterUserName(stack.getCluster());
        verify(ambariSecurityConfigProvider, times(1)).getCloudbreakClusterPassword(stack.getCluster());
        verify(ambariUserHandler, times(1))
                .createAmbariUser("cloudbreak", "cloudbreak123", stack, ambariClient, clientConfig);
        verify(ambariClient, times(0)).deleteUser("admin");
        verify(ambariUserHandler, times(0))
                .changeAmbariPassword(anyString(), anyString(), anyString(), any(Stack.class), any(AmbariClient.class), any(HttpClientConfig.class));

    }

    @Test
    public void testChangeOriginalAmbariCredentialsAndCreateCloudbreakUserWhenAdminIsTheDefinedUserAndPasswordIsNotAdminThenTryToChangeDefaultUser()
            throws CloudbreakException, IOException, URISyntaxException {
        Cluster cluster = TestUtil.cluster();
        cluster.setUserName("admin");
        cluster.setPassword("admin1");
        stack.setCluster(cluster);

        when(clientFactory.getDefaultAmbariClient(stack, clientConfig)).thenReturn(ambariClient);
        when(ambariSecurityConfigProvider.getCloudbreakClusterUserName(cluster)).thenReturn("cloudbreak");
        when(ambariSecurityConfigProvider.getCloudbreakClusterPassword(cluster)).thenReturn("cloudbreak123");
        when(ambariUserHandler.createAmbariUser("cloudbreak", "cloudbreak123", stack, ambariClient, clientConfig)).thenReturn(ambariClient);

        underTest.changeOriginalCredentialsAndCreateCloudbreakUser();

        verify(ambariSecurityConfigProvider, times(1)).getCloudbreakClusterUserName(stack.getCluster());
        verify(ambariSecurityConfigProvider, times(1)).getCloudbreakClusterPassword(stack.getCluster());
        verify(ambariUserHandler, times(1)).createAmbariUser("cloudbreak", "cloudbreak123", stack, ambariClient, clientConfig);
        verify(ambariClient, times(0)).deleteUser("admin");
        verify(ambariUserHandler, times(1))
                .changeAmbariPassword("admin", "admin", cluster.getPassword(), stack, ambariClient, clientConfig);
    }

    @Test
    public void testPrepareSecurityWhenEverythingWorks() throws CloudbreakException, IOException, URISyntaxException {
        Cluster cluster = TestUtil.cluster();
        stack.setCluster(cluster);

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
        doNothing().when(clusterConnectorPollingResultChecker).checkPollingResult(pair.getLeft(), failed);

        underTest.prepareSecurity();

        verify(ambariOperationService, times(1)).waitForOperations(stack, ambariClient, operationRequests, PREPARE_DEKERBERIZING);
        verify(cloudbreakMessagesService, times(1)).getMessage(AMBARI_CLUSTER_PREPARE_DEKERBERIZING_FAILED.code());
        verify(clusterConnectorPollingResultChecker, times(1)).checkPollingResult(pair.getLeft(), failed);
    }

    @Test
    public void testPrepareSecurityWhenCancellationExceptionOccursThenShouldThrowCancellationException()
            throws CloudbreakException, IOException, URISyntaxException {
        Cluster cluster = TestUtil.cluster();
        stack.setCluster(cluster);

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

        underTest.prepareSecurity();

        verify(ambariOperationService, times(1)).waitForOperations(stack, ambariClient, operationRequests, PREPARE_DEKERBERIZING);
        verify(cloudbreakMessagesService, times(1)).getMessage(AMBARI_CLUSTER_PREPARE_DEKERBERIZING_FAILED.code());
        verify(clusterConnectorPollingResultChecker, times(1)).checkPollingResult(pair.getLeft(), failed);
    }

    @Test
    public void testPrepareSecurityWhenExceptionOccursWhichNotCancellationThenShouldThrowAmbariOperationFailedException()
            throws CloudbreakException, IOException, URISyntaxException {
        Cluster cluster = TestUtil.cluster();
        stack.setCluster(cluster);

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

        underTest.prepareSecurity();

        verify(ambariOperationService, times(1)).waitForOperations(stack, ambariClient, operationRequests, PREPARE_DEKERBERIZING);
        verify(cloudbreakMessagesService, times(1)).getMessage(AMBARI_CLUSTER_PREPARE_DEKERBERIZING_FAILED.code());
        verify(clusterConnectorPollingResultChecker, times(1)).checkPollingResult(pair.getLeft(), failed);
    }

    @Test
    public void testDisableSecurityWhenEverythingWorks() throws CloudbreakException, IOException, URISyntaxException {
        Cluster cluster = TestUtil.cluster();
        stack.setCluster(cluster);

        when(ambariClient.disableKerberos()).thenReturn(1);
        Map<String, Integer> operationRequests = singletonMap("DISABLE_KERBEROS_REQUEST", 1);

        ImmutablePair<PollingResult, Exception> pair = new ImmutablePair<>(PollingResult.SUCCESS, null);
        String failed = "failed";

        when(ambariOperationService.waitForOperations(stack, ambariClient, operationRequests, DISABLE_KERBEROS_STATE)).thenReturn(pair);
        when(cloudbreakMessagesService.getMessage(AMBARI_CLUSTER_DISABLE_KERBEROS_FAILED.code())).thenReturn("failed");
        doNothing().when(clusterConnectorPollingResultChecker).checkPollingResult(pair.getLeft(), failed);

        underTest.disableSecurity();

        verify(ambariOperationService, times(1)).waitForOperations(stack, ambariClient, operationRequests, DISABLE_KERBEROS_STATE);
        verify(cloudbreakMessagesService, times(1)).getMessage(AMBARI_CLUSTER_DISABLE_KERBEROS_FAILED.code());
        verify(clusterConnectorPollingResultChecker, times(1)).checkPollingResult(pair.getLeft(), failed);
    }

    @Test
    public void testDisableSecurityWhenCancellationExceptionOccursThenShouldThrowCancellationException()
            throws CloudbreakException, IOException, URISyntaxException {
        Cluster cluster = TestUtil.cluster();
        stack.setCluster(cluster);

        when(ambariClient.disableKerberos()).thenReturn(1);
        Map<String, Integer> operationRequests = singletonMap("DISABLE_KERBEROS_REQUEST", 1);
        ImmutablePair<PollingResult, Exception> pair = new ImmutablePair<>(PollingResult.EXIT, null);
        String failed = "failed";

        when(ambariOperationService.waitForOperations(stack, ambariClient, operationRequests, DISABLE_KERBEROS_STATE))
                .thenThrow(new CancellationException("cancel"));
        thrown.expect(CancellationException.class);
        thrown.expectMessage("cancel");

        underTest.disableSecurity();

        verify(ambariOperationService, times(1)).waitForOperations(stack, ambariClient, operationRequests, DISABLE_KERBEROS_STATE);
        verify(cloudbreakMessagesService, times(1)).getMessage(AMBARI_CLUSTER_DISABLE_KERBEROS_FAILED.code());
        verify(clusterConnectorPollingResultChecker, times(1)).checkPollingResult(pair.getLeft(), failed);
    }

    @Test
    public void testDisableSecurityWhenExceptionOccursWhichNotCancellationThenShouldThrowAmbariOperationFailedException()
            throws CloudbreakException, IOException, URISyntaxException {
        Cluster cluster = TestUtil.cluster();
        stack.setCluster(cluster);

        when(ambariClient.disableKerberos()).thenReturn(1);
        Map<String, Integer> operationRequests = singletonMap("DISABLE_KERBEROS_REQUEST", 1);
        ImmutablePair<PollingResult, Exception> pair = new ImmutablePair<>(PollingResult.EXIT, null);
        String failed = "failed";

        when(ambariOperationService.waitForOperations(stack, ambariClient, operationRequests, DISABLE_KERBEROS_STATE))
                .thenThrow(new AmbariConnectionException("failed"));

        thrown.expect(AmbariOperationFailedException.class);
        thrown.expectMessage("failed");

        underTest.disableSecurity();

        verify(ambariOperationService, times(1)).waitForOperations(stack, ambariClient, operationRequests, DISABLE_KERBEROS_STATE);
        verify(cloudbreakMessagesService, times(1)).getMessage(AMBARI_CLUSTER_DISABLE_KERBEROS_FAILED.code());
        verify(clusterConnectorPollingResultChecker, times(1)).checkPollingResult(pair.getLeft(), failed);
    }

}
