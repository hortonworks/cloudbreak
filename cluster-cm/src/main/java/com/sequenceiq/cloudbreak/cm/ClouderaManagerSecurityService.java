package com.sequenceiq.cloudbreak.cm;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.security.KeyPair;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.BatchResourceApi;
import com.cloudera.api.swagger.CertManagerResourceApi;
import com.cloudera.api.swagger.HostsResourceApi;
import com.cloudera.api.swagger.ToolsResourceApi;
import com.cloudera.api.swagger.UsersResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiAuthRoleRef;
import com.cloudera.api.swagger.model.ApiBatchRequest;
import com.cloudera.api.swagger.model.ApiBatchRequestElement;
import com.cloudera.api.swagger.model.ApiBatchResponse;
import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiGenerateHostCertsArguments;
import com.cloudera.api.swagger.model.ApiHostList;
import com.cloudera.api.swagger.model.ApiUser2;
import com.cloudera.api.swagger.model.ApiUser2List;
import com.cloudera.api.swagger.model.HTTPMethod;
import com.sequenceiq.cloudbreak.auth.altus.VirtualGroupRequest;
import com.sequenceiq.cloudbreak.auth.altus.exception.UnauthorizedException;
import com.sequenceiq.cloudbreak.certificate.PkiUtil;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.cluster.api.ClusterSecurityService;
import com.sequenceiq.cloudbreak.cluster.service.ClusterClientInitException;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiClientProvider;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerClientInitException;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.cm.deregister.ClouderaManagerDeregisterService;
import com.sequenceiq.cloudbreak.cm.exception.ClouderaManagerOperationFailedException;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollingServiceProvider;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.dto.LdapView;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.dto.datalake.DatalakeDto;
import com.sequenceiq.cloudbreak.polling.ExtendedPollingResult;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.util.URLUtils;
import com.sequenceiq.cloudbreak.view.ClusterView;

@Service
@Scope("prototype")
public class ClouderaManagerSecurityService implements ClusterSecurityService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerSecurityService.class);

    private static final String ADMIN_USER = "admin";

    private static final int TEST_MAX_ATTEMPTS = 5;

    private static final int TEST_BACKOFF = 5000;

    @Inject
    private ClouderaManagerSecurityConfigProvider securityConfigProvider;

    @Inject
    private ClouderaManagerKerberosService kerberosService;

    @Inject
    private ClouderaManagerLdapService ldapService;

    @Inject
    private ClouderaManagerApiClientProvider clouderaManagerApiClientProvider;

    @Inject
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    @Inject
    private ClouderaManagerDeregisterService clouderaManagerDeregisterService;

    @Inject
    private ClouderaManagerPollingServiceProvider clouderaManagerPollingServiceProvider;

    private final StackDtoDelegate stack;

    private final HttpClientConfig clientConfig;

    public ClouderaManagerSecurityService(StackDtoDelegate stack, HttpClientConfig clientConfig) {
        this.stack = stack;
        this.clientConfig = clientConfig;
    }

    @Override
    @Retryable(value = UnauthorizedException.class, maxAttempts = TEST_MAX_ATTEMPTS, backoff = @Backoff(delay = TEST_BACKOFF))
    public void testUser(String user, String password) throws CloudbreakException {
        try {
            ApiClient client = getClient(stack.getGatewayPort(), user, password, clientConfig);
            clouderaManagerApiFactory.getUserResourceApi(client).readUsers2("SUMMARY");
        } catch (ApiException ae) {
            if (HttpStatus.UNAUTHORIZED.value() == ae.getCode()) {
                throw new UnauthorizedException("Test user is not authorized for CM call");
            }
            throw new CloudbreakException(String.format("Error occurred during test of user %s.", user), ae);
        } catch (Exception e) {
            throw new CloudbreakException(String.format("Error occurred during test of user %s.", user), e);
        }
    }

    @Override
    public void createNewUser(String oldUserForAuthRoles, String newUserName, String newPassword, String clientUserName, String clientPassword)
            throws CloudbreakException {
        try {
            ApiClient client = getClient(stack.getGatewayPort(), clientUserName, clientPassword, clientConfig);
            UsersResourceApi usersResourceApi = clouderaManagerApiFactory.getUserResourceApi(client);
            ApiUser2List userList = usersResourceApi.readUsers2("SUMMARY");
            if (getUser(userList, newUserName).isPresent()) {
                LOGGER.info("CM user {} already exists.", newUserName);
                return;
            }
            Optional<ApiUser2> oldUser = getUser(userList, oldUserForAuthRoles);
            if (oldUser.isEmpty()) {
                throw new CloudbreakException(String.format("User %s does not exists in CM, thus we cannot check auth roles for new user.", newUserName));
            }
            createNewUser(usersResourceApi, oldUser.get().getAuthRoles(), newUserName, newPassword);
        } catch (Exception e) {
            throw new CloudbreakException(String.format("Error occurred during creation of user %s in CM", newUserName), e);
        }
    }

    @Override
    @Retryable(value = UnauthorizedException.class, maxAttempts = TEST_MAX_ATTEMPTS, backoff = @Backoff(delay = TEST_BACKOFF))
    public void deleteUser(String userName, String clientUser, String clientPassword) throws CloudbreakException {
        try {
            ApiClient client = getClient(stack.getGatewayPort(), clientUser, clientPassword, clientConfig);
            UsersResourceApi usersResourceApi = clouderaManagerApiFactory.getUserResourceApi(client);
            ApiUser2List userList = usersResourceApi.readUsers2("SUMMARY");
            if (getUser(userList, userName).isPresent()) {
                usersResourceApi.deleteUser2(userName);
            }
        } catch (Exception e) {
            throw new CloudbreakException(String.format("Error occurred during deletion of user %s in CM", userName), e);
        }
    }

    @Override
    @Retryable(value = UnauthorizedException.class, maxAttempts = TEST_MAX_ATTEMPTS, backoff = @Backoff(delay = TEST_BACKOFF))
    public void checkUser(String userName, String clientUser, String clientPassword) throws Exception {
        ApiClient client = getClient(stack.getGatewayPort(), clientUser, clientPassword, clientConfig);
        UsersResourceApi usersResourceApi = clouderaManagerApiFactory.getUserResourceApi(client);
        ApiUser2List userList = usersResourceApi.readUsers2("SUMMARY");
        getUser(userList, userName).orElseThrow(() -> new CloudbreakException(String.format("User %s does not exists in CM!", userName)));
    }

    @Override
    public void replaceUserNamePassword(String newUserName, String newPassword) throws CloudbreakException {
        ClusterView cluster = stack.getCluster();
        String user = cluster.getCloudbreakClusterManagerUser();
        String password = cluster.getCloudbreakClusterManagerPassword();
        try {
            ApiClient client = getClient(stack.getGatewayPort(), user, password, clientConfig);
            UsersResourceApi usersResourceApi = clouderaManagerApiFactory.getUserResourceApi(client);
            ApiUser2List oldUserList = usersResourceApi.readUsers2("SUMMARY");
            Optional<ApiUser2> oldAdminUser = oldUserList.getItems().stream()
                    .filter(apiUser2 -> apiUser2.getName().equals(stack.getCluster().getUserName()))
                    .findFirst();
            if (oldAdminUser.isPresent()) {
                createNewUser(usersResourceApi, oldAdminUser.get().getAuthRoles(), newUserName, newPassword, oldUserList);
                usersResourceApi.deleteUser2(oldAdminUser.get().getName());
            } else {
                throw new CloudbreakException("Can't find original admin user");
            }
        } catch (ApiException | ClouderaManagerClientInitException e) {
            LOGGER.info("Can't replace original admin user due to: ", e);
            throw new CloudbreakException("Can't replace original admin user due to: " + e.getMessage());
        }
    }

    @Override
    public void updateUserNamePassword(String newPassword) throws CloudbreakException {
        ClusterView cluster = stack.getCluster();
        String cmUser = cluster.getCloudbreakClusterManagerUser();
        String password = cluster.getCloudbreakClusterManagerPassword();
        try {
            ApiClient client = getClient(stack.getGatewayPort(), cmUser, password, clientConfig);
            UsersResourceApi usersResourceApi = clouderaManagerApiFactory.getUserResourceApi(client);
            ApiUser2List oldUserList = usersResourceApi.readUsers2("SUMMARY");
            Optional<ApiUser2> oldAdminUser = oldUserList.getItems().stream()
                    .filter(apiUser2 -> apiUser2.getName().equals(stack.getCluster().getUserName()))
                    .findFirst();
            if (oldAdminUser.isPresent()) {
                ApiUser2 user = oldAdminUser.get();
                user.setPassword(newPassword);
                usersResourceApi.updateUser2(user.getName(), user);
            } else {
                throw new CloudbreakException("Can't find admin user");
            }
        } catch (ApiException | ClouderaManagerClientInitException e) {
            LOGGER.info("Can't replace admin password due to: ", e);
            throw new CloudbreakException("Can't replace admin password due to: " + e.getMessage());
        }
    }

    @Override
    public void disableSecurity() {
        try {
            kerberosService.deleteCredentials(clientConfig, stack);
        } catch (Exception e) {
            LOGGER.warn("Couldn't cleanup kerberos. It is possible that CM is not started.", e);
        }
    }

    @Override
    public void deregisterServices(String clusterName, Optional<DatalakeDto> datalakeDto) {
        try {
            clouderaManagerDeregisterService.deregister(clientConfig, stack, datalakeDto);
        } catch (Exception e) {
            LOGGER.warn("Couldn't remove services. It's possible that CM is not started.", e);
        }
    }

    @Override
    public void changeOriginalCredentialsAndCreateCloudbreakUser(boolean ldapConfigured) throws CloudbreakException {
        LOGGER.debug("change original admin user and create cloudbreak user");
        try {
            ApiClient client = createApiClient();
            UsersResourceApi usersResourceApi = clouderaManagerApiFactory.getUserResourceApi(client);
            ApiUser2List userList = usersResourceApi.readUsers2("SUMMARY");

            ApiUser2 oldAdminUser = getOldAdminUser(userList).orElseThrow(() -> new CloudbreakException("Can't find original admin user"));
            ClusterView cluster = stack.getCluster();
            createNewUser(usersResourceApi, oldAdminUser.getAuthRoles(), cluster.getCloudbreakClusterManagerUser(),
                    cluster.getCloudbreakClusterManagerPassword(), userList);
            createNewUser(usersResourceApi, oldAdminUser.getAuthRoles(), cluster.getDpClusterManagerUser(), cluster.getDpClusterManagerPassword(), userList);
            if (ADMIN_USER.equals(cluster.getUserName())) {
                oldAdminUser.setPassword(cluster.getPassword());
                usersResourceApi.updateUser2(oldAdminUser.getName(), oldAdminUser);
            } else if (cluster.getUserName() != null) {
                createUserSuppliedCMUser(userList, oldAdminUser, cluster);
            }
            removeDefaultAdminUser(ldapConfigured, Optional.ofNullable(cluster.getUserName()));
        } catch (ApiException | ClusterClientInitException | ClouderaManagerClientInitException e) {
            LOGGER.info("Can't replace original admin user due to: ", e);
            throw new CloudbreakException("Can't replace original admin user due to: " + e.getMessage());
        }
    }

    private void createUserSuppliedCMUser(ApiUser2List userList, ApiUser2 oldAdminUser, ClusterView cluster)
            throws ClouderaManagerClientInitException, ApiException {
        ApiClient client;
        String user = cluster.getCloudbreakClusterManagerUser();
        String password = cluster.getCloudbreakClusterManagerPassword();
        client = getClient(stack.getGatewayPort(), user, password, clientConfig);
        UsersResourceApi newUsersResourceApi = clouderaManagerApiFactory.getUserResourceApi(client);
        createNewUser(newUsersResourceApi, oldAdminUser.getAuthRoles(), cluster.getUserName(), cluster.getPassword(), userList);
    }

    private Optional<ApiUser2> getUser(ApiUser2List userList, String username) {
        return userList.getItems().stream().filter(user -> StringUtils.equals(user.getName(), username)).findFirst();
    }

    private ApiClient createApiClient() throws ClusterClientInitException, ClouderaManagerClientInitException, CloudbreakException {
        try {
            ApiClient client =
                    clouderaManagerApiClientProvider.getDefaultClient(stack.getGatewayPort(), clientConfig, ClouderaManagerApiClientProvider.API_V_31);
            ToolsResourceApi toolsResourceApi = clouderaManagerApiFactory.getToolsResourceApi(client);
            toolsResourceApi.echo("TEST");
            LOGGER.debug("Cloudera Manager already running, old admin user's password has not been changed yet.");
            return client;
        } catch (ClouderaManagerClientInitException e) {
            throw new ClusterClientInitException(e);
        } catch (ApiException e) {
            if (org.springframework.http.HttpStatus.UNAUTHORIZED.value() == e.getCode()) {
                return createClientWithNewPassword();
            }
            LOGGER.debug("Cloudera Manager is not running.", e);
            throw new CloudbreakException("Cloudera Manager is not running. " + e.getMessage());
        }
    }

    private ApiClient createClientWithNewPassword() throws ClouderaManagerClientInitException {
        LOGGER.debug("Cloudera Manager already running, old admin user's password has been changed.");
        ClusterView cluster = stack.getCluster();
        String user = cluster.getCloudbreakClusterManagerUser();
        String password = cluster.getCloudbreakClusterManagerPassword();
        return clouderaManagerApiClientProvider.getV40Client(stack.getGatewayPort(), user, password, clientConfig);
    }

    private void removeDefaultAdminUser(boolean ldapConfigured, Optional<String> userName) {
        if (ldapConfigured && isUserIsNullOrNotAdmin(userName)) {
            try {
                String user = stack.getCluster().getCloudbreakClusterManagerUser();
                String password = stack.getCluster().getCloudbreakClusterManagerPassword();
                ApiClient client = getClient(stack.getGatewayPort(), user, password, clientConfig);
                UsersResourceApi usersResourceApi = clouderaManagerApiFactory.getUserResourceApi(client);
                usersResourceApi.deleteUser2(ADMIN_USER);
            } catch (ApiException | ClouderaManagerClientInitException e) {
                LOGGER.info("Can't remove default admin user due to: ", e);
            }
        }
    }

    private boolean isUserIsNullOrNotAdmin(Optional<String> userName) {
        return userName.filter(ADMIN_USER::equals).map(x -> Boolean.FALSE).orElse(Boolean.TRUE);
    }

    @Retryable
    private Optional<ApiUser2> getOldAdminUser(ApiUser2List userList) {
        return userList.getItems().stream()
                .filter(apiUser2 -> ADMIN_USER.equals(apiUser2.getName()))
                .findFirst();
    }

    @Override
    public void setupLdapAndSSO(String primaryGatewayPublicAddress, LdapView ldapConfig, VirtualGroupRequest virtualGroupRequest) throws CloudbreakException {
        try {
            ldapService.setupLdap(stack.getStack(), stack.getCluster(), clientConfig, ldapConfig, virtualGroupRequest);
        } catch (ApiException | ClouderaManagerClientInitException e) {
            throw new CloudbreakException(e);
        }
    }

    @Override
    public Optional<String> getTrustStoreForValidation() throws CloudbreakException {
        try {
            ClusterView cluster = stack.getCluster();
            String user = cluster.getCloudbreakClusterManagerUser();
            String password = cluster.getCloudbreakClusterManagerPassword();
            ApiClient client = clouderaManagerApiClientProvider.getClouderaManagerClient(clientConfig,
                    stack.getGatewayPort(), user, password, ClouderaManagerApiClientProvider.API_V_45);
            CertManagerResourceApi certManagerResourceApi = clouderaManagerApiFactory.getCertManagerResourceApi(client);
            return Optional.of(FileUtils.readFileToString(certManagerResourceApi.getTruststore("PEM"), Charset.defaultCharset()));
        } catch (ApiException ae) {
            HttpStatus status = HttpStatus.resolve(ae.getCode());
            if (status != null && status.is4xxClientError()) {
                LOGGER.error("Couldn't get trust store from CM, though trust store validation is not a hard requirement, skipping.", ae);
                return Optional.empty();
            } else {
                throw new CloudbreakException(ae);
            }
        } catch (Exception e) {
            throw new CloudbreakException(e);
        }
    }

    @Override
    public String getCloudbreakClusterUserName() {
        return securityConfigProvider.getCloudbreakClusterUserName(stack.getCluster());
    }

    @Override
    public String getCloudbreakClusterPassword() {
        return securityConfigProvider.getCloudbreakClusterPassword(stack.getCluster());
    }

    @Override
    public String getDataplaneClusterUserName() {
        return securityConfigProvider.getDataplaneClusterUserName(stack.getCluster());
    }

    @Override
    public String getDataplaneClusterPassword() {
        return securityConfigProvider.getDataplaneClusterPassword(stack.getCluster());
    }

    @Override
    public String getClusterUserProvidedPassword() {
        return securityConfigProvider.getClusterUserProvidedPassword(stack.getCluster());
    }

    @Override
    public String getCertPath() {
        return securityConfigProvider.getCertPath();
    }

    @Override
    public String getKeystorePath() {
        return securityConfigProvider.getKeystorePath();
    }

    @Override
    public String getKeystorePassword() {
        return securityConfigProvider.getKeystorePassword();
    }

    @Override
    public String getMasterKey() {
        return securityConfigProvider.getMasterKey(stack.getCluster());
    }

    @Override
    public void rotateHostCertificates(String sshUser, KeyPair sshKeyPair, String subAltName) throws CloudbreakException {
        ClusterView cluster = stack.getCluster();
        String user = cluster.getCloudbreakClusterManagerUser();
        String password = cluster.getCloudbreakClusterManagerPassword();
        try {
            ApiClient client = getClient(stack.getGatewayPort(), user, password, clientConfig);
            HostsResourceApi hostsResourceApi = clouderaManagerApiFactory.getHostsResourceApi(client);
            BatchResourceApi batchResourceApi = clouderaManagerApiFactory.getBatchResourceApi(client);
            ApiHostList hostList = hostsResourceApi.readHosts(null, null, "SUMMARY");
            ApiBatchRequest batchRequest = createHostCertsBatchRequest(hostList, sshUser, sshKeyPair, subAltName);
            ApiBatchResponse apiBatchResponse = batchResourceApi.execute(batchRequest);
            processHostCertsBatchResponse(client, apiBatchResponse);
        } catch (ApiException | ClouderaManagerClientInitException e) {
            LOGGER.warn("Can't rotate the host certificates", e);
            throw new CloudbreakException("Can't rotate the host certificates due to: " + e.getMessage());
        }
    }

    private void createNewUser(UsersResourceApi usersResourceApi, List<ApiAuthRoleRef> authRoles, String userName, String password, ApiUser2List userList)
            throws ApiException {
        if (getUser(userList, userName).isPresent()) {
            return;
        }
        createNewUser(usersResourceApi, authRoles, userName, password);
    }

    private void createNewUser(UsersResourceApi usersResourceApi, List<ApiAuthRoleRef> authRoles, String userName, String password) throws ApiException {
        ApiUser2List apiUser2List = new ApiUser2List();
        ApiUser2 newUser = new ApiUser2();
        newUser.setName(userName);
        newUser.setPassword(password);
        newUser.setAuthRoles(authRoles);
        apiUser2List.addItemsItem(newUser);
        usersResourceApi.createUsers2(apiUser2List);
    }

    private ApiClient getDefaultClient(Integer gatewayPort, HttpClientConfig clientConfig) throws ClouderaManagerClientInitException {
        return clouderaManagerApiClientProvider.getDefaultClient(gatewayPort, clientConfig, ClouderaManagerApiClientProvider.API_V_31);
    }

    public ApiClient getClient(Integer gatewayPort, String user, String password, HttpClientConfig clientConfig) throws ClouderaManagerClientInitException {
        if (StringUtils.isNoneBlank(user, password)) {
            return clouderaManagerApiClientProvider.getClouderaManagerClient(clientConfig,
                    gatewayPort, user, password, ClouderaManagerApiClientProvider.API_V_31);
        } else {
            return getDefaultClient(gatewayPort, clientConfig);
        }
    }

    private ApiBatchRequest createHostCertsBatchRequest(ApiHostList hostList, String sshUser, KeyPair sshKeyPair, String subAltName) {
        ApiGenerateHostCertsArguments apiGenerateHostCertsArguments = createApiGenerateHostCertsArguments(sshUser, sshKeyPair, subAltName);
        List<ApiBatchRequestElement> batchRequestElements = hostList.getItems().stream()
                .filter(host -> host.getClusterRef() != null)
                .map(host -> new ApiBatchRequestElement()
                        .method(HTTPMethod.POST)
                        .url(ClouderaManagerApiClientProvider.API_V_31 + "/hosts/" + URLUtils.encodeString(host.getHostId()) + "/commands/generateHostCerts")
                        .body(apiGenerateHostCertsArguments)
                        .acceptType("application/json")
                        .contentType("application/json"))
                .collect(Collectors.toList());
        return new ApiBatchRequest().items(batchRequestElements);
    }

    private ApiGenerateHostCertsArguments createApiGenerateHostCertsArguments(String sshUser, KeyPair sshKeyPair, String subAltName) {
        ApiGenerateHostCertsArguments apiGenerateHostCertsArguments = new ApiGenerateHostCertsArguments();
        if (StringUtils.isNotEmpty(sshUser)) {
            apiGenerateHostCertsArguments.userName(sshUser).privateKey(createPrivateKeyString(sshKeyPair));
        }
        if (StringUtils.isNotEmpty(subAltName)) {
            apiGenerateHostCertsArguments.addSubjectAltNameItem(subAltName);
        }
        return apiGenerateHostCertsArguments;
    }

    private void processHostCertsBatchResponse(ApiClient client, ApiBatchResponse apiBatchResponse) {
        if (apiBatchResponse != null && apiBatchResponse.isSuccess() != null && apiBatchResponse.getItems() != null && apiBatchResponse.isSuccess()) {
            List<BigDecimal> ids = apiBatchResponse.getItems().stream()
                    .map(bre -> new Json((String) bre.getResponse()).getSilent(ApiCommand.class).getId())
                    .collect(Collectors.toList());
            ExtendedPollingResult pollingResult = clouderaManagerPollingServiceProvider.startPollingCommandList(stack, client, ids, "Rotate host certificates");
            if (pollingResult.isExited()) {
                throw new CancellationException("Cluster was terminated during rotation of host certificates");
            } else if (pollingResult.isTimeout()) {
                throw new ClouderaManagerOperationFailedException("Timeout while Cloudera Manager rotates the host certificates.");
            }
        } else {
            throw new ClouderaManagerOperationFailedException("Host certificates rotation batch operation failed: " + apiBatchResponse);
        }
    }

    private String createPrivateKeyString(KeyPair sshKeyPair) {
        return StringUtils.removeEnd(PkiUtil.convert(sshKeyPair.getPrivate()), "\n");
    }

}
