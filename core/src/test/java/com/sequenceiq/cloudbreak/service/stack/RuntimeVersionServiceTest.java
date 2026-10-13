package com.sequenceiq.cloudbreak.service.stack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;

@ExtendWith(MockitoExtension.class)
public class RuntimeVersionServiceTest {

    private static final Long CLUSTER_ID = 1L;

    @Mock
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @InjectMocks
    private RuntimeVersionService underTest;

    @Test
    void theServicePackQualifiedRuntimeVersionIsReadFromTheCdhProduct() {
        when(clusterComponentConfigProvider.getCdhProduct(CLUSTER_ID)).thenReturn(Optional.of(product("CDH", "7.3.1-1.cdh7.3.1.p600.67845063")));

        assertEquals(Optional.of("7.3.1.600"), underTest.getServicePackQualifiedRuntimeVersion(CLUSTER_ID));
    }

    @Test
    void theServicePackQualifiedRuntimeVersionFallsBackToTheShortVersionWhenThereIsNoPatchPart() {
        when(clusterComponentConfigProvider.getCdhProduct(CLUSTER_ID)).thenReturn(Optional.of(product("CDH", "7.3.1")));

        assertEquals(Optional.of("7.3.1"), underTest.getServicePackQualifiedRuntimeVersion(CLUSTER_ID));
    }

    @Test
    void theServicePackQualifiedRuntimeVersionIsEmptyWithoutACdhProduct() {
        when(clusterComponentConfigProvider.getCdhProduct(CLUSTER_ID)).thenReturn(Optional.empty());

        assertEquals(Optional.empty(), underTest.getServicePackQualifiedRuntimeVersion(CLUSTER_ID));
    }

    @Test
    void theServicePackQualifiedRuntimeVersionIsEmptyWithoutACluster() {
        assertEquals(Optional.empty(), underTest.getServicePackQualifiedRuntimeVersion(null));
    }

    @Test
    void theCdhFullVersionIsTheStackVersionWhenThereIsNoPatchPart() {
        when(clusterComponentConfigProvider.getCdhProduct(CLUSTER_ID)).thenReturn(Optional.of(product("CDH", "7.3.1")));

        assertEquals(Optional.of("7.3.1"), underTest.getCdhFullVersion(CLUSTER_ID));
    }

    @Test
    void theCdhFullVersionContainsTheServicePack() {
        when(clusterComponentConfigProvider.getCdhProduct(CLUSTER_ID)).thenReturn(Optional.of(product("CDH", "7.3.2-1.cdh7.3.2.p10000.80393083")));

        assertEquals(Optional.of("7.3.2.10000"), underTest.getCdhFullVersion(CLUSTER_ID));
    }

    private static ClouderaManagerProduct product(String name, String version) {
        ClouderaManagerProduct product = new ClouderaManagerProduct();
        product.setName(name);
        product.setVersion(version);
        return product;
    }

    @Test
    public void testGetCdhVersionFromClouderaManagerProductsIfNameAddedButVersionMissing() {

        ClouderaManagerProduct cdhProduct = getCdhProduct();
        Optional<String> actual = RuntimeVersionService.getRuntimeVersionFromClouderaManagerProducts(List.of(cdhProduct));

        assertTrue(actual.isEmpty());
    }

    @Test
    public void testGetCdhVersionFromClouderaManagerProductsIfNameAndVersionAdded() {

        ClouderaManagerProduct cdhProduct = getCdhProduct("version");
        Optional<String> actual = RuntimeVersionService.getRuntimeVersionFromClouderaManagerProducts(List.of(cdhProduct));

        assertFalse(actual.isEmpty());
    }

    private ClouderaManagerProduct getCdhProduct(String version) {
        ClouderaManagerProduct product = new ClouderaManagerProduct();
        product.setName("CDH");
        product.setVersion(version + "-something");
        return product;
    }

    private ClouderaManagerProduct getCdhProduct() {
        ClouderaManagerProduct product = new ClouderaManagerProduct();
        product.setName("CDH");
        product.setVersion(null);
        return product;
    }
}
