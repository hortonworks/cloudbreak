package com.sequenceiq.provisioning.converter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.provisioning.controller.json.UserJson;
import com.sequenceiq.provisioning.domain.User;

@Component
public class UserConverter extends AbstractConverter<UserJson, User> {

    @Autowired
    private AzureTemplateConverter azureTemplateConverter;

    @Autowired
    private AwsTemplateConverter awsTemplateConverter;

    @Autowired
    private StackConverter stackConverter;

    @Override
    public UserJson convert(User entity) {
        UserJson userJson = new UserJson();
        userJson.setEmail(entity.getEmail());
        userJson.setFirstName(entity.getFirstName());
        userJson.setLastName(entity.getLastName());
        userJson.setRoleArn(entity.getRoleArn() == null ? "" : entity.getRoleArn());
        userJson.setJks(entity.getJks() == null ? "" : entity.getJks());
        userJson.setSubscriptionId(entity.getSubscriptionId() == null ? "" : entity.getSubscriptionId());
        userJson.setAwsTemplates(awsTemplateConverter.convertAllEntityToJson(entity.getAwsTemplates()));
        userJson.setAzureTemplates(azureTemplateConverter.convertAllEntityToJson(entity.getAzureTemplates()));
        userJson.setStacks(stackConverter.convertAllEntityToJson(entity.getStacks()));
        return userJson;
    }

    @Override
    public User convert(UserJson json) {
        User user = new User();
        user.setEmail(json.getEmail());
        user.setFirstName(json.getFirstName());
        user.setLastName(json.getLastName());
        user.setRoleArn(json.getRoleArn());
        user.setJks(json.getJks());
        user.setSubscriptionId(json.getSubscriptionId());
        user.setAwsTemplates(awsTemplateConverter.convertAllJsonToEntity(json.getAwsTemplates()));
        user.setAzureTemplates(azureTemplateConverter.convertAllJsonToEntity(json.getAzureTemplates()));
        user.setStacks(stackConverter.convertAllJsonToEntity(json.getStacks()));
        return user;
    }
}
