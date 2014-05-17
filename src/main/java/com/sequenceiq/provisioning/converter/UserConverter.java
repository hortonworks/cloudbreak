package com.sequenceiq.provisioning.converter;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.provisioning.domain.User;
import com.sequenceiq.provisioning.json.UserJson;

@Component
public class UserConverter extends AbstractConverter<UserJson, User> {

    @Autowired
    private AzureStackConverter azureStackConverter;

    @Autowired
    private AwsStackConverter awsStackConverter;

    @Override
    public UserJson convert(User entity) {
        UserJson userJson = new UserJson();
        userJson.setEmail(entity.getEmail());
        userJson.setAwsStackList(awsStackConverter.convertAllEntityToJson(entity.getAwsStackList()));
        userJson.setAzureStackList(azureStackConverter.convertAllEntityToJson(entity.getAzureStackList()));
        return userJson;
    }

    @Override
    public User convert(UserJson json) {
        User user = new User();
        user.setEmail(json.getEmail());
        user.setAwsStackList(awsStackConverter.convertAllJsonToEntity(json.getAwsStackList()));
        user.setAzureStackList(azureStackConverter.convertAllJsonToEntity(json.getAzureStackList()));
        return user;
    }
}
