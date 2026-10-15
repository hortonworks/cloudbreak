package com.sequenceiq.cloudbreak.converter;

import java.util.ArrayList;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.ClouderaManagerV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.product.ClouderaManagerProductV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.repository.ClouderaManagerRepositoryV4Request;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.clouderamanager.ClouderaManagerRepositoryV4RequestToClouderaManagerRepoConverter;
import com.sequenceiq.cloudbreak.service.upgrade.UpgradeComponents;

public final class UpgradeComponentsConverter {

    private UpgradeComponentsConverter() {
    }

    public static UpgradeComponents fromRequest(ClouderaManagerV4Request request) {
        return new UpgradeComponents(convertRepository(request.getRepository()), request.getProducts() == null ? null
                : request.getProducts().stream().map(UpgradeComponentsConverter::convertProduct).toList());
    }

    public static ClouderaManagerV4Request toRequest(UpgradeComponents components) {
        return new ClouderaManagerV4Request().withRepository(convertRepository(components.repository()))
                .withProducts(components.products() == null ? null : components.products().stream()
                        .map(UpgradeComponentsConverter::convertProduct).toList());
    }

    private static ClouderaManagerRepo convertRepository(ClouderaManagerRepositoryV4Request repository) {
        return repository == null ? null : ClouderaManagerRepositoryV4RequestToClouderaManagerRepoConverter.convert(repository);
    }

    private static ClouderaManagerRepositoryV4Request convertRepository(ClouderaManagerRepo repository) {
        return repository == null ? null : new ClouderaManagerRepositoryV4Request().withVersion(repository.getFullVersion())
                .withBaseUrl(repository.getBaseUrl()).withGpgKeyUrl(repository.getGpgKeyUrl());
    }

    private static ClouderaManagerProduct convertProduct(ClouderaManagerProductV4Request product) {
        return product == null ? null : new ClouderaManagerProduct().withName(product.getName()).withVersion(product.getVersion())
                .withParcel(product.getParcel()).withCsd(product.getCsd() == null ? null : new ArrayList<>(product.getCsd()));
    }

    private static ClouderaManagerProductV4Request convertProduct(ClouderaManagerProduct product) {
        return product == null ? null : new ClouderaManagerProductV4Request().withName(product.getName()).withVersion(product.getVersion())
                .withParcel(product.getParcel()).withCsd(product.getCsd() == null ? null : new ArrayList<>(product.getCsd()));
    }
}
