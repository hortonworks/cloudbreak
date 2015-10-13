package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Entity;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.sequenceiq.cloudbreak.common.type.CloudPlatform;

@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = { "account", "name" })
})
@NamedQuery(
        name = "Credential.findByRoleArn",
        query = "SELECT c FROM Credential c "
                + "WHERE c.roleArn = :roleArn")
public class AwsCredential extends Credential implements ProvisionEntity {

    private String roleArn;

    private String keyPairName;

    public AwsCredential() {

    }

    @Override
    public CloudPlatform cloudPlatform() {
        return CloudPlatform.AWS;
    }

    public String getKeyPairName() {
        return keyPairName;
    }

    public void setKeyPairName(String keyPairName) {
        this.keyPairName = keyPairName;
    }

    public String getRoleArn() {
        return roleArn;
    }

    public void setRoleArn(String roleArn) {
        this.roleArn = roleArn;
    }

}
