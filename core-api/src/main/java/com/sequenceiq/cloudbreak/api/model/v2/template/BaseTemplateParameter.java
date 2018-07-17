package com.sequenceiq.cloudbreak.api.model.v2.template;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.model.JsonEntity;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class BaseTemplateParameter implements JsonEntity {

    public static final String PLATFORM_TYPE = "platformType";

}
