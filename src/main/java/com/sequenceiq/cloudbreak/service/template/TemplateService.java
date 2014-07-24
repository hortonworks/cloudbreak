package com.sequenceiq.cloudbreak.service.template;

import com.sequenceiq.cloudbreak.controller.json.IdJson;
import com.sequenceiq.cloudbreak.controller.json.TemplateJson;
import com.sequenceiq.cloudbreak.domain.User;

import java.util.Set;

public interface TemplateService {

    Set<TemplateJson> getAll(User user);

    Set<TemplateJson> getAllForAdmin(User user);

    TemplateJson get(Long id);

    IdJson create(User user, TemplateJson templateRequest);

    void delete(Long id);

}
