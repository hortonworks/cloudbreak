package com.sequenceiq.cloudbreak.service.template;

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
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
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
        if (user.getRoles().contains(CbUserRole.ADMIN)) {
            return templateRepository.findAllInAccount(user.getAccount());
        } else {
            return templateRepository.findPublicInAccountForUser(user.getUserId(), user.getAccount());
        }
    }

    @Override
    public Template get(Long id) {
        Template template = templateRepository.findOne(id);
        MDCBuilder.buildMdcContext(template);
        if (template == null) {
            throw new NotFoundException(String.format(TEMPLATE_NOT_FOUND_MSG, id));
        } else {
            return template;
        }
    }

    @Override
    public Template create(CbUser user, Template template) {
        MDCBuilder.buildMdcContext(template);
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
    public void delete(Long templateId, CbUser user) {
        Template template = templateRepository.findByIdInAccount(templateId, user.getAccount());
        if (template == null) {
            throw new NotFoundException(String.format(TEMPLATE_NOT_FOUND_MSG, templateId));
        }
        delete(template, user);
    }

    public Template getPrivateTemplate(String name, CbUser user) {
        Template template = templateRepository.findByNameInUser(name, user.getUserId());
        if (template == null) {
            throw new NotFoundException(String.format(TEMPLATE_NOT_FOUND_MSG, name));
        } else {
            MDCBuilder.buildMdcContext(template);
            return template;
        }
    }

    public Template getPublicTemplate(String name, CbUser user) {
        Template template = templateRepository.findOneByName(name, user.getAccount());
        if (template == null) {
            throw new NotFoundException(String.format(TEMPLATE_NOT_FOUND_MSG, name));
        } else {
            MDCBuilder.buildMdcContext(template);
            return template;
        }
    }

    @Override
    public void delete(String templateName, CbUser user) {
        Template template = templateRepository.findByNameInAccount(templateName, user.getAccount(), user.getUserId());
        if (template == null) {
            throw new NotFoundException(String.format(TEMPLATE_NOT_FOUND_MSG, templateName));
        }
        delete(template, user);
    }

    private void delete(Template template, CbUser user) {
        MDCBuilder.buildMdcContext(template);
        LOGGER.debug("Deleting template. {} - {}", new Object[]{template.getId(), template.getName()});
        List<Stack> allStackForTemplate = stackRepository.findAllStackForTemplate(template.getId());
        if (allStackForTemplate.isEmpty()) {
            if (!user.getUserId().equals(template.getOwner()) && !user.getRoles().contains(CbUserRole.ADMIN)) {
                throw new BadRequestException("Templates can be deleted only by account admins or owners.");
            }
            templateRepository.delete(template);
        } else {
            throw new BadRequestException(String.format(
                    "There are stacks associated with template '%s'. Please remove these before deleting the template.", template.getName()));
        }
    }

}
