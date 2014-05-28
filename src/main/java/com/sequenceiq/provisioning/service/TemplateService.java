package com.sequenceiq.provisioning.service;

import java.util.Set;

import com.sequenceiq.provisioning.controller.json.IdJson;
import com.sequenceiq.provisioning.controller.json.TemplateJson;
import com.sequenceiq.provisioning.domain.User;

public interface TemplateService {

    Set<TemplateJson> getAll(User user);

    TemplateJson get(Long id);

    IdJson create(User user, TemplateJson templateRequest);

    void delete(Long id);

}
