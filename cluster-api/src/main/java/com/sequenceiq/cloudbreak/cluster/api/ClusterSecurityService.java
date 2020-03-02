package com.sequenceiq.cloudbreak.cluster.api;

import com.sequenceiq.cloudbreak.dto.LdapView;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.auth.altus.VirtualGroupRequest;

public interface ClusterSecurityService {

    void replaceUserNamePassword(String newUserName, String newPassword) throws CloudbreakException;

    void updateUserNamePassword(String newPassword) throws CloudbreakException;

    void prepareSecurity();

    void disableSecurity();

    void deregisterServices(String clusterName);

    void changeOriginalCredentialsAndCreateCloudbreakUser(boolean ldapConfigured) throws CloudbreakException;

    void setupLdapAndSSO(String primaryGatewayPublicAddress, LdapView ldapConfig, VirtualGroupRequest virtualGroupRequest) throws CloudbreakException;

    void setupMonitoringUser() throws CloudbreakException;

    String getCloudbreakClusterUserName();

    String getCloudbreakClusterPassword();

    String getDataplaneClusterUserName();

    String getDataplaneClusterPassword();

    String getClusterUserProvidedPassword();

    String getCertPath();

    String getKeystorePath();

    String getKeystorePassword();

    String getMasterKey();
}
