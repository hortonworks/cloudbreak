package com.sequenceiq.cloudbreak.cloud.azure.storage;

import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import com.microsoft.azure.management.storage.StorageAccountSkuType;
import com.sequenceiq.cloudbreak.cloud.azure.AzureDiskType;

@ExtendWith(MockitoExtension.class)
public class SkuTypeResolverTest {

    private SkuTypeResolver underTest = new SkuTypeResolver();

    @ParameterizedTest(name = "{index}: ({0})={1}")
    @MethodSource("data")
    public void testResolving(DiskTypeSkuPair pair) {
        StorageAccountSkuType result = underTest.resolveFromAzureDiskType(pair.getAzureDiskType());

        Assertions.assertEquals(pair.getExpectedStorageAccountSkuType().name(), result.name());
    }

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