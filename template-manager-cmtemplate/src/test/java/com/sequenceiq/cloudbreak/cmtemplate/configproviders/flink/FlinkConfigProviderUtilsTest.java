package com.sequenceiq.cloudbreak.cmtemplate.configproviders.flink;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;

class FlinkConfigProviderUtilsTest {

    private final FlinkConfigProviderUtils utils = new FlinkConfigProviderUtils();

    @Test
    void testGetFlinkProduct() {
        assertThatThrownBy(() -> utils.getFlinkProduct(null)).isInstanceOf(NullPointerException.class);

        List<ClouderaManagerProduct> products = List.of();
        Optional<ClouderaManagerProduct> flinkProduct = utils.getFlinkProduct(products);
        assertThat(flinkProduct).isEmpty();

        products = getDummyProducts("SPARK", "LIVY", "HIVE");
        flinkProduct = utils.getFlinkProduct(products);
        assertThat(flinkProduct).isEmpty();

        products = getDummyProducts("FLINK");
        flinkProduct = utils.getFlinkProduct(products);
        assertThat(flinkProduct).isNotEmpty();
        assertThat(flinkProduct.get()).isEqualTo(products.getFirst());
    }

    @Test
    void testAddReleaseNameIfNeeded() {
        List<ApiClusterTemplateConfig> configList = new ArrayList<>();
        Optional<ClouderaManagerProduct> flinkProduct = Optional.empty();

        // No Flink product
        utils.addReleaseNameIfNeeded("7.3.1", configList, flinkProduct);
        assertThat(configList).isEmpty();

        // Non-unified Flink product version
        flinkProduct = Optional.of(new ClouderaManagerProduct().withVersion("1.18.1-csadh1.12.0.0-cdh7.2.18.0-123-12345678"));
        utils.addReleaseNameIfNeeded("7.3.1", configList, flinkProduct);
        assertThat(configList).isEmpty();

        // Too old CDH version
        flinkProduct = Optional.of(new ClouderaManagerProduct().withVersion("1.19.1-csa1.14.0.0-12345678"));
        utils.addReleaseNameIfNeeded("7.2.18", configList, flinkProduct);
        assertThat(configList).isEmpty();

        // Unified Flink version with 7.3.1
        utils.addReleaseNameIfNeeded("7.3.1", configList, flinkProduct);
        assertThat(configList).hasSize(1);
        assertThat(configList.getFirst().getName()).isEqualTo("release.name");
        assertThat(configList.getFirst().getValue()).isEqualTo("CSA-DH");
    }

    private List<ClouderaManagerProduct> getDummyProducts(String... productNames) {
        return Arrays.stream(productNames)
                .map(name -> new ClouderaManagerProduct().withName(name))
                .collect(Collectors.toList());
    }
}
