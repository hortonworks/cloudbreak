package com.sequenceiq.environment.parameters.dao.domain;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("YARN")
public class YarnParameters extends BaseParameters {

}
