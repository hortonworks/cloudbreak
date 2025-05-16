package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.UpdateType;
import com.sequenceiq.cloudbreak.cloud.aws.common.service.AwsCommonDiskUpdateService;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsNetworkView;
import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.loadbalancer.AwsLoadBalancerLaunchService;
import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.AwsRdsUpgradeService;
import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation.AwsRdsUpgradeValidatorService;
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
import com.sequenceiq.cloudbreak.cloud.model.database.ExternalDatabaseParameters;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.common.api.adjustment.AdjustmentTypeWithThreshold;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.ResourceType;

import freemarker.template.Configuration;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesResponse;

@Service
public class AwsResourceConnector implements ResourceConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsResourceConnector.class);

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
    private AwsRdsUpgradeService awsRdsUpgradeService;

    @Inject
    private AwsRdsUpgradeValidatorService awsRdsUpgradeValidatorService;

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

    @Inject
    private AwsCommonDiskUpdateService awsCommonDiskUpdateService;

    @Inject
    private AwsDatabaseSslCertRotationService awsDatabaseSslCertRotationService;

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
            PersistenceNotifier persistenceNotifier) {
        return awsRdsLaunchService.launch(ac, stack, persistenceNotifier);
    }

    @Override
    public void validateUpgradeDatabaseServer(AuthenticatedContext authenticatedContext, DatabaseStack stack, TargetMajorVersion targetMajorVersion) {
        awsRdsUpgradeValidatorService.validateCustomPropertiesAdded(authenticatedContext, stack);
    }

    @Override
    public List<CloudResourceStatus> launchValidateUpgradeDatabaseServerResources(AuthenticatedContext authenticatedContext, DatabaseStack stack,
            TargetMajorVersion targetMajorVersion, DatabaseStack migratedDbStack, PersistenceNotifier persistenceNotifier) {
        return List.of();
    }

    @Override
    public void cleanupValidateUpgradeDatabaseServerResources(AuthenticatedContext authenticatedContext, DatabaseStack stack, List<CloudResource> resources,
            PersistenceNotifier persistenceNotifier) {
    }

    @Override
    public void upgradeDatabaseServer(AuthenticatedContext authenticatedContext, DatabaseStack originalStack, DatabaseStack stack,
            PersistenceNotifier persistenceNotifier, TargetMajorVersion targetMajorVersion, List<CloudResource> resources) {
        LOGGER.debug("Starting the upgrade of database server to {}", targetMajorVersion);
        awsRdsUpgradeService.upgrade(authenticatedContext, stack, targetMajorVersion, persistenceNotifier, resources);
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
        DescribeDbInstancesResponse describeDbInstancesResponse = awsRdsStatusLookupService.getDescribeDBInstancesResponseForDeleteProtection(ac, stack);
        if (awsRdsStatusLookupService.isDeleteProtectionEnabled(describeDbInstancesResponse)) {
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
    public ExternalDatabaseParameters getDatabaseServerParameters(AuthenticatedContext authenticatedContext, DatabaseStack stack) {
        ExternalDatabaseStatus databaseStatus = awsRdsStatusLookupService.getStatus(authenticatedContext, stack);
        return new ExternalDatabaseParameters(databaseStatus, null, null);
    }

    @Override
    public CloudDatabaseServerSslCertificate getDatabaseServerActiveSslRootCertificate(AuthenticatedContext authenticatedContext, DatabaseStack stack) {
        return awsRdsStatusLookupService.getActiveSslRootCertificate(authenticatedContext, stack);
    }

    @Override
    public List<CloudResourceStatus> update(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources,
        UpdateType type, Optional<String> group) {
        return awsUpdateService.update(authenticatedContext, stack, resources, type, group);
    }

    @Override
    public void updateUserData(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> cloudResources,
            Map<InstanceGroupType, String> userData) throws Exception {
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
    public List<CloudResource> collectResourcesToRemove(AuthenticatedContext authenticatedContext, CloudStack stack,
            List<CloudResource> resources, List<CloudInstance> vms) {
        return null;
    }

    @Override
    public List<CloudResourceStatus> downscale(AuthenticatedContext auth, CloudStack stack, List<CloudResource> resources, List<CloudInstance> vms,
            List<CloudResource> resourcesToRemove) {
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
    public String getDBStackTemplate(DatabaseStack databaseStack) {
        try {
            return freemarkerConfiguration.getTemplate(awsDbCloudformationTemplatePath, "UTF-8").toString();
        } catch (IOException e) {
            throw new CloudConnectorException("can't get freemarker template", e);
        }
    }

    @Override
    public void updateDatabaseRootPassword(AuthenticatedContext authenticatedContext, DatabaseStack databaseStack, String newPassword) {
        awsRdsModifyService.updateMasterUserPassword(authenticatedContext, databaseStack, newPassword);
    }

    @Override
    public void updateDatabaseServerActiveSslRootCertificate(AuthenticatedContext authenticatedContext, DatabaseStack databaseStack, String desiredCertificate) {
        awsDatabaseSslCertRotationService.applyCertificateChange(authenticatedContext, databaseStack, desiredCertificate);
    }

    @Override
    public void migrateDatabaseFromNonSslToSsl(AuthenticatedContext authenticatedContext, DatabaseStack databaseStack) {
        awsRdsModifyService.migrateNonSslToSsl(authenticatedContext, databaseStack.getDatabaseServer());
    }

    @Override
    public void updateDiskVolumes(AuthenticatedContext authenticatedContext, List<String> volumeIds, String diskType, int size) throws Exception {
        awsCommonDiskUpdateService.modifyVolumes(authenticatedContext, volumeIds, diskType, size);
    }

    @Override
    public ResourceType getInstanceResourceType() {
        return ResourceType.AWS_INSTANCE;
    }
}