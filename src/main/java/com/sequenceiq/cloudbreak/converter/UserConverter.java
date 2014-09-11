package com.sequenceiq.cloudbreak.converter;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.json.CredentialJson;
import com.sequenceiq.cloudbreak.controller.json.UserJson;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.repository.AccountRepository;

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
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AccountRepository companyRepository;

    @Autowired
    private AwsCredentialConverter awsCredentialConverter;

    @Autowired
    private AzureCredentialConverter azureCredentialConverter;

    @Override
    public UserJson convert(User entity) {
        UserJson userJson = new UserJson();
        userJson.setEmail(entity.getEmail());
        userJson.setFirstName(entity.getFirstName());
        userJson.setLastName(entity.getLastName());
        userJson.setCredentials(convertCredentials(entity));
        userJson.setStacks(stackConverter.convertAllEntityToJsonWithClause(entity.getStacks()));
        userJson.setBlueprints(blueprintConverter.convertAllToIdList(entity.getBlueprints()));
        userJson.setCompany(entity.getAccount().getName());
        userJson.setUserId(entity.getId());
        return userJson;
    }

    private Set<CredentialJson> convertCredentials(User entity) {
        Set<CredentialJson> jsons = new HashSet<>();
        jsons.addAll(awsCredentialConverter.convertAllEntityToJson(entity.getAwsCredentials()));
        jsons.addAll(azureCredentialConverter.convertAllEntityToJson(entity.getAzureCredentials()));
        return jsons;
    }

    @Override
    public User convert(UserJson json) {
        User user = new User();
        user.setEmail(json.getEmail());
        user.setFirstName(json.getFirstName());
        user.setLastName(json.getLastName());
        user.setStacks(stackConverter.convertAllJsonToEntity(json.getStacks()));
        user.setPassword(passwordEncoder.encode(json.getPassword()));
        return user;
    }
}
