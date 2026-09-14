package com.sequenceiq.cloudbreak.cloud.credential;

public interface CredentialAssumeRoleTypePersister {

    void persistAssumeRoleType(String credentialCrn, String accountId, String assumeRoleType);
}
