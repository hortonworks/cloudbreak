package com.sequenceiq.cloudbreak.service.cluster.api;

import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

public interface ClusterSecurityService {

    void replaceUserNamePassword(Stack stackId, String newUserName, String newPassword) throws CloudbreakException;

    void updateUserNamePassword(Stack stack, String newPassword) throws CloudbreakException;

    void prepareSecurity(Stack stack);

    void disableSecurity(Stack stack);

    void changeOriginalAmbariCredentialsAndCreateCloudbreakUser(Stack stack) throws CloudbreakException;
}
