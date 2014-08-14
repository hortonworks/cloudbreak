package com.sequenceiq.cloudbreak.converter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.json.AccountJson;
import com.sequenceiq.cloudbreak.domain.Account;
import com.sequenceiq.cloudbreak.domain.User;

@Component
public class AccountConverter extends AbstractConverter<AccountJson, Account> {

    @Autowired
    private UserConverter userConverter;

    @Override
    public AccountJson convert(Account entity) {
        AccountJson json = new AccountJson();
        json.setAccountName(entity.getName());
        for (User user : entity.getUsers()) {
            json.getUsers().add(userConverter.convert(user));
        }
        return json;
    }

    @Override
    public Account convert(AccountJson json) {
        Account entity = new Account();
        entity.setName(json.getAccountName());
        return entity;
    }
}
