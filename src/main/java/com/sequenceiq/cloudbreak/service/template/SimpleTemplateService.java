package com.sequenceiq.cloudbreak.service.template;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.NotFoundException;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.domain.UserRole;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.repository.TemplateRepository;
import com.sequenceiq.cloudbreak.repository.UserRepository;
import com.sequenceiq.cloudbreak.service.company.CompanyService;
import com.sequenceiq.cloudbreak.service.credential.SimpleCredentialService;
import com.sequenceiq.cloudbreak.service.credential.azure.AzureCertificateService;

@Service
public class SimpleTemplateService implements TemplateService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleCredentialService.class);

    private static final String CLOUD_PLATFORM_NOT_SUPPORTED_MSG = "The cloudPlatform '%s' is not supported.";

    private static final String TEMPLATE_NOT_FOUND_MSG = "Template '%s' not found.";

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private StackRepository stackRepository;

    @Autowired
    private AzureCertificateService azureCertificateService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyService companyService;

    @Override
    public Set<Template> getAll(User user) {
        Set<Template> userTemplates = new HashSet<>();
        Set<Template> legacyTemplates = new HashSet<>();

        userTemplates.addAll(user.getAwsTemplates());
        userTemplates.addAll(user.getAzureTemplates());
        LOGGER.debug("User credentials: #{}", userTemplates.size());

        if (user.getUserRoles().contains(UserRole.COMPANY_ADMIN)) {
            LOGGER.debug("Getting company user templates for company admin; id: [{}]", user.getId());
            legacyTemplates = getCompanyUserTemplates(user);
        } else {
            LOGGER.debug("Getting company templates for company user; id: [{}]", user.getId());
            legacyTemplates = getCompanyTemplates(user);
        }
        LOGGER.debug("Found #{} legacy templates for user [{}]", legacyTemplates.size(), user.getId());
        userTemplates.addAll(legacyTemplates);
        return userTemplates;
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
    public Template create(User user, Template template) {
        LOGGER.debug("Creating template for user: [{}]", user.getId());
        template.setUser(user);
        template = templateRepository.save(template);
        return template;
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
                    "There are stacks associated with template '%s'. Please remove these before the deleting the template.", templateId));
        }
    }

    private Set<Template> getCompanyTemplates(User user) {
        Set<Template> companyTemplates = new HashSet<>();
        User adminWithFilteredData = companyService.companyUserData(user.getCompany().getId(), user.getUserRoles().iterator().next());
        if (adminWithFilteredData != null) {
            companyTemplates.addAll(adminWithFilteredData.getAwsTemplates());
            companyTemplates.addAll(adminWithFilteredData.getAzureTemplates());
        } else {
            LOGGER.debug("There's no company admin for user: [{}]", user.getId());
        }
        return companyTemplates;
    }

    private Set<Template> getCompanyUserTemplates(User user) {
        Set<Template> companyUserTemplates = new HashSet<>();
        Set<User> companyUsers = companyService.companyUsers(user.getCompany().getId());
        companyUsers.remove(user);
        for (User cUser : companyUsers) {
            LOGGER.debug("Adding templates of company user: [{}]", cUser.getId());
            companyUserTemplates.addAll(cUser.getAwsTemplates());
            companyUserTemplates.addAll(cUser.getAzureTemplates());
        }
        return companyUserTemplates;
    }
}
