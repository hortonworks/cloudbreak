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
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Template;
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
        return templateRepository.findForUser(user.getUsername());
    }

    @Override
    public Set<Template> retrieveAccountTemplates(CbUser user) {
        Set<Template> templates = new HashSet<>();
        if (user.getRoles().contains("admin")) {
            templates = templateRepository.findAllInAccount(user.getAccount());
        } else {
            templates = templateRepository.findPublicsInAccount(user.getAccount());
        }
        return templates;
    }

    @Override
    public Template get(Long id) {
        Template template = templateRepository.findOne(id);
        if (template == null) {
            throw new NotFoundException(String.format(TEMPLATE_NOT_FOUND_MSG, id));
        } else {
            return template;
        }
    }

    @Override
    public Template create(CbUser user, Template template) {
        LOGGER.debug("Creating template: [User: '{}', Account: '{}']", user.getUsername(), user.getAccount());
        Template savedTemplate = null;
        template.setOwner(user.getUsername());
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
        LOGGER.debug("Deleting template : [{}]", templateId);
        Template template = templateRepository.findOne(templateId);
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

    // private Set<Template> getCompanyTemplates(User user) {
    // Set<Template> companyTemplates = new HashSet<>();
    // User adminWithFilteredData =
    // accountService.accountUserData(user.getAccount().getId(),
    // user.getUserRoles().iterator().next());
    // if (adminWithFilteredData != null) {
    // companyTemplates.addAll(adminWithFilteredData.getAwsTemplates());
    // companyTemplates.addAll(adminWithFilteredData.getAzureTemplates());
    // } else {
    // LOGGER.debug("There's no company admin for user: [{}]", user.getId());
    // }
    // return companyTemplates;
    // }
    //
    // private Set<Template> getCompanyUserTemplates(User user) {
    // Set<Template> companyUserTemplates = new HashSet<>();
    // Set<User> companyUsers =
    // accountService.accountUsers(user.getAccount().getId());
    // companyUsers.remove(user);
    // for (User cUser : companyUsers) {
    // LOGGER.debug("Adding templates of company user: [{}]", cUser.getId());
    // companyUserTemplates.addAll(cUser.getAwsTemplates());
    // companyUserTemplates.addAll(cUser.getAzureTemplates());
    // }
    // return companyUserTemplates;
    // }
}
