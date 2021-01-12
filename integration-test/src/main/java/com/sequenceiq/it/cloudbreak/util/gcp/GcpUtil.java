package com.sequenceiq.it.cloudbreak.util.gcp;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.it.cloudbreak.util.gcp.action.GcpClientActions;

@Component
public class GcpUtil {
    @Inject
    private GcpClientActions gcpClientActions;

    private GcpUtil() {
    }

    public List<String> listInstanceDiskNames(List<String> instanceIds) {
        return gcpClientActions.listInstanceDiskNames(instanceIds);
    }

    public Map<String, Map<String, String>> listTagsByInstanceId(List<String> instanceIds) {
        return gcpClientActions.listTagsByInstanceId(instanceIds);
    }

    public void deleteHostGroupInstances(List<String> instanceIds) {
        gcpClientActions.deleteHostGroupInstances(instanceIds);
    }

    public void stopHostGroupInstances(List<String> instanceIds) {
        gcpClientActions.stopHostGroupInstances(instanceIds);
    }
}
