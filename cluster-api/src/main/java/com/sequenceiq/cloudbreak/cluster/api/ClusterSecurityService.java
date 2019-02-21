package com.sequenceiq.cloudbreak.cluster.api;

import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

public interface ClusterSecurityService {

    void replaceUserNamePassword(String newUserName, String newPassword) throws CloudbreakException;

    void updateUserNamePassword(String newPassword) throws CloudbreakException;

    void prepareSecurity();

    void disableSecurity();

    void changeOriginalCredentialsAndCreateCloudbreakUser() throws CloudbreakException;

    void setupLdapAndSSO(AmbariRepo ambariRepo, String primaryGatewayPublicAddress);

    boolean isLdapAndSSOReady(AmbariRepo ambariRepo);

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
