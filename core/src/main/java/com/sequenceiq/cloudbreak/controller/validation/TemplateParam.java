package com.sequenceiq.cloudbreak.controller.validation;

import com.google.common.base.Optional;

public interface TemplateParam {

    String getName();

    Class getClazz();

    Boolean getRequired();

    Optional<String> getRegex();
}
