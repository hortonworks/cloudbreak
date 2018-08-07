package com.sequenceiq.cloudbreak.converter.v2.template;

import static com.sequenceiq.cloudbreak.api.model.v2.template.BaseTemplateParameter.PLATFORM_TYPE;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.v2.template.AwsParameters;
import com.sequenceiq.cloudbreak.api.model.v2.template.EncryptionType;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class AwsTemplateParametersToParametersConverter extends AbstractConversionServiceAwareConverter<AwsParameters, Map<String, Object>> {

    @Override
    public Map<String, Object> convert(AwsParameters source) {
        Map<String, Object> parameters = new HashMap<>();
        if (source.isEncrypted() != null || source.getEncryption() != null) {
            if (source.getEncryption() != null) {
                if (source.getEncryption() == null) {
                    parameters.put("type", EncryptionType.DEFAULT);
                    parameters.put("encrypted", true);
                } else {
                    if (source.getEncryption().getKey() != null) {
                        parameters.put("type", EncryptionType.CUSTOM);
                        parameters.put("key", source.getEncryption().getKey());
                        parameters.put("encrypted", true);
                    } else {
                        EncryptionType encryptionType = EncryptionType.valueOf(source.getEncryption().getType());
                        parameters.put("type", encryptionType);
                        if (EncryptionType.NONE.equals(encryptionType)) {
                            parameters.put("encrypted", false);
                        } else {
                            parameters.put("encrypted", true);
                        }
                    }
                }
            } else if (source.isEncrypted()) {
                parameters.put("type", EncryptionType.DEFAULT);
                parameters.put("encrypted", true);
            } else {
                parameters.put("type", EncryptionType.NONE);
                parameters.put("encrypted", false);
            }
        } else if (source.isEncrypted() == null) {
            parameters.put("type", EncryptionType.NONE);
            parameters.put("encrypted", false);
        }
        if (source.getSpotPrice() != null) {
            parameters.put("spotPrice", source.getSpotPrice());
        }
        parameters.put(PLATFORM_TYPE, CloudConstants.AWS);
        return parameters;
    }
}
