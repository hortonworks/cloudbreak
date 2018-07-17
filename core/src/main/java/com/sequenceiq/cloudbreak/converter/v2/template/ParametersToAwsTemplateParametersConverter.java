package com.sequenceiq.cloudbreak.converter.v2.template;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.v2.template.AwsParameters;
import com.sequenceiq.cloudbreak.api.model.v2.template.Encryption;
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
                    Encryption encryption = new Encryption();
                    encryption.setType(EncryptionType.DEFAULT.name());
                    awsParameters.setEncryption(encryption);
                    awsParameters.setEncrypted(true);
                } else {
                    Encryption encryption = new Encryption();
                    encryption.setType(EncryptionType.CUSTOM.name());
                    encryption.setKey(key.toString());
                    awsParameters.setEncryption(encryption);
                    awsParameters.setEncrypted(true);
                }
            } else {
                Encryption encryption = new Encryption();
                encryption.setType(EncryptionType.NONE.name());
                awsParameters.setEncryption(encryption);
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
