package com.sequenceiq.cloudbreak.service.account;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.Account;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.domain.UserRole;
import com.sequenceiq.cloudbreak.repository.AccountRepository;
import com.sequenceiq.cloudbreak.repository.UserRepository;

@Service
public class DefaultAccountService implements AccountService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultAccountService.class);

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public Account registerAccount(String accountName) {
        Account account = new Account();
        account.setName(accountName);
        account = accountRepository.save(account);
        LOGGER.info("New account with id '{}' registered,", account.getId());
        return account;
    }

    @Override
    public Set<User> accountUsers(Long accountId) {
        LOGGER.debug("Retrieving decorated users for account with id: '{}'", accountId);
        Set<User> users = accountRepository.accountUsers(accountId);
        LOGGER.debug("Found #{} users.", users.size());
        return users;
    }

    @Override
    public User accountUserData(Long accountId, UserRole role) {
        LOGGER.debug("Retrieving company user data for the company '{}' in userRole '{}' ...", accountId, role);
        User admin = accountRepository.findAccountAdmin(accountId);
        User decoratedAdmin = userRepository.findOneWithLists(admin.getId());

//        getAwsCredentialsForRole(decoratedAdmin, role);
//        getAzureCredentialsForRole(decoratedAdmin, role);
        // getAwsTemplatesForRole(decoratedAdmin, role);
        // getAzureTemplatesForRole(decoratedAdmin, role);
        getSatcksForRole(decoratedAdmin, role);
        // getBlueprintsForRole(decoratedAdmin, role);

        return decoratedAdmin;
    }

    @Override
    public boolean isUserInAccount(Long accountId, Long userId) {
        LOGGER.debug("Checking whether user id {} belongs to the account with id {} ...", userId, accountId);
        boolean userInAccount = accountRepository.isUserInAccount(accountId, userId);
        LOGGER.debug("Result: {}", userInAccount);
        return userInAccount;
    }

    // private void getBlueprintsForRole(User decoratedAdmin, UserRole role) {
    // Set<Blueprint> blueprintsInRole = new HashSet<>();
    // for (Blueprint blueprint : decoratedAdmin.getBlueprints()) {
    // if (blueprint.getUserRoles().contains(role)) {
    // blueprintsInRole.add(blueprint);
    // }
    // }
    // decoratedAdmin.getBlueprints().clear();
    // decoratedAdmin.getBlueprints().addAll(blueprintsInRole);
    // }

    private void getSatcksForRole(User decoratedAdmin, UserRole role) {
        Set<Stack> stacksInRole = new HashSet<>();
        for (Stack stack : decoratedAdmin.getStacks()) {
            if (stack.getUserRoles().contains(role)) {
                stacksInRole.add(stack);
            }
        }
        decoratedAdmin.getStacks().clear();
        decoratedAdmin.getStacks().addAll(stacksInRole);
    }

    // private void getAzureTemplatesForRole(User decoratedAdmin, UserRole role)
    // {
    // Set<AzureTemplate> azureTemplatesInRole = new HashSet<>();
    // for (AzureTemplate azureTemplate : decoratedAdmin.getAzureTemplates()) {
    // if (azureTemplate.getUserRoles().contains(role)) {
    // azureTemplatesInRole.add(azureTemplate);
    // }
    // }
    // decoratedAdmin.getAzureTemplates().clear();
    // decoratedAdmin.getAzureTemplates().addAll(azureTemplatesInRole);
    // }
    //
    // private void getAwsTemplatesForRole(User decoratedAdmin, UserRole role) {
    // Set<AwsTemplate> awsTemplatesInRole = new HashSet<>();
    // for (AwsTemplate awsTemplate : decoratedAdmin.getAwsTemplates()) {
    // if (awsTemplate.getUserRoles().contains(role)) {
    // awsTemplatesInRole.add(awsTemplate);
    // }
    // }
    // decoratedAdmin.getAwsTemplates().clear();
    // decoratedAdmin.getAwsTemplates().addAll(awsTemplatesInRole);
    // }

//    private void getAzureCredentialsForRole(User decoratedAdmin, UserRole role) {
//        Set<AzureCredential> azureCredentialsInRole = new HashSet<>();
//        for (AzureCredential azureCredential : decoratedAdmin.getAzureCredentials()) {
//            if (azureCredential.getUserRoles().contains(role)) {
//                azureCredentialsInRole.add(azureCredential);
//            }
//        }
//        decoratedAdmin.getAzureCredentials().clear();
//        decoratedAdmin.getAzureCredentials().addAll(azureCredentialsInRole);
//    }
//
//    private void getAwsCredentialsForRole(User decoratedAdmin, UserRole role) {
//        Set<AwsCredential> awsCredentialsInRole = new HashSet<>();
//        for (AwsCredential awsCredential : decoratedAdmin.getAwsCredentials()) {
//            if (awsCredential.getUserRoles().contains(role)) {
//                awsCredentialsInRole.add(awsCredential);
//            }
//        }
//        decoratedAdmin.getAwsCredentials().clear();
//        decoratedAdmin.getAwsCredentials().addAll(awsCredentialsInRole);
//    }

}
