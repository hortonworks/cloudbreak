package com.sequenceiq.it.cloudbreak.newway.mock.util;

import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.it.cloudbreak.mock.MockInstanceUtil;

public class MockUtil {

    private MockUtil() {

    }

    public static Map<String, CloudVmMetaDataStatus> generateInstances(String mockServer, int sshPort, int instanceNumber) {
        Map<String, CloudVmMetaDataStatus> instanceMap = new HashMap<>();
        MockInstanceUtil mockInstanceUtil = new MockInstanceUtil(mockServer, sshPort);
        mockInstanceUtil.addInstance(instanceMap, instanceNumber);
        return instanceMap;
    }
}
