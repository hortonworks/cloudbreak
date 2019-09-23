package com.sequenceiq.distrox.v1.distrox.converter;

import static com.sequenceiq.cloudbreak.util.NullUtil.doIfNotNull;
import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;

import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.ClouderaManagerV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.product.ClouderaManagerProductV4Request;
import com.sequenceiq.distrox.api.v1.distrox.model.cluster.cm.ClouderaManagerV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.cluster.cm.product.ClouderaManagerProductV1Request;

@Component
public class ClouderaManagerV1ToClouderaManagerV4Converter {

    @Inject
    private ClouderaManagerRepositoryV1ToClouderaManagerRepositoryV4Converter repositoryConverter;

    public ClouderaManagerV4Request convert(ClouderaManagerV1Request source) {
        ClouderaManagerV4Request response = new ClouderaManagerV4Request();
        doIfNotNull(source.getProducts(), products ->
                response.setProducts(products.stream().map(this::convertProduct).collect(Collectors.toList())));
        response.setRepository(getIfNotNull(source.getRepository(), repositoryConverter::convert));
        response.setEnableAutoTls(source.getEnableAutoTls());
        return response;
    }

    public ClouderaManagerV1Request convert(ClouderaManagerV4Request source) {
        ClouderaManagerV1Request response = new ClouderaManagerV1Request();
        doIfNotNull(source.getProducts(), products ->
                response.setProducts(products.stream().map(this::convertProduct).collect(Collectors.toList())));
        response.setRepository(getIfNotNull(source.getRepository(), repositoryConverter::convert));
        response.setEnableAutoTls(source.getEnableAutoTls());
        return response;
    }

    public ClouderaManagerProductV4Request convertProduct(ClouderaManagerProductV1Request source) {
        ClouderaManagerProductV4Request response = new ClouderaManagerProductV4Request();
        response.setName(source.getName());
        response.setParcel(source.getParcel());
        response.setVersion(source.getVersion());
        response.setCsd(source.getCsd());
        return response;
    }

    public ClouderaManagerProductV1Request convertProduct(ClouderaManagerProductV4Request source) {
        ClouderaManagerProductV1Request response = new ClouderaManagerProductV1Request();
        response.setName(source.getName());
        response.setParcel(source.getParcel());
        response.setVersion(source.getVersion());
        response.setCsd(source.getCsd());
        return response;
    }
}
