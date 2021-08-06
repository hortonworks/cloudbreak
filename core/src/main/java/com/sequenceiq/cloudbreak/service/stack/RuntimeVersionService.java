package com.sequenceiq.cloudbreak.service.stack;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProductBase;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;

@Service
public class RuntimeVersionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RuntimeVersionService.class);

    @Inject
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    public Optional<String> getRuntimeVersion(Long clusterId) {
        if (clusterId != null) {
            List<ClouderaManagerProduct> clouderaManagerProductDetails =
                    clusterComponentConfigProvider.getClouderaManagerProductDetails(clusterId);
            return getRuntimeVersionFromClouderaManagerProducts(clouderaManagerProductDetails);
        }
        return Optional.empty();
    }

    public static Optional<String> getRuntimeVersionFromClouderaManagerProducts(List<? extends ClouderaManagerProductBase> products) {
        return products.stream()
                .filter(product -> "CDH".equals(product.getName()))
                .filter(product -> {
                    if (product.getVersion() == null) {
                        LOGGER.info("Cannot find the CDH's version in the request. We fallback to the default CDH version");
                    }
                    return product.getVersion() != null;
                })
                .map(product -> StringUtils.substringBefore(product.getVersion(), "-"))
                .findFirst();
    }
}
