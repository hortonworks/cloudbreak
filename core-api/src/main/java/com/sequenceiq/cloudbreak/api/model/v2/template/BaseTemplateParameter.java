package com.sequenceiq.cloudbreak.api.model.v2.template;

import static com.sequenceiq.cloudbreak.api.model.v2.template.TemplatePlatformType.EMPTY;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.model.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.TemplateModelDescription;

import io.swagger.annotations.ApiModelProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class BaseTemplateParameter implements JsonEntity {

    public static final String PLATFORM_TYPE = "platformType";

    @ApiModelProperty(TemplateModelDescription.ENCRYPTION)
    private Encryption encryption;

    @ApiModelProperty(TemplateModelDescription.ENCRYPTED)
    private Boolean encrypted;

    public Map<String, Object> asMap() {
        Map<String, Object> ret = new HashMap<>();
        if (encryption != null) {
            if (encryption.getKey() == null) {
                ret.put("type", EncryptionType.DEFAULT);
            } else {
                ret.put("key", encryption.getKey());
                if (encryption.getType() == null) {
                    ret.put("type", EncryptionType.CUSTOM);
                } else {
                    ret.put("type", EncryptionType.valueOf(encryption.getType()));
                }
            }
        }
        if (encrypted != null) {
            ret.put("encrypted", encrypted);
        }
        if (type() != null) {
            ret.put(PLATFORM_TYPE, type());
        }
        ret.putAll(getCustomParameter());
        return ret;
    }

    public Encryption getEncryption() {
        return encryption;
    }

    public void setEncryption(Encryption encryption) {
        this.encryption = encryption;
    }

    public Boolean isEncrypted() {
        return encrypted;
    }

    public void setEncrypted(Boolean encrypted) {
        this.encrypted = encrypted;
    }

    public TemplatePlatformType type() {
        return EMPTY;
    }

    private Map<String, Object> getCustomParameter() {
        return Arrays.stream(getClass().getDeclaredFields())
                .filter(f -> Objects.nonNull(getObject(f)))
                .collect(Collectors.toMap(Field::getName, this::getObject));
    }

    private Object getObject(Field f) {
        try {
            f.setAccessible(true);
            return f.get(this);
        } catch (IllegalAccessException e) {
            return null;
        }
    }
}
