package com.sequenceiq.cloudbreak.cloud.azure;

import static com.sequenceiq.common.model.PrivateEndpointType.USE_PRIVATE_ENDPOINT;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.azure.view.AzureDatabaseServerView;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureNetworkView;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.common.anonymizer.AnonymizerUtil;
import com.sequenceiq.cloudbreak.util.FreeMarkerTemplateUtils;

import freemarker.template.TemplateException;

@Service
public class AzureDatabaseTemplateBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureDatabaseTemplateBuilder.class);

    private static final String GENERAL_PURPOSE = "GeneralPurpose";

    private static final String MEMORY_OPTIMIZED = "MemoryOptimized";

    private static final Set<String> GENERAL_PURPOSE_INSTANCE_TYPES = Set.of("GP_Gen5_2", "GP_Gen5_4", "GP_Gen5_8", "GP_Gen5_16", "GP_Gen5_32");

    private static final Set<String> MEMORY_OPTIMIZED_INSTANCE_TYPES = Set.of("MO_Gen5_2", "MO_Gen5_4", "MO_Gen5_8", "MO_Gen5_16", "MO_Gen5_32");

    private static final Pattern ENCRYPTION_KEY_URL_VAULT_NAME = Pattern.compile("https://([^.]+)\\.vault.*");

    private static final Pattern ENCRYPTION_KEY_NAME = Pattern.compile(".*\\/keys\\/([^.]+)\\/.*");

    private static final Pattern ENCRYPTION_KEY_VERSION = Pattern.compile(".*\\/keys\\/.*\\/([^.]+)");

    @Value("${cb.azure.database.template.batchSize}")
    private int defaultBatchSize;

    @Inject
    private FreeMarkerTemplateUtils freeMarkerTemplateUtils;

    @Inject
    private AzureDatabaseTemplateProvider azureDatabaseTemplateProvider;

    @Inject
    private AzureUtils azureUtils;

    public String build(CloudContext cloudContext, DatabaseStack databaseStack) {
        try {
            String location = cloudContext.getLocation().getRegion().getRegionName();
            AzureNetworkView azureNetworkView = new AzureNetworkView(databaseStack.getNetwork());
            AzureDatabaseServerView azureDatabaseServerView = new AzureDatabaseServerView(databaseStack.getDatabaseServer());
            Map<String, Object> model = new HashMap<>();
            model.put("usePrivateEndpoints", USE_PRIVATE_ENDPOINT.equals(azureNetworkView.getEndpointType()));
            model.put("subnetIdForPrivateEndpoint", azureNetworkView.getSubnetIdForPrivateEndpoint());
            model.put("adminLoginName", azureDatabaseServerView.getAdminLoginName());
            model.put("adminPassword", azureDatabaseServerView.getAdminPassword());
            model.put("backupRetentionDays", azureDatabaseServerView.getBackupRetentionDays());
            model.put("dbServerName", azureDatabaseServerView.getDbServerName());
            model.put("dbVersion", azureDatabaseServerView.getDbVersion());
            model.put("geoRedundantBackup", azureDatabaseServerView.getGeoRedundantBackup());
            model.put("location", location);
            if (azureDatabaseServerView.getPort() != null) {
                LOGGER.warn("Found port {} in database stack, but Azure ignores it", azureDatabaseServerView.getPort());
            }
            model.put("dataEncryption", false);
            String keyVaultUrl = azureDatabaseServerView.getKeyVaultUrl();
            if (keyVaultUrl != null) {
                String keyVaultName;
                String keyName;
                String keyVersion;
                Matcher matcher = ENCRYPTION_KEY_URL_VAULT_NAME.matcher(keyVaultUrl);
                if (matcher.matches()) {
                    keyVaultName = matcher.group(1);
                } else {
                    throw new IllegalArgumentException(String.format("keyVaultName cannot be fetched from encryptionKeyUrl %s.", keyVaultUrl));
                }
                matcher = ENCRYPTION_KEY_NAME.matcher(keyVaultUrl);
                if (matcher.matches()) {
                    keyName = matcher.group(1);
                } else {
                    throw new IllegalArgumentException(String.format("keyName cannot be fetched from encryptionKeyUrl %s.", keyVaultUrl));
                }
                matcher = ENCRYPTION_KEY_VERSION.matcher(keyVaultUrl);
                if (matcher.matches()) {
                    keyVersion = matcher.group(1);
                } else {
                    throw new IllegalArgumentException(String.format("keyVersion cannot be fetched from encryptionKeyUrl %s.", keyVaultUrl));
                }

                model.put("dataEncryption", true);
                model.put("keyVaultName", keyVaultName);
                model.put("keyVaultResourceGroupName", azureDatabaseServerView.getKeyVaultResourceGroupName());
                model.put("keyName", keyName);
                model.put("keyVersion", keyVersion);
            }
            model.put("serverTags", databaseStack.getTags());
            model.put("skuCapacity", azureDatabaseServerView.getSkuCapacity());
            model.put("skuFamily", azureDatabaseServerView.getSkuFamily());
            model.put("skuName", azureDatabaseServerView.getSkuName());
            model.put("skuSizeMB", azureDatabaseServerView.getAllocatedStorageInMb());
            model.put("skuTier", getSkuTier(azureDatabaseServerView));
            model.put("useSslEnforcement", azureDatabaseServerView.isUseSslEnforcement());
            model.put("storageAutoGrow", azureDatabaseServerView.getStorageAutoGrow());
            model.put("subnets", azureNetworkView.getSubnets());
            String[] subnets = azureNetworkView.getSubnets().split(",");
            model.put("subnetIdList", subnets);
            // if subnet number is 1 then Azure does not create the endpoints if the batchsize is 5
            model.put("batchSize", azureNetworkView.getSubnets().split(",").length >= defaultBatchSize ? defaultBatchSize : 1);
            model.put("location", azureDatabaseServerView.getLocation());
            model.put("privateEndpointName", String.format("pe-%s-to-%s",
                    azureUtils.encodeString(getSubnetName(azureNetworkView.getSubnetList().get(0))), azureDatabaseServerView.getDbServerName()));
            String generatedTemplate = freeMarkerTemplateUtils.processTemplateIntoString(azureDatabaseTemplateProvider.getTemplate(databaseStack), model);
            LOGGER.debug("Generated ARM database template: {}", AnonymizerUtil.anonymize(generatedTemplate));
            return generatedTemplate;
        } catch (IOException | TemplateException e) {
            throw new CloudConnectorException("Failed to process the ARM TemplateBuilder", e);
        }
    }

    private String getSkuTier(AzureDatabaseServerView azureDatabaseServerView) {
        String skuTier = azureDatabaseServerView.getSkuTier();
        String skuName = azureDatabaseServerView.getSkuName();
        if (Objects.isNull(skuTier)) {
            if (Objects.isNull(skuName)) {
                return null;
            } else {
                return getSkuTierFromSkuName(skuName);
            }
        } else {
            return azureDatabaseServerView.getSkuTier();
        }
    }

    private String getSkuTierFromSkuName(String skuName) {
        if (GENERAL_PURPOSE_INSTANCE_TYPES.contains(skuName)) {
            return GENERAL_PURPOSE;
        } else if (MEMORY_OPTIMIZED_INSTANCE_TYPES.contains(skuName)) {
            return MEMORY_OPTIMIZED;
        } else {
            return null;
        }
    }

    private String getSubnetName(String subnetId) {
        return StringUtils.substringAfterLast(subnetId, "/");
    }
}
