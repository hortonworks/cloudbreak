package com.sequenceiq.cloudbreak.service.account;

import java.util.Set;

import com.sequenceiq.cloudbreak.domain.Account;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.domain.UserRole;

public interface AccountService {

    Account registerAccount(String accountName);

    Set<User> accountUsers(Long accountId);

    /**
     * Retrieves account wide user data for the given role. These data are made
     * available in the given role by the account administrator.
     *
     * @param accountId the identifier of the account
     * @param role      the role the returned resources should be in
     * @return a User instance with the account wide resources in the given user
     * role
     */
    User accountUserData(Long accountId, UserRole role);

    Account findAccount(String accountName);


}
