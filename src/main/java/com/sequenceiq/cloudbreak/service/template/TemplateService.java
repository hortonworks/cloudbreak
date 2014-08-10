package com.sequenceiq.cloudbreak.service.template;

import java.util.Set;

import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.User;

public interface TemplateService {

    Set<Template> getAll(User user);

    Template get(Long id);

    Template create(User user, Template templateRequest);

    void delete(Long id);

}
