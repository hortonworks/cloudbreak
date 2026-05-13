package com.sequenceiq.cloudbreak.cloud.openstack.view;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.service.CloudbreakRuntimeException;

public class KeystoneCredentialView {

    public static final String CB_KEYSTONE_V3_PROJECT_SCOPE = "cb-keystone-v3-project-scope";

    public static final String CB_KEYSTONE_V3_DOMAIN_SCOPE = "cb-keystone-v3-domain-scope";

    private static final String CB_KEYPAIR_NAME = "cb";

    private final CloudCredential cloudCredential;

    private final String stackName;

    private final KeystoneConfig keystoneConfig;

    public KeystoneCredentialView(AuthenticatedContext authenticatedContext) {
        stackName = authenticatedContext.getCloudContext().getName() + '_' + authenticatedContext.getCloudContext().getId();
        cloudCredential = authenticatedContext.getCloudCredential();
        keystoneConfig = cloudCredential.getParameter("openstack", KeystoneConfig.class);
    }

    public KeystoneCredentialView(CloudCredential cloudCredential) {
        stackName = "";
        this.cloudCredential = cloudCredential;
        keystoneConfig = cloudCredential.getParameter("openstack", KeystoneConfig.class);
    }

    public String getKeyPairName() {
        return String.format("%s-%s", CB_KEYPAIR_NAME, stackName);
    }

    public String getName() {
        return cloudCredential.getName();
    }

    public String getFacing() {
        return keystoneConfig.facing;
    }

    public String getStackName() {
        return stackName;
    }

    public String getUserName() {
        return keystoneConfig.userName;
    }

    public String getPassword() {
        return keystoneConfig.password;
    }

    public String getEndpoint() {
        return keystoneConfig.endpoint;
    }

    public String getUserDomain() {
        if (keystoneConfig.keystoneV3.project != null) {
            return keystoneConfig.keystoneV3.project.userDomain;
        } else if (keystoneConfig.keystoneV3.domain != null) {
            return keystoneConfig.keystoneV3.domain.userDomain;
        } else {
            throw new CloudbreakRuntimeException("Keystone scope is not defined. Please check your cloud credential parameters.");
        }
    }

    public String getProjectName() {
        if (keystoneConfig.keystoneV3.project != null) {
            return keystoneConfig.keystoneV3.project.projectName;
        } else {
            throw new CloudbreakRuntimeException("projectName is not defined. Please check your cloud credential parameters.");
        }
    }

    public String getProjectDomain() {
        if (keystoneConfig.keystoneV3.project.projectDomainName != null) {
            return keystoneConfig.keystoneV3.project.projectDomainName;
        } else {
            throw new CloudbreakRuntimeException("projectDomainName is not defined. Please check your cloud credential parameters.");
        }
    }

    public String getDomainName() {
        if (keystoneConfig.keystoneV3.domain.domainName != null) {
            return keystoneConfig.keystoneV3.domain.domainName;
        } else {
            throw new CloudbreakRuntimeException("domainName is not defined. Please check your cloud credential parameters.");
        }
    }

    public String getScope() {
        if (keystoneConfig.keystoneV3.project != null) {
            return CB_KEYSTONE_V3_PROJECT_SCOPE;
        } else if (keystoneConfig.keystoneV3.domain != null) {
            return CB_KEYSTONE_V3_DOMAIN_SCOPE;
        } else {
            throw new CloudbreakRuntimeException("Keystone scope is not defined. Please check your cloud credential parameters.");
        }
    }

    public String getRemoteEnvironmentCrn() {
        return keystoneConfig.remoteEnvironmentCrn;
    }

    public String getVersion() {
        return cloudCredential.getParameter("keystoneVersion", String.class);
    }

    record KeystoneConfig(
            String endpoint,
            String facing,
            String userName,
            String password,
            KeystoneV3 keystoneV3,
            String remoteEnvironmentCrn
    ) {
        record KeystoneV3(
                Project project,
                Domain domain
        ) {
        }

        record Project(
                String projectName,
                String projectDomainName,
                String userDomain
        ) {
        }

        record Domain(
                String userDomain,
                String domainName
        ) {
        }
    }
}