package com.sequenceiq.cloudbreak.cloud.gcp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.Instance;
import com.sequenceiq.cloudbreak.cloud.InstanceConnector;
import com.sequenceiq.cloudbreak.cloud.MetadataCollector;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.common.type.CloudRegion;
import com.sequenceiq.cloudbreak.cloud.template.AbstractInstanceConnector;

@Service
public class GcpInstanceConnector extends AbstractInstanceConnector implements InstanceConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpInstanceConnector.class);

    @Inject
    private GcpMetadataCollector metadataCollector;

    @Override
    public MetadataCollector metadata() {
        return metadataCollector;
    }

    @Override
    public List<CloudVmInstanceStatus> check(AuthenticatedContext ac, List<CloudInstance> vms) {
        List<CloudVmInstanceStatus> statuses = new ArrayList<>();
        CloudCredential credential = ac.getCloudCredential();
        CloudContext cloudContext = ac.getCloudContext();
        Compute compute = GcpStackUtil.buildCompute(credential);
        for (CloudInstance instance : vms) {
            InstanceStatus status = InstanceStatus.UNKNOWN;
            try {
                Instance executeInstance = getInstance(cloudContext, credential, compute, instance.getInstanceId());
                if ("RUNNING".equals(executeInstance.getStatus())) {
                    status = InstanceStatus.STARTED;
                } else if ("TERMINATED".equals(executeInstance.getStatus())) {
                    status = InstanceStatus.STOPPED;
                }
            } catch (GoogleJsonResponseException e) {
                if (e.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                    status = InstanceStatus.TERMINATED;
                } else {
                    LOGGER.warn(String.format("Instance %s is not reachable", instance), e);
                }
            } catch (IOException e) {
                LOGGER.warn(String.format("Instance %s is not reachable", instance), e);
            }
            statuses.add(new CloudVmInstanceStatus(instance, status));
        }
        return statuses;
    }

    @Override
    public String getConsoleOutput(AuthenticatedContext authenticatedContext, CloudInstance vm) {
        CloudCredential credential = authenticatedContext.getCloudCredential();
        try {
            Compute.Instances.GetSerialPortOutput instanceGet = GcpStackUtil.buildCompute(credential).instances()
                    .getSerialPortOutput(GcpStackUtil.getProjectId(credential),
                            CloudRegion.valueOf(authenticatedContext.getCloudContext().getRegion()).value(), vm.getInstanceId());
            return instanceGet.execute().getContents();
        } catch (Exception e) {
            throw new GcpResourceException("Couldn't parse SSH fingerprint from console output.", e);
        }
    }

    private Instance getInstance(CloudContext context, CloudCredential credential, Compute compute, String instanceName) throws IOException {
        return compute.instances().get(GcpStackUtil.getProjectId(credential),
                CloudRegion.valueOf(context.getRegion()).value(), instanceName).execute();
    }
}
