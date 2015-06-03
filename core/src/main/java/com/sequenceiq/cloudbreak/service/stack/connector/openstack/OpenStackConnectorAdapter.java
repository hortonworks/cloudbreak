package com.sequenceiq.cloudbreak.service.stack.connector.openstack;

import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.stack.connector.CloudPlatformConnector;

@Service
public class OpenStackConnectorAdapter implements CloudPlatformConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenStackConnectorAdapter.class);


    @Value("${cb.openstack.experimental.connector:false}")
    private boolean experimentalConnector;

    @Inject
    private OpenStackConnector openStackConnector;

    @Inject
    private OpenStackConnectorV2Facade openStackConnectorV2Facade;

    @Override
    public Set<Resource> buildStack(Stack stack, String gateWayUserData, String coreUserData, Map<String, Object> setupProperties) {
        if (experimentalConnector) {
            return openStackConnectorV2Facade.buildStack(stack, gateWayUserData, coreUserData, setupProperties);
        } else {
            return openStackConnector.buildStack(stack, gateWayUserData, coreUserData, setupProperties);
        }
    }

    @Override
    public Set<Resource> addInstances(Stack stack, String gateWayUserData, String coreUserData, Integer adjustment, String instanceGroup) {
        if (experimentalConnector) {
            return openStackConnectorV2Facade.addInstances(stack, gateWayUserData, coreUserData, adjustment, instanceGroup);
        } else {
            return openStackConnector.addInstances(stack, gateWayUserData, coreUserData, adjustment, instanceGroup);
        }
    }

    @Override
    public Set<String> removeInstances(Stack stack, Set<String> instanceIds, String instanceGroup) {
        return openStackConnector.removeInstances(stack, instanceIds, instanceGroup);
    }

    @Override
    public void deleteStack(Stack stack, Credential credential) {
        if (experimentalConnector) {
            openStackConnectorV2Facade.deleteStack(stack, credential);
        } else {
            openStackConnector.deleteStack(stack, credential);
        }
    }

    @Override
    public void rollback(Stack stack, Set<Resource> resourceSet) {
        openStackConnector.rollback(stack, resourceSet);
    }

    @Override
    public boolean startAll(Stack stack) {
        return openStackConnector.startAll(stack);
    }

    @Override
    public boolean stopAll(Stack stack) {
        return openStackConnector.stopAll(stack);
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return openStackConnector.getCloudPlatform();
    }

    @Override
    public void updateAllowedSubnets(Stack stack, String gateWayUserData, String coreUserData) {
        openStackConnector.updateAllowedSubnets(stack, gateWayUserData, coreUserData);
    }
}
