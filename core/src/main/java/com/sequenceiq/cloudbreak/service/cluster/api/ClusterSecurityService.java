package com.sequenceiq.cloudbreak.service.cluster.api;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

public interface ClusterSecurityService {

    void replaceUserNamePassword(Stack stack, String newUserName, String newPassword) throws CloudbreakException;

    void updateUserNamePassword(Stack stack, String newPassword) throws CloudbreakException;

    void prepareSecurity(Stack stack);

    void disableSecurity(Stack stack);

    void changeOriginalCredentialsAndCreateCloudbreakUser(Stack stack) throws CloudbreakException;
}
