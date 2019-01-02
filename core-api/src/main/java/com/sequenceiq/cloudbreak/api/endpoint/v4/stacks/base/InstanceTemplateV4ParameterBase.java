package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.MappableBase;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class InstanceTemplateV4ParameterBase extends MappableBase implements JsonEntity {
}
