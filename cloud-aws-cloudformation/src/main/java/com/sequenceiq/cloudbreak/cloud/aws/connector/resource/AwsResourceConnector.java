package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import static java.util.Collections.singletonList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.amazonaws.services.rds.model.DescribeDBInstancesResult;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsNetworkView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.ExternalDatabaseStatus;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.TlsInfo;
import com.sequenceiq.cloudbreak.cloud.model.database.CloudDatabaseServerSslCertificate;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.common.api.adjustment.AdjustmentTypeWithThreshold;

import freemarker.template.Configuration;

@Service
public class AwsResourceConnector implements ResourceConnector<Object> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AwsResourceConnector.class);

    private static final List<String> CAPABILITY_IAM = singletonList("CAPABILITY_IAM");

    private static final List<String> UPSCALE_PROCESSES = singletonList("Launch");

    private static final String CFS_OUTPUT_EIPALLOCATION_ID = "EIPAllocationID";

    private static final String S3_ACCESS_ROLE = "S3AccessRole";

    private static final String CREATED_VPC = "CreatedVpc";

    private static final String CREATED_SUBNET = "CreatedSubnet";

    @Inject
    private Configuration freemarkerConfiguration;

    @Value("${cb.aws.vpc:}")
    private String cloudbreakVpc;

    @Value("${cb.aws.cf.template.new.path:}")
    private String awsCloudformationTemplatePath;

    @Value("${cb.aws.cf.template.newdb.path:}")
    private String awsDbCloudformationTemplatePath;

    @Inject
    private AwsLaunchService awsLaunchService;

    @Inject
    private AwsRdsLaunchService awsRdsLaunchService;

    @Inject
    private AwsRdsTerminateService awsRdsTerminateService;

    @Inject
    private AwsRdsModifyService awsRdsModifyService;

    @Inject
    private AwsTerminateService awsTerminateService;

    @Inject
    private AwsUpscaleService awsUpscaleService;

    @Inject
    private AwsDownscaleService awsDownscaleService;

    @Inject
    private AwsUpdateService awsUpdateService;

    @Inject
    private AwsRdsStartService awsRdsStartService;

    @Inject
    private AwsRdsStopService awsRdsStopService;

    @Inject
    private AwsRdsStatusLookupService awsRdsStatusLookupService;

    @Inject
    private AwsLoadBalancerLaunchService awsLoadBalancerLaunchService;

    @Override
    public List<CloudResourceStatus> launch(AuthenticatedContext ac, CloudStack stack, PersistenceNotifier resourceNotifier,
            AdjustmentTypeWithThreshold adjustmentTypeWithThreshold) throws Exception {
        return awsLaunchService.launch(ac, stack, resourceNotifier, adjustmentTypeWithThreshold);
    }

    @Override
    public List<CloudResourceStatus> launchLoadBalancers(AuthenticatedContext authenticatedContext, CloudStack stack, PersistenceNotifier persistenceNotifier) {
        return awsLoadBalancerLaunchService.updateCloudformationWithLoadBalancers(authenticatedContext, stack, persistenceNotifier);
    }

    @Override
    public List<CloudResourceStatus> launchDatabaseServer(AuthenticatedContext ac, DatabaseStack stack,
            PersistenceNotifier persistenceNotifier) throws Exception {
        return awsRdsLaunchService.launch(ac, stack, persistenceNotifier);
    }

    private boolean deployingToSameVPC(AwsNetworkView awsNetworkView, boolean existingVPC) {
        return StringUtils.isNotEmpty(cloudbreakVpc) && existingVPC && awsNetworkView.getExistingVpc().equals(cloudbreakVpc);
    }

    @Override
    public List<CloudResourceStatus> check(AuthenticatedContext authenticatedContext, List<CloudResource> resources) {
        return new ArrayList<>();
    }

    @Override
    public List<CloudResourceStatus> terminate(AuthenticatedContext ac, CloudStack stack, List<CloudResource> resources) {
        return awsTerminateService.terminate(ac, stack, resources);
    }

    @Override
    public List<CloudResourceStatus> terminateDatabaseServer(AuthenticatedContext ac, DatabaseStack stack,
            List<CloudResource> resources, PersistenceNotifier persistenceNotifier, boolean force) throws Exception {
        DescribeDBInstancesResult describeDBInstancesResult = awsRdsStatusLookupService.getDescribeDBInstancesResultForDeleteProtection(ac, stack);
        if (awsRdsStatusLookupService.isDeleteProtectionEnabled(describeDBInstancesResult)) {
            LOGGER.debug("Delete protection is enabled for DB: {}, Disabling it", stack.getDatabaseServer().getServerId());
            awsRdsModifyService.disableDeleteProtection(ac, stack);
        }
        return awsRdsTerminateService.terminate(ac, stack, force, persistenceNotifier, resources);
    }

    @Override
    public void startDatabaseServer(AuthenticatedContext ac, DatabaseStack stack) throws Exception {
        awsRdsStartService.start(ac, stack);
    }

    @Override
    public void stopDatabaseServer(AuthenticatedContext ac, DatabaseStack stack) throws Exception {
        awsRdsStopService.stop(ac, stack);
    }

    @Override
    public ExternalDatabaseStatus getDatabaseServerStatus(AuthenticatedContext authenticatedContext, DatabaseStack stack) throws Exception {
        return awsRdsStatusLookupService.getStatus(authenticatedContext, stack);
    }

    @Override
    public CloudDatabaseServerSslCertificate getDatabaseServerActiveSslRootCertificate(AuthenticatedContext authenticatedContext, DatabaseStack stack) {
        return awsRdsStatusLookupService.getActiveSslRootCertificate(authenticatedContext, stack);
    }

    @Override
    public List<CloudResourceStatus> update(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources) {
        return awsUpdateService.update(authenticatedContext, stack, resources);
    }

    @Override
    public void updateUserData(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> cloudResources, String userData)
            throws Exception {
        awsUpdateService.updateUserData(authenticatedContext, stack, cloudResources, userData);
    }

    @Override
    public void checkUpdate(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources) throws Exception {
        awsUpdateService.checkUpdate(authenticatedContext, stack, resources);
    }

    @Override
    public List<CloudResourceStatus> upscale(AuthenticatedContext ac, CloudStack stack, List<CloudResource> resources,
            AdjustmentTypeWithThreshold adjustmentTypeWithThreshold) {
        return awsUpscaleService.upscale(ac, stack, resources, adjustmentTypeWithThreshold);
    }

    @Override
    public Object collectResourcesToRemove(AuthenticatedContext authenticatedContext, CloudStack stack,
            List<CloudResource> resources, List<CloudInstance> vms) {
        return null;
    }

    @Override
    public List<CloudResourceStatus> downscale(AuthenticatedContext auth, CloudStack stack, List<CloudResource> resources, List<CloudInstance> vms,
            Object resourcesToRemove) {
        return awsDownscaleService.downscale(auth, stack, resources, vms);
    }

    @Override
    public TlsInfo getTlsInfo(AuthenticatedContext authenticatedContext, CloudStack cloudStack) {
        Network network = cloudStack.getNetwork();
        AwsNetworkView networkView = new AwsNetworkView(network);
        boolean sameVPC = deployingToSameVPC(networkView, networkView.isExistingVPC());
        return new TlsInfo(sameVPC);
    }

    @Override
    public String getStackTemplate() {
        try {
            return freemarkerConfiguration.getTemplate(awsCloudformationTemplatePath, "UTF-8").toString();
        } catch (IOException e) {
            throw new CloudConnectorException("can't get freemarker template", e);
        }
    }

    @Override
    public String getDBStackTemplate() {
        try {
            return freemarkerConfiguration.getTemplate(awsDbCloudformationTemplatePath, "UTF-8").toString();
        } catch (IOException e) {
            throw new CloudConnectorException("can't get freemarker template", e);
        }
    }
}
