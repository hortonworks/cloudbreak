package com.sequenceiq.it.cloudbreak.testcase.e2e.azure;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.it.cloudbreak.testcase.e2e.CloudFunctionality;
import com.sequenceiq.it.cloudbreak.util.azure.azurevm.action.AzureClientActions;

@Component
public class AzureCloudFunctionality implements CloudFunctionality {

    @Inject
    private AzureClientActions azureClientActions;

    @Override
    public List<String> listInstanceVolumeIds(List<String> instanceIds) {
        return azureClientActions.listInstanceVolumeIds(instanceIds);
    }

    @Override
    public void deleteInstances(List<String> instanceIds) {
        azureClientActions.deleteInstances(instanceIds);
    }

    @Override
    public void stopInstances(List<String> instanceIds) {
        azureClientActions.stopInstances(instanceIds);
    }
}
