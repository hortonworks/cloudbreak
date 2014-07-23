package com.sequenceiq.cloudbreak.converter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.json.UserJson;
import com.sequenceiq.cloudbreak.domain.User;
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
        return user;
    }
}
