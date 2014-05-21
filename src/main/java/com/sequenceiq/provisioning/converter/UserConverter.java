package com.sequenceiq.provisioning.converter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.provisioning.controller.json.UserJson;
import com.sequenceiq.provisioning.domain.User;

@Component
public class UserConverter extends AbstractConverter<UserJson, User> {

    @Autowired
    private AzureInfraConverter azureInfraConverter;

    @Autowired
    private AwsInfraConverter awsInfraConverter;

    @Autowired
    private CloudInstanceConverter cloudInstanceConverter;

    @Override
    public UserJson convert(User entity) {
        UserJson userJson = new UserJson();
        userJson.setEmail(entity.getEmail());
        userJson.setFirstName(entity.getFirstName());
        userJson.setLastName(entity.getLastName());
        userJson.setRoleArn(entity.getRoleArn() == null ? "" : entity.getRoleArn());
        userJson.setJks(entity.getJks() == null ? "" : entity.getJks());
        userJson.setSubscriptionId(entity.getSubscriptionId() == null ? "" : entity.getSubscriptionId());
        userJson.setAwsInfras(awsInfraConverter.convertAllEntityToJson(entity.getAwsInfras()));
        userJson.setAzureInfras(azureInfraConverter.convertAllEntityToJson(entity.getAzureInfras()));
        userJson.setCloudInstances(cloudInstanceConverter.convertAllEntityToJson(entity.getCloudInstances()));
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
        user.setAwsInfras(awsInfraConverter.convertAllJsonToEntity(json.getAwsInfras()));
        user.setAzureInfras(azureInfraConverter.convertAllJsonToEntity(json.getAzureInfras()));
        user.setCloudInstances(cloudInstanceConverter.convertAllJsonToEntity(json.getCloudInstances()));
        return user;
    }
}
