package com.sequenceiq.distrox.v1.distrox.converter;

import static com.sequenceiq.cloudbreak.util.NullUtil.ifNotNull;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.storage.AdlsCloudStorageV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.storage.AdlsGen2CloudStorageV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.storage.S3CloudStorageV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.storage.WasbCloudStorageV4Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.cluster.storage.parameter.AdlsCloudStorageV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.cluster.storage.parameter.AdlsGen2CloudStorageV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.cluster.storage.parameter.S3CloudStorageV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.cluster.storage.parameter.WasbCloudStorageV1Parameters;

@Component
public class CloudStorageConverter {

    public S3CloudStorageV4Parameters convert(S3CloudStorageV1Parameters source) {
        S3CloudStorageV4Parameters response = new S3CloudStorageV4Parameters();
        ifNotNull(source.getInstanceProfile(), response::setInstanceProfile);
        return response;
    }

    public AdlsCloudStorageV4Parameters convert(AdlsCloudStorageV1Parameters source) {
        AdlsCloudStorageV4Parameters response = new AdlsCloudStorageV4Parameters();
        response.setCredential(source.getCredential());
        response.setAccountName(source.getAccountName());
        response.setClientId(source.getClientId());
        response.setTenantId(source.getTenantId());
        return response;
    }

    public AdlsGen2CloudStorageV4Parameters convert(AdlsGen2CloudStorageV1Parameters source) {
        AdlsGen2CloudStorageV4Parameters response = new AdlsGen2CloudStorageV4Parameters();
        response.setAccountKey(source.getAccountKey());
        response.setAccountName(source.getAccountName());
        response.setSecure(source.isSecure());
        return response;
    }

    public WasbCloudStorageV4Parameters convert(WasbCloudStorageV1Parameters source) {
        WasbCloudStorageV4Parameters response = new WasbCloudStorageV4Parameters();
        response.setAccountKey(source.getAccountKey());
        response.setAccountName(source.getAccountName());
        response.setSecure(source.isSecure());
        return response;
    }
}
