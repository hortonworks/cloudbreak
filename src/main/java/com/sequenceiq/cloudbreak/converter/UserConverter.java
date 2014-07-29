package com.sequenceiq.cloudbreak.converter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.json.UserJson;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.domain.UserRole;
import com.sequenceiq.cloudbreak.repository.CompanyRepository;
import com.sequenceiq.cloudbreak.service.credential.CredentialService;

@Component
public class UserConverter extends AbstractConverter<UserJson, User> {

    @Autowired
    private AzureTemplateConverter azureTemplateConverter;

    @Autowired
    private AwsTemplateConverter awsTemplateConverter;

    @Autowired
    private StackConverter stackConverter;

    @Autowired
    private BlueprintConverter blueprintConverter;

    @Autowired
    private CredentialService credentialService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CompanyConverter companyConverter;

    @Autowired
    private CompanyRepository companyRepository;

    @Override
    public UserJson convert(User entity) {
        UserJson userJson = new UserJson();
        userJson.setEmail(entity.getEmail());
        userJson.setFirstName(entity.getFirstName());
        userJson.setLastName(entity.getLastName());
        userJson.setCredentials(credentialService.getAll(entity));
        userJson.setAwsTemplates(awsTemplateConverter.convertAllEntityToJson(entity.getAwsTemplates()));
        userJson.setAzureTemplates(azureTemplateConverter.convertAllEntityToJson(entity.getAzureTemplates()));
        userJson.setStacks(stackConverter.convertAllEntityToJsonWithClause(entity.getStacks()));
        userJson.setBlueprints(blueprintConverter.convertAllToIdList(entity.getBlueprints()));
        userJson.setCompany(entity.getCompany().getName());
        return userJson;
    }

    @Override
    public User convert(UserJson json) {
        User user = new User();
        user.setEmail(json.getEmail());
        user.setFirstName(json.getFirstName());
        user.setLastName(json.getLastName());
        user.setAwsTemplates(awsTemplateConverter.convertAllJsonToEntity(json.getAwsTemplates()));
        user.setAzureTemplates(azureTemplateConverter.convertAllJsonToEntity(json.getAzureTemplates()));
        user.setStacks(stackConverter.convertAllJsonToEntity(json.getStacks()));
        user.setPassword(passwordEncoder.encode(json.getPassword()));
        user.setCompany(companyRepository.findByName(json.getCompany()));

        switch (json.getUserType()) {
            case DEFAULT:
                break;
            case COMPANY_USER:
                break;
            case COMPANY_ADMIN:
                user.getUserRole().add(UserRole.COMPANY_ADMIN);
                break;
            default:
                throw new BadRequestException("Unsupported user type.");
        }
        user.getUserRole().add(UserRole.COMPANY_USER);
        return user;
    }
}
