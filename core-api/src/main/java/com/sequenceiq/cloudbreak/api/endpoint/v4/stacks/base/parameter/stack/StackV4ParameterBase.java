package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.common.mappable.MappableBase;
import com.sequenceiq.common.model.JsonEntity;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class StackV4ParameterBase extends MappableBase implements JsonEntity {

}
