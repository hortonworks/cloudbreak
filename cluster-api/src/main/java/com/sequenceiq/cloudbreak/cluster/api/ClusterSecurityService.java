package com.sequenceiq.cloudbreak.cluster.api;

import java.security.KeyPair;
import java.util.Optional;

import com.sequenceiq.cloudbreak.auth.altus.VirtualGroupRequest;
import com.sequenceiq.cloudbreak.dto.LdapView;
import com.sequenceiq.cloudbreak.dto.datalake.DatalakeDto;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

public interface ClusterSecurityService {

    void testUser(String user, String password) throws CloudbreakException;

    void createNewUser(String oldUserNameForAuthRoles, String newUserName, String newPassword,
            String clientUserName, String clientPassword) throws CloudbreakException;

    void deleteUser(String userName, String clientUser, String clientPassword) throws CloudbreakException;

    void checkUser(String userName, String clientUser, String clientPassword) throws Exception;

    void replaceUserNamePassword(String newUserName, String newPassword) throws CloudbreakException;

    void updateUserNamePassword(String newPassword) throws CloudbreakException;

    void prepareSecurity();

    void disableSecurity();

    void deregisterServices(String clusterName, Optional<DatalakeDto> datalakeDto);

    void changeOriginalCredentialsAndCreateCloudbreakUser(boolean ldapConfigured) throws CloudbreakException;

    void setupLdapAndSSO(String primaryGatewayPublicAddress, LdapView ldapConfig, VirtualGroupRequest virtualGroupRequest) throws CloudbreakException;

    String getCloudbreakClusterUserName();

    String getCloudbreakClusterPassword();

    String getDataplaneClusterUserName();

    String getDataplaneClusterPassword();

    String getClusterUserProvidedPassword();

    String getCertPath();

    String getKeystorePath();

    String getKeystorePassword();

    String getMasterKey();

    void rotateHostCertificates(String sshUser, KeyPair sshKeyPair, String subAltName) throws CloudbreakException;

    String getTrustStore() throws CloudbreakException;
}
