package com.sequenceiq.cloudbreak.service.template;

import java.util.Set;

import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.Template;

public interface TemplateService {

    Set<Template> retrievePrivateTemplates(CbUser user);

    Set<Template> retrieveAccountTemplates(CbUser user);

    Template get(Long id);

    Template get(String name, CbUser user);

    Template create(CbUser user, Template templateRequest);

    void delete(Long id);

    void delete(String templateName, CbUser user);
}
