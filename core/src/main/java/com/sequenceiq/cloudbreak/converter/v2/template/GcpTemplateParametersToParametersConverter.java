package com.sequenceiq.cloudbreak.converter.v2.template;

import static com.sequenceiq.cloudbreak.api.model.v2.template.BaseTemplateParameter.PLATFORM_TYPE;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.v2.template.GcpEncryption;
import com.sequenceiq.cloudbreak.api.model.v2.template.GcpEncryptionType;
import com.sequenceiq.cloudbreak.api.model.v2.template.GcpParameters;
import com.sequenceiq.cloudbreak.api.model.v2.template.KeyEncryptionMethod;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class GcpTemplateParametersToParametersConverter extends AbstractConversionServiceAwareConverter<GcpParameters, Map<String, Object>> {

    private static final String ENCRYPTION_KEY_FIELD = "key";

    private static final String ENCRYPTION_TYPE_FIELD = "type";

    private static final String KEY_ENCRYPTION_METHOD_FIELD = "keyEncryptionMethod";

    @Override
    public Map<String, Object> convert(GcpParameters source) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(PLATFORM_TYPE, CloudConstants.GCP);

        GcpEncryption encryption = Optional.ofNullable(source.getEncryption()).orElseGet(() -> {
            GcpEncryption gcpEncryption = new GcpEncryption();
            gcpEncryption.setType(GcpEncryptionType.DEFAULT.name());
            return gcpEncryption;
        });

        if (GcpEncryptionType.DEFAULT.name().equals(encryption.getType())) {
            parameters.put(ENCRYPTION_TYPE_FIELD, GcpEncryptionType.DEFAULT);
        } else {
            parameters.put(ENCRYPTION_TYPE_FIELD, GcpEncryptionType.valueOf(encryption.getType()));
            parameters.put(ENCRYPTION_KEY_FIELD, encryption.getKey());
            parameters.put(KEY_ENCRYPTION_METHOD_FIELD, KeyEncryptionMethod.valueOf(encryption.getKeyEncryptionMethod()));
        }

        return parameters;
    }
}
