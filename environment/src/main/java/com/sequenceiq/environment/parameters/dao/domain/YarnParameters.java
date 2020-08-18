package com.sequenceiq.environment.parameters.dao.domain;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("YARN")
public class YarnParameters extends BaseParameters {

}
