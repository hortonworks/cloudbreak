package com.sequenceiq.cloudbreak.cloud.gcp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.TemplatingNotSupportedException;
import com.sequenceiq.cloudbreak.cloud.gcp.sql.GcpDatabaseServerCertificateService;
import com.sequenceiq.cloudbreak.cloud.gcp.sql.GcpDatabaseServerUpdateService;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.TlsInfo;
import com.sequenceiq.cloudbreak.cloud.model.database.CloudDatabaseServerSslCertificate;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.service.CloudbreakResourceNameService;
import com.sequenceiq.cloudbreak.cloud.template.AbstractResourceConnector;
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext;
import com.sequenceiq.cloudbreak.cloud.template.init.ContextBuilders;
import com.sequenceiq.cloudbreak.cloud.template.loadbalancer.LoadBalancerResourceService;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class GcpResourceConnector extends AbstractResourceConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(GcpResourceConnector.class);

    @Inject
    private LoadBalancerResourceService loadBalancerResourceService;

    @Inject
    private GcpDatabaseServerUpdateService gcpDatabaseServerUpdateService;

    @Inject
    private GcpDatabaseServerCertificateService gcpDatabaseServerCertificateService;

    @Inject
    private ContextBuilders contextBuilders;

    @Override
    public TlsInfo getTlsInfo(AuthenticatedContext authenticatedContext, CloudStack cloudStack) {
        return new TlsInfo(false);
    }

    @Override
    public String getStackTemplate() throws TemplatingNotSupportedException {
        throw new TemplatingNotSupportedException();
    }

    @Override
    public String getDBStackTemplate(DatabaseStack databaseStack) {
        return "";
    }

    @Override
    public void updateDiskVolumes(AuthenticatedContext authenticatedContext, List<String> volumeIds, String diskType, int size) throws Exception {
        throw new UnsupportedOperationException("Method not implemented.");
    }

    @Override
    public List<CloudResourceStatus> launchLoadBalancers(AuthenticatedContext auth, CloudStack stack, PersistenceNotifier persistenceNotifier)
            throws Exception {
        CloudContext cloudContext = auth.getCloudContext();
        Platform platform = cloudContext.getPlatform();

        ResourceBuilderContext context = contextBuilders.get(platform).contextInit(cloudContext, auth, stack.getNetwork(), true);

        return loadBalancerResourceService.buildResources(context, auth, stack);
    }

    @Override
    public List<CloudResourceStatus> updateLoadBalancers(AuthenticatedContext authenticatedContext, CloudStack stack, PersistenceNotifier persistenceNotifier)
            throws Exception {
        return launchLoadBalancers(authenticatedContext, stack, persistenceNotifier);
    }

    @Override
    protected ResourceType getDiskResourceType() {
        return ResourceType.GCP_ATTACHED_DISKSET;
    }

    @Override
    public List<CloudResource> collectResourcesToRemove(AuthenticatedContext authenticatedContext, CloudStack stack,
            List<CloudResource> resources, List<CloudInstance> vms) {
        List<CloudResource> result = Lists.newArrayList();
        result.addAll(getDeletableResources(resources, vms));
        result.addAll(collectProviderSpecificResources(resources, vms));
        return result;
    }

    @Override
    protected Collection<CloudResource> getDeletableResources(Iterable<CloudResource> resources, Iterable<CloudInstance> instances) {
        Collection<CloudResource> result = new ArrayList<>();
        for (CloudInstance instance : instances) {
            String instanceId = instance.getInstanceId();
            for (CloudResource resource : resources) {
                if (instanceId.equalsIgnoreCase(resource.getName()) || instanceId.equalsIgnoreCase(resource.getInstanceId())) {
                    result.add(resource);
                }
            }
        }
        return result;
    }

    @Override
    protected List<CloudResource> collectProviderSpecificResources(List<CloudResource> resources, List<CloudInstance> vms) {
        List<CloudResource> result = new ArrayList<>();
        for (CloudInstance instance : vms) {
            String instanceId = instance.getInstanceId();
            String baseName = instanceId.substring(0, instanceId.lastIndexOf(CloudbreakResourceNameService.DELIMITER));
            for (CloudResource resource : resources) {
                if (resource.getType() == ResourceType.GCP_RESERVED_IP && resource.getName().startsWith(baseName)) {
                    result.add(resource);
                }
            }
        }
        return result;
    }

    @Override
    public List<CloudResourceStatus> check(AuthenticatedContext authenticatedContext, List<CloudResource> resources) {
        return List.of();
    }

    @Override
    public void updateUserData(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources,
            Map<InstanceGroupType, String> userData) {
        LOGGER.info("Update userdata is not implemented on GCP!");
    }

    @Override
    public void updateDatabaseRootPassword(AuthenticatedContext authenticatedContext, DatabaseStack databaseStack, String newPassword) {
        gcpDatabaseServerUpdateService.updateRootUserPassword(authenticatedContext, databaseStack, newPassword);
    }

    @Override
    public CloudDatabaseServerSslCertificate getDatabaseServerActiveSslRootCertificate(AuthenticatedContext authenticatedContext, DatabaseStack stack)
            throws Exception {
        return gcpDatabaseServerCertificateService.getActiveSslRootCertificate(authenticatedContext, stack);
    }

    @Override
    public ResourceType getInstanceResourceType() {
        return ResourceType.GCP_INSTANCE;
    }
}
