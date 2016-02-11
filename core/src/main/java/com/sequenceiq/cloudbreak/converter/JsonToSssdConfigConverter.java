package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.SssdConfigRequest;
import com.sequenceiq.cloudbreak.domain.SssdConfig;

@Component
public class JsonToSssdConfigConverter extends AbstractConversionServiceAwareConverter<SssdConfigRequest, SssdConfig> {

    @Override
    public SssdConfig convert(SssdConfigRequest json) {
        SssdConfig config = new SssdConfig();
        config.setName(json.getName());
        config.setDescription(json.getDescription());
        config.setProviderType(json.getProviderType());
        config.setUrl(json.getUrl());
        config.setSchema(json.getSchema());
        config.setBaseSearch(json.getBaseSearch());
        config.setTlsReqcert(json.getTlsReqcert());
        config.setAdServer(json.getAdServer());
        config.setKerberosServer(json.getKerberosServer());
        config.setKerberosRealm(json.getKerberosRealm());
        config.setConfiguration(json.getConfiguration());
        return config;
    }
}
