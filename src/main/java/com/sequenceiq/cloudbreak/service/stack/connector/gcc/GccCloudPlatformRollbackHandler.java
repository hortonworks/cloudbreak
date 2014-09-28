package com.sequenceiq.cloudbreak.service.stack.connector.gcc;

import static com.sequenceiq.cloudbreak.domain.CloudPlatform.GCC;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.stack.connector.CloudPlatformRollbackHandler;

@Service
public class GccCloudPlatformRollbackHandler implements CloudPlatformRollbackHandler {

    @Autowired
    private GccConnector gccConnector;

    @Override
    public void rollback(Stack stack, Set<Resource> resourceSet) {
        gccConnector.rollback(stack.getUser(), stack, stack.getCredential(), resourceSet);
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return GCC;
    }
}
