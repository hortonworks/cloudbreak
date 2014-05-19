package com.sequenceiq.provisioning.converter;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.provisioning.domain.User;
import com.sequenceiq.provisioning.json.UserJson;

@Component
public class UserConverter extends AbstractConverter<UserJson, User> {

    @Autowired
    private AzureInfraConverter azureInfraConverter;

    @Autowired
    private AwsInfraConverter awsInfraConverter;

    @Autowired
    private AzureCloudInstanceConverter azureCloudInstanceConverter;

    @Autowired
    private AwsCloudInstanceConverter awsCloudInstanceConverter;

    @Override
    public UserJson convert(User entity) {
        UserJson userJson = new UserJson();
        userJson.setEmail(entity.getEmail());
        userJson.setFirstName(entity.getFirstName());
        userJson.setLastName(entity.getLastName());
        userJson.setRoleArn(entity.getRoleArn() == null ? "" : entity.getRoleArn());
        userJson.setJks(entity.getJks() == null ? "" : entity.getJks());
        userJson.setSubscriptionId(entity.getSubscriptionId() == null ? "" : entity.getSubscriptionId());
        userJson.setAwsInfraList(awsInfraConverter.convertAllEntityToJson(entity.getAwsInfraList()));
        userJson.setAzureInfraList(azureInfraConverter.convertAllEntityToJson(entity.getAzureInfraList()));
        userJson.setAzureCloudList(azureCloudInstanceConverter.convertAllEntityToJson(entity.getAzureCloudInstanceList()));
        userJson.setAwsCloudList(awsCloudInstanceConverter.convertAllEntityToJson(entity.getAwsCloudInstanceList()));
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
        user.setAwsInfraList(awsInfraConverter.convertAllJsonToEntity(json.getAwsInfraList()));
        user.setAzureInfraList(azureInfraConverter.convertAllJsonToEntity(json.getAzureInfraList()));
        user.setAzureCloudInstanceList(azureCloudInstanceConverter.convertAllJsonToEntity(json.getAzureCloudList()));
        user.setAwsCloudInstanceList(awsCloudInstanceConverter.convertAllJsonToEntity(json.getAwsCloudList()));
        return user;
    }
}
