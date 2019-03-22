package com.sequenceiq.cloudbreak.cloud.azure.storage;

import static org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.microsoft.azure.management.storage.StorageAccountSkuType;
import com.sequenceiq.cloudbreak.cloud.azure.AzureDiskType;

@RunWith(Parameterized.class)
public class SkuTypeResolverTest {

    private SkuTypeResolver underTest;

    private DiskTypeSkuPair pair;

    public SkuTypeResolverTest(DiskTypeSkuPair pair) {
        this.pair = pair;
        underTest = new SkuTypeResolver();
    }

    @Test
    public void testResolving() {
        StorageAccountSkuType result = underTest.resolveFromAzureDiskType(pair.getAzureDiskType());

        Assert.assertEquals(pair.getExpectedStorageAccountSkuType().name(), result.name());
    }

    @Parameters(name = "{index}: ({0})={1}")
    public static Iterable<DiskTypeSkuPair> data() {
        return Arrays.asList(DiskTypeSkuPair.values());
    }

    private enum DiskTypeSkuPair {

        LOCALLY_REDUNDANT(AzureDiskType.LOCALLY_REDUNDANT, StorageAccountSkuType.STANDARD_LRS),

        GEO_REDUNDANT(AzureDiskType.GEO_REDUNDANT, StorageAccountSkuType.STANDARD_GRS),

        STANDARD_SSD_LRS(AzureDiskType.STANDARD_SSD_LRS, StorageAccountSkuType.STANDARD_LRS),

        PREMIUM_LOCALLY_REDUNDANT(AzureDiskType.PREMIUM_LOCALLY_REDUNDANT, StorageAccountSkuType.PREMIUM_LRS);

        private AzureDiskType azureDiskType;

        private StorageAccountSkuType expectedStorageAccountSkuType;

        DiskTypeSkuPair(AzureDiskType azureDiskType, StorageAccountSkuType storageAccountSkuType) {
            this.azureDiskType = azureDiskType;
            this.expectedStorageAccountSkuType = storageAccountSkuType;
        }

        public AzureDiskType getAzureDiskType() {
            return azureDiskType;
        }

        public StorageAccountSkuType getExpectedStorageAccountSkuType() {
            return expectedStorageAccountSkuType;
        }

    }

}