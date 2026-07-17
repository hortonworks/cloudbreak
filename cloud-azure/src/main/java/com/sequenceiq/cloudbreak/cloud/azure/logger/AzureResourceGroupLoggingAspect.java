package com.sequenceiq.cloudbreak.cloud.azure.logger;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.azure.AzureResourceGroupMetadataProvider;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;

/**
 * Emits a single INFO anchor log with the Azure resource group name whenever a call enters the Azure cloud SPI
 * (the {@code cloud-api} connector interfaces implemented in {@code cloud-azure}). The resource group is deliberately
 * kept out of the shared, provider-agnostic MDC context; correlation with the rest of an operation's log lines relies
 * on the generic MDC fields (flowId, resourceCrn) that already ride along.
 * <p>
 * Only external calls routed through the Spring proxy are intercepted, so internal helper calls between methods of the
 * same connector bean are not logged twice.
 */
@Aspect
@Component
public class AzureResourceGroupLoggingAspect {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureResourceGroupLoggingAspect.class);

    private static final String UNKNOWN_RESOURCE_GROUP = "unknown";

    private final AzureResourceGroupMetadataProvider azureResourceGroupMetadataProvider;

    public AzureResourceGroupLoggingAspect(AzureResourceGroupMetadataProvider azureResourceGroupMetadataProvider) {
        this.azureResourceGroupMetadataProvider = azureResourceGroupMetadataProvider;
    }

    @Pointcut("within(com.sequenceiq.cloudbreak.cloud.azure..*) && execution(public * *(..)) && ("
            + "target(com.sequenceiq.cloudbreak.cloud.Setup) || "
            + "target(com.sequenceiq.cloudbreak.cloud.ResourceConnector) || "
            + "target(com.sequenceiq.cloudbreak.cloud.InstanceConnector) || "
            + "target(com.sequenceiq.cloudbreak.cloud.MetadataCollector) || "
            + "target(com.sequenceiq.cloudbreak.cloud.NetworkConnector) || "
            + "target(com.sequenceiq.cloudbreak.cloud.ResourceVolumeConnector))")
    public void azureSpiEntry() {
    }

    @Before("azureSpiEntry()")
    public void logResourceGroup(JoinPoint joinPoint) {
        try {
            String resourceGroupName = resolveResourceGroupName(joinPoint.getArgs());
            LOGGER.info("Azure SPI {}.{} invoked, resourceGroup=[{}]",
                    joinPoint.getSignature().getDeclaringType().getSimpleName(),
                    joinPoint.getSignature().getName(),
                    resourceGroupName);
        } catch (Exception e) {
            // Logging must never break provisioning.
            LOGGER.debug("Could not resolve Azure resource group for SPI entry logging", e);
        }
    }

    String resolveResourceGroupName(Object[] args) {
        // CloudContext is only needed for the default resource group name fallback; an explicitly set resource group
        // name is carried by the CloudStack/DatabaseStack/CloudInstance itself, so we resolve it even without a context.
        CloudContext cloudContext = findCloudContext(args);
        for (Object arg : args) {
            if (arg instanceof CloudStack cloudStack) {
                return orUnknown(azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, cloudStack));
            }
            if (arg instanceof DatabaseStack databaseStack) {
                return orUnknown(azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, databaseStack));
            }
        }
        CloudInstance cloudInstance = findFirstCloudInstance(args);
        if (cloudInstance != null) {
            return orUnknown(azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, cloudInstance));
        }
        return UNKNOWN_RESOURCE_GROUP;
    }

    private String orUnknown(String resourceGroupName) {
        return StringUtils.isBlank(resourceGroupName) ? UNKNOWN_RESOURCE_GROUP : resourceGroupName;
    }

    private CloudContext findCloudContext(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof AuthenticatedContext authenticatedContext) {
                return authenticatedContext.getCloudContext();
            }
        }
        return null;
    }

    private CloudInstance findFirstCloudInstance(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof CloudInstance cloudInstance) {
                return cloudInstance;
            }
            if (arg instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof CloudInstance cloudInstance) {
                        return cloudInstance;
                    }
                }
            }
        }
        return null;
    }
}
