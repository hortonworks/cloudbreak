package com.sequenceiq.cloudbreak.cloud.azure.storage;

import org.springframework.stereotype.Component;

import com.azure.resourcemanager.storage.models.SkuName;
import com.azure.resourcemanager.storage.models.StorageAccountSkuType;
import com.sequenceiq.cloudbreak.cloud.azure.AzureDiskType;

@Component
public class SkuTypeResolver {

    public StorageAccountSkuType resolveFromAzureDiskType(AzureDiskType type) {
        StorageAccountSkuType result = null;
        if (type != null) {
            String[] typeComponents = type.value().split("_");
            String prefix = "";
            if (typeComponents[0].toUpperCase().contains("STANDARD")) {
                prefix = "Standard";
            } else if (typeComponents[0].toUpperCase().contains("PREMIUM")) {
                prefix = "Premium";
            }
            result = StorageAccountSkuType.fromSkuName(SkuName.fromString(prefix + "_" + typeComponents[typeComponents.length - 1]));
        }
        return result;
    }

}
