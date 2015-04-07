package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.json.TemplateJson;
import com.sequenceiq.cloudbreak.controller.validation.AwsTemplateParam;
import com.sequenceiq.cloudbreak.domain.AwsEncryption;
import com.sequenceiq.cloudbreak.domain.AwsInstanceType;
import com.sequenceiq.cloudbreak.domain.AwsTemplate;
import com.sequenceiq.cloudbreak.domain.AwsVolumeType;

@Component
public class JsonToAwsTemplateConverter extends AbstractConversionServiceAwareConverter<TemplateJson, AwsTemplate> {
    private static final String DEFAULT_SSH_LOCATION = "0.0.0.0/0";

    @Override
    public AwsTemplate convert(TemplateJson source) {
        AwsTemplate awsTemplate = new AwsTemplate();
        awsTemplate.setName(source.getName());
        awsTemplate.setInstanceType(AwsInstanceType.valueOf(String.valueOf(source.getParameters().get(AwsTemplateParam.INSTANCE_TYPE.getName()))));
        String sshLocation = source.getParameters().containsKey(AwsTemplateParam.SSH_LOCATION.getName())
                ? String.valueOf(source.getParameters().get(AwsTemplateParam.SSH_LOCATION.getName())) : DEFAULT_SSH_LOCATION;
        awsTemplate.setSshLocation(sshLocation);
        awsTemplate.setDescription(source.getDescription());
        awsTemplate.setVolumeCount(source.getVolumeCount() == null ? 0 : source.getVolumeCount());
        awsTemplate.setVolumeSize(source.getVolumeSize() == null ? 0 : source.getVolumeSize());
        awsTemplate.setVolumeType(AwsVolumeType.valueOf(String.valueOf(source.getParameters().get(AwsTemplateParam.VOLUME_TYPE.getName()))));
        awsTemplate.setEncrypted(source.getParameters().containsKey(AwsTemplateParam.ENCRYPTED.getName())
                && Boolean.valueOf(source.getParameters().get(AwsTemplateParam.ENCRYPTED.getName()).toString())
                ? AwsEncryption.TRUE : AwsEncryption.FALSE);
        awsTemplate.setSpotPrice(source.getParameters().containsKey(AwsTemplateParam.SPOT_PRICE.getName())
                && source.getParameters().get(AwsTemplateParam.SPOT_PRICE.getName()) != null
                ? Double.valueOf(source.getParameters().get(AwsTemplateParam.SPOT_PRICE.getName()).toString()) : null);
        if (awsTemplate.isEncrypted() && awsTemplate.getVolumeType().equals(AwsVolumeType.Ephemeral)) {
            throw new BadRequestException("AwsTemplate can not be both encrypted and ephemeral");
        }
        return awsTemplate;
    }
}
