package com.sequenceiq.distrox.v1.distrox.converter;

import static com.sequenceiq.cloudbreak.util.NullUtil.doIfNotNull;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.storage.AdlsCloudStorageParameters;
import com.sequenceiq.cloudbreak.cloud.model.storage.AdlsGen2CloudStorageParameters;
import com.sequenceiq.cloudbreak.cloud.model.storage.S3CloudStorageParameters;
import com.sequenceiq.cloudbreak.cloud.model.storage.WasbCloudStorageParameters;
import com.sequenceiq.distrox.api.v1.distrox.model.cluster.storage.parameter.AdlsCloudStorageV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.cluster.storage.parameter.AdlsGen2CloudStorageV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.cluster.storage.parameter.S3CloudStorageV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.cluster.storage.parameter.WasbCloudStorageV1Parameters;

@Component
public class CloudStorageConverter {

    public S3CloudStorageParameters convert(S3CloudStorageV1Parameters source) {
        S3CloudStorageParameters response = new S3CloudStorageParameters();
        doIfNotNull(source.getInstanceProfile(), response::setInstanceProfile);
        return response;
    }

    public AdlsCloudStorageParameters convert(AdlsCloudStorageV1Parameters source) {
        AdlsCloudStorageParameters response = new AdlsCloudStorageParameters();
        response.setCredential(source.getCredential());
        response.setAccountName(source.getAccountName());
        response.setClientId(source.getClientId());
        response.setTenantId(source.getTenantId());
        return response;
    }

    public AdlsGen2CloudStorageParameters convert(AdlsGen2CloudStorageV1Parameters source) {
        AdlsGen2CloudStorageParameters response = new AdlsGen2CloudStorageParameters();
        response.setAccountKey(source.getAccountKey());
        response.setAccountName(source.getAccountName());
        response.setSecure(source.isSecure());
        return response;
    }

    public WasbCloudStorageParameters convert(WasbCloudStorageV1Parameters source) {
        WasbCloudStorageParameters response = new WasbCloudStorageParameters();
        response.setAccountKey(source.getAccountKey());
        response.setAccountName(source.getAccountName());
        response.setSecure(source.isSecure());
        return response;
    }

    public S3CloudStorageV1Parameters convert(S3CloudStorageParameters source) {
        S3CloudStorageV1Parameters response = new S3CloudStorageV1Parameters();
        doIfNotNull(source.getInstanceProfile(), response::setInstanceProfile);
        return response;
    }

    public AdlsCloudStorageV1Parameters convert(AdlsCloudStorageParameters source) {
        AdlsCloudStorageV1Parameters response = new AdlsCloudStorageV1Parameters();
        response.setCredential(source.getCredential());
        response.setAccountName(source.getAccountName());
        response.setClientId(source.getClientId());
        response.setTenantId(source.getTenantId());
        return response;
    }

    public AdlsGen2CloudStorageV1Parameters convert(AdlsGen2CloudStorageParameters source) {
        AdlsGen2CloudStorageV1Parameters response = new AdlsGen2CloudStorageV1Parameters();
        response.setAccountKey(source.getAccountKey());
        response.setAccountName(source.getAccountName());
        response.setSecure(source.isSecure());
        return response;
    }

    public WasbCloudStorageV1Parameters convert(WasbCloudStorageParameters source) {
        WasbCloudStorageV1Parameters response = new WasbCloudStorageV1Parameters();
        response.setAccountKey(source.getAccountKey());
        response.setAccountName(source.getAccountName());
        response.setSecure(source.isSecure());
        return response;
    }
}
