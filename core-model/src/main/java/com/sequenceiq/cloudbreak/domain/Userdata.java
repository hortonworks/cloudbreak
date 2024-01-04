package com.sequenceiq.cloudbreak.domain;

import java.util.Objects;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;

import com.sequenceiq.cloudbreak.common.dal.model.AccountIdAwareResource;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.secret.SecretValue;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.service.secret.domain.SecretToString;

@Entity
public class Userdata implements ProvisionEntity, AccountIdAwareResource {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "userdata_generator")
    @SequenceGenerator(name = "userdata_generator", sequenceName = "userdata_id_seq", allocationSize = 1)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    private Stack stack;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret gatewayUserdata = Secret.EMPTY;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret coreUserdata = Secret.EMPTY;

    private String accountId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Stack getStack() {
        return stack;
    }

    public void setStack(Stack stack) {
        this.stack = stack;
    }

    public String getGatewayUserdata() {
        return gatewayUserdata.getRaw();
    }

    public String getGatewayUserdataSecret() {
        return gatewayUserdata.getSecret();
    }

    public void setGatewayUserdata(String gatewayUserdata) {
        if (gatewayUserdata != null) {
            this.gatewayUserdata = new Secret(gatewayUserdata);
        }
    }

    public String getCoreUserdata() {
        return coreUserdata.getRaw();
    }

    public String getCoreUserdataSecret() {
        return coreUserdata.getSecret();
    }

    public void setCoreUserdata(String coreUserdata) {
        if (coreUserdata != null) {
            this.coreUserdata = new Secret(coreUserdata);
        }
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    @Override
    public String getAccountId() {
        return accountId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Userdata userdata = (Userdata) o;
        return Objects.equals(id, userdata.id)
                && Objects.equals(stack, userdata.stack)
                && Objects.equals(gatewayUserdata, userdata.gatewayUserdata)
                && Objects.equals(coreUserdata, userdata.coreUserdata)
                && Objects.equals(accountId, userdata.accountId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, gatewayUserdata, coreUserdata, accountId);
    }

    @Override
    public String toString() {
        return "Userdata{" +
                "id=" + id +
                ", accountId='" + accountId + '\'' +
                ", stackId='" + stack.getId() + '\'' +
                '}';
    }
}
