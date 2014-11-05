package com.sequenceiq.cloudbreak.service.template;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.NotFoundException;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.CbUserRole;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.logger.CbLoggerFactory;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.repository.TemplateRepository;
import com.sequenceiq.cloudbreak.service.DuplicateKeyValueException;
import com.sequenceiq.cloudbreak.service.credential.SimpleCredentialService;

@Service
public class SimpleTemplateService implements TemplateService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleCredentialService.class);

    private static final String TEMPLATE_NOT_FOUND_MSG = "Template '%s' not found.";

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private StackRepository stackRepository;

    @Override
    public Set<Template> retrievePrivateTemplates(CbUser user) {
        return templateRepository.findForUser(user.getUserId());
    }

    @Override
    public Set<Template> retrieveAccountTemplates(CbUser user) {
        Set<Template> templates = new HashSet<>();
        if (user.getRoles().contains(CbUserRole.ADMIN)) {
            templates = templateRepository.findAllInAccount(user.getAccount());
        } else {
            templates = templateRepository.findPublicsInAccount(user.getAccount());
        }
        return templates;
    }

    @Override
    public Template get(Long id) {
        Template template = templateRepository.findOne(id);
        CbLoggerFactory.buildMdvContext(template);
        if (template == null) {
            throw new NotFoundException(String.format(TEMPLATE_NOT_FOUND_MSG, id));
        } else {
            return template;
        }
    }

    @Override
    public Template create(CbUser user, Template template) {
        CbLoggerFactory.buildMdvContext(template);
        LOGGER.debug("Creating template: [User: '{}', Account: '{}']", user.getUsername(), user.getAccount());
        Template savedTemplate = null;
        template.setOwner(user.getUserId());
        template.setAccount(user.getAccount());
        try {
            savedTemplate = templateRepository.save(template);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateKeyValueException(template.getName(), ex);
        }
        return savedTemplate;
    }

    @Override
    public void delete(Long templateId) {
        Template template = templateRepository.findOne(templateId);
        CbLoggerFactory.buildMdvContext(template);
        LOGGER.debug("Deleting template : [{}]", templateId);
        if (template == null) {
            throw new NotFoundException(String.format(TEMPLATE_NOT_FOUND_MSG, templateId));
        }
        List<Stack> allStackForTemplate = stackRepository.findAllStackForTemplate(templateId);
        if (allStackForTemplate.isEmpty()) {
            templateRepository.delete(template);
        } else {
            throw new BadRequestException(String.format(
                    "There are stacks associated with template '%s'. Please remove these before deleting the template.", templateId));
        }
    }

}
