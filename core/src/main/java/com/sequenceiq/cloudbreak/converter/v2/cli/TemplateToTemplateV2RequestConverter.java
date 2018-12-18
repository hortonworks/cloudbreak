package com.sequenceiq.cloudbreak.converter.v2.cli;

import static java.util.Optional.ofNullable;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.v2.TemplateV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.template.KeyEncryptionMethod;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.json.Json;

@Component
public class TemplateToTemplateV2RequestConverter extends AbstractConversionServiceAwareConverter<Template, TemplateV2Request> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TemplateToTemplateV2RequestConverter.class);

    private static final String KEY_ENCRYPTION_METHOD_FIELD = "keyEncryptionMethod";

    private static final String KEY_FIELD = "key";

    @Override
    public TemplateV2Request convert(Template source) {
        TemplateV2Request templateV2Request = new TemplateV2Request();

        Map<String, Object> parameters = new HashMap<>();
        ofNullable(source.getAttributes()).ifPresent(attr -> parameters.putAll(attr.getMap()));
        ofNullable(source.getSecretAttributes()).ifPresent(attr -> parameters.putAll(new Json(attr).getMap()));
        if (parameters.containsKey(KEY_ENCRYPTION_METHOD_FIELD)
                && !KeyEncryptionMethod.KMS.name().equalsIgnoreCase((String) parameters.get(KEY_ENCRYPTION_METHOD_FIELD))) {
            parameters.remove(KEY_FIELD);
        }

        templateV2Request.setParameters(parameters);
        templateV2Request.setInstanceType(source.getInstanceType());
        templateV2Request.setVolumeCount(source.getVolumeCount());
        templateV2Request.setVolumeSize(source.getVolumeSize());
        templateV2Request.setVolumeType(source.getVolumeType());
        templateV2Request.setRootVolumeSize(source.getRootVolumeSize());
        return templateV2Request;
    }

}
