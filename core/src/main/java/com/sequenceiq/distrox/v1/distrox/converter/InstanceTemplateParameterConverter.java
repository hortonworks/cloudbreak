package com.sequenceiq.distrox.v1.distrox.converter;

import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AwsEncryptionV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AwsInstanceTemplateV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AzureInstanceTemplateV4Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.AwsEncryptionV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.AwsInstanceTemplateV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.AzureInstanceTemplateV1Parameters;

@Component
public class InstanceTemplateParameterConverter {

    public AwsInstanceTemplateV4Parameters convert(AwsInstanceTemplateV1Parameters source) {
        AwsInstanceTemplateV4Parameters response = new AwsInstanceTemplateV4Parameters();
        response.setEncryption(getIfNotNull(source.getEncryption(), this::convert));
        response.setSpotPrice(source.getSpotPrice());
        return response;
    }

    private AwsEncryptionV4Parameters convert(AwsEncryptionV1Parameters source) {
        AwsEncryptionV4Parameters response = new AwsEncryptionV4Parameters();
        response.setKey(source.getKey());
        response.setType(source.getType());
        return response;
    }

    public AzureInstanceTemplateV4Parameters convert(AzureInstanceTemplateV1Parameters source) {
        AzureInstanceTemplateV4Parameters response = new AzureInstanceTemplateV4Parameters();
        response.setEncrypted(source.getEncrypted());
        response.setManagedDisk(source.getManagedDisk());
        response.setPrivateId(source.getPrivateId());
        return response;
    }

    public AwsInstanceTemplateV1Parameters convert(AwsInstanceTemplateV4Parameters source) {
        AwsInstanceTemplateV1Parameters response = new AwsInstanceTemplateV1Parameters();
        response.setEncryption(getIfNotNull(source.getEncryption(), this::convert));
        response.setSpotPrice(source.getSpotPrice());
        return response;
    }

    private AwsEncryptionV1Parameters convert(AwsEncryptionV4Parameters source) {
        AwsEncryptionV1Parameters response = new AwsEncryptionV1Parameters();
        response.setKey(source.getKey());
        response.setType(source.getType());
        return response;
    }

    public AzureInstanceTemplateV1Parameters convert(AzureInstanceTemplateV4Parameters source) {
        AzureInstanceTemplateV1Parameters response = new AzureInstanceTemplateV1Parameters();
        response.setEncrypted(source.getEncrypted());
        response.setManagedDisk(source.getManagedDisk());
        response.setPrivateId(source.getPrivateId());
        return response;
    }
}
