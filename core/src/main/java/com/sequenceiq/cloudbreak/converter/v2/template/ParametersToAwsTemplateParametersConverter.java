package com.sequenceiq.cloudbreak.converter.v2.template;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.v2.template.AwsParameters;
import com.sequenceiq.cloudbreak.api.model.v2.template.AwsEncryption;
import com.sequenceiq.cloudbreak.api.model.v2.template.EncryptionType;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class ParametersToAwsTemplateParametersConverter extends AbstractConversionServiceAwareConverter<Map<String, Object>, AwsParameters> {

    @Override
    public AwsParameters convert(Map<String, Object> source) {
        AwsParameters awsParameters = new AwsParameters();
        if (source.get("encrypted") != null) {
            if (Boolean.valueOf(source.get("encrypted").toString())) {
                Object key = source.get("key");
                if (key == null) {
                    AwsEncryption awsEncryption = new AwsEncryption();
                    awsEncryption.setType(EncryptionType.DEFAULT.name());
                    awsParameters.setEncryption(awsEncryption);
                } else {
                    AwsEncryption awsEncryption = new AwsEncryption();
                    awsEncryption.setType(EncryptionType.CUSTOM.name());
                    awsEncryption.setKey(key.toString());
                    awsParameters.setEncryption(awsEncryption);
                }
                awsParameters.setEncrypted(true);
            } else {
                AwsEncryption awsEncryption = new AwsEncryption();
                awsEncryption.setType(EncryptionType.NONE.name());
                awsParameters.setEncryption(awsEncryption);
                awsParameters.setEncrypted(false);
            }
        } else {
            awsParameters.setEncrypted(false);
        }
        if (source.get("spotPrice") != null) {
            awsParameters.setSpotPrice(Double.valueOf(source.get("spotPrice").toString()));
        }
        return awsParameters;
    }
}
