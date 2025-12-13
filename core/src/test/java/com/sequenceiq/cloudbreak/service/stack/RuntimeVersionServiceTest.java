package com.sequenceiq.cloudbreak.service.stack;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;

@ExtendWith(MockitoExtension.class)
public class RuntimeVersionServiceTest {

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
