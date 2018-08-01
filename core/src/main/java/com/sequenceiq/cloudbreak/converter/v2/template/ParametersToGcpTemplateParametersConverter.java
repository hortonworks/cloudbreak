package com.sequenceiq.cloudbreak.converter.v2.template;

import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.v2.template.EncryptionType;
import com.sequenceiq.cloudbreak.api.model.v2.template.GcpEncryption;
import com.sequenceiq.cloudbreak.api.model.v2.template.GcpParameters;
import com.sequenceiq.cloudbreak.api.model.v2.template.KeyEncryptionMethod;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class ParametersToGcpTemplateParametersConverter extends AbstractConversionServiceAwareConverter<Map<String, Object>, GcpParameters> {

    private static final String ENCRYPTION_KEY_FIELD = "key";

    private static final String ENCRYPTION_TYPE_FIELD = "type";

    private static final String KEY_ENCRYPTION_METHOD_FIELD = "keyEncryptionMethod";

    @Override
    public GcpParameters convert(Map<String, Object> source) {
        GcpParameters gcpParameters = new GcpParameters();
        Object key = source.get(ENCRYPTION_KEY_FIELD);
        Object type = source.getOrDefault(ENCRYPTION_TYPE_FIELD, EncryptionType.DEFAULT.name());
        if (Objects.isNull(key) || !EncryptionType.valueOf(type.toString()).equals(EncryptionType.CUSTOM)) {
            GcpEncryption encryption = new GcpEncryption();
            encryption.setType(EncryptionType.DEFAULT.name());
            gcpParameters.setEncryption(encryption);
        } else {
            GcpEncryption encryption = new GcpEncryption();
            encryption.setType(EncryptionType.CUSTOM.name());
            Object keyEncryptionMethod = source.getOrDefault(KEY_ENCRYPTION_METHOD_FIELD, KeyEncryptionMethod.RSA.name());
            encryption.setKeyEncryptionMethod(KeyEncryptionMethod.valueOf(keyEncryptionMethod.toString()).name());
            encryption.setKey(key.toString());
            gcpParameters.setEncryption(encryption);
        }
        return gcpParameters;
    }
}
