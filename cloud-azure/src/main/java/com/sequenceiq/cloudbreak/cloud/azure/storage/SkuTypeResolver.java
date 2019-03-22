package com.sequenceiq.cloudbreak.cloud.azure.storage;

import org.springframework.stereotype.Component;

import com.microsoft.azure.management.storage.SkuName;
import com.microsoft.azure.management.storage.StorageAccountSkuType;
import com.sequenceiq.cloudbreak.cloud.azure.AzureDiskType;

@Component
public class SkuTypeResolver {

    public StorageAccountSkuType resolveFromAzureDiskType(AzureDiskType type) {
        StorageAccountSkuType result = null;
        if (type != null) {
            String[] typeComponents = type.value().split("_");
            String prefix = "";
            if (typeComponents[0].toUpperCase().contains("STANDARD")) {
                prefix = "STANDARD";
            } else if (typeComponents[0].toUpperCase().contains("PREMIUM")) {
                prefix = "PREMIUM";
            }
            result = StorageAccountSkuType.fromSkuName(SkuName.fromString(prefix + "_" + typeComponents[typeComponents.length - 1]));
        }
        return result;
    }

}
