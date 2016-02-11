package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.SssdConfigResponse;
import com.sequenceiq.cloudbreak.domain.SssdConfig;

@Component
public class SssdConfigToJsonConverter extends AbstractConversionServiceAwareConverter<SssdConfig, SssdConfigResponse> {
    @Override
    public SssdConfigResponse convert(SssdConfig config) {
        SssdConfigResponse json = new SssdConfigResponse();
        json.setName(config.getName());
        json.setDescription(config.getDescription());
        json.setProviderType(config.getProviderType());
        json.setUrl(config.getUrl());
        json.setSchema(config.getSchema());
        json.setBaseSearch(config.getBaseSearch());
        json.setTlsReqcert(config.getTlsReqcert());
        json.setAdServer(config.getAdServer());
        json.setKerberosServer(config.getKerberosServer());
        json.setKerberosRealm(config.getKerberosRealm());
        json.setConfiguration(config.getConfiguration());
        json.setId(config.getId());
        json.setPublicInAccount(config.isPublicInAccount());
        return json;
    }
}
