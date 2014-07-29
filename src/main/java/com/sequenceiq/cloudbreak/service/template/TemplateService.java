package com.sequenceiq.cloudbreak.service.template;

import java.util.Set;

import com.sequenceiq.cloudbreak.controller.json.IdJson;
import com.sequenceiq.cloudbreak.controller.json.TemplateJson;
import com.sequenceiq.cloudbreak.domain.User;

public interface TemplateService {

    Set<TemplateJson> getAll(User user);

    Set<TemplateJson> getAllForAdmin(User user);

    TemplateJson get(Long id);

    IdJson create(User user, TemplateJson templateRequest);

    void delete(Long id);

}
