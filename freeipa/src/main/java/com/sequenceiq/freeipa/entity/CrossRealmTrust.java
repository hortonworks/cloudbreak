package com.sequenceiq.freeipa.entity;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.dal.model.AccountIdAwareResource;
import com.sequenceiq.cloudbreak.common.type.KdcType;
import com.sequenceiq.cloudbreak.service.secret.SecretValue;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.service.secret.domain.SecretToString;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.TrustStatus;
import com.sequenceiq.freeipa.entity.util.TrustStatusConverter;

@Entity
public class CrossRealmTrust implements AccountIdAwareResource {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "crossrealmtrust_generator")
    @SequenceGenerator(name = "crossrealmtrust_generator", sequenceName = "crossrealmtrust_id_seq", allocationSize = 1)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    private Stack stack;

    private String environmentCrn;

    private String remoteEnvironmentCrn;

    @Convert(converter = KdcType.Converter.class)
    private KdcType kdcType;

    @Column(name = "fqdn")
    private String kdcFqdn;

    @Column(name = "ip")
    private String kdcIp;

    @Column(name = "realm")
    private String kdcRealm;

    private String dnsIp;

    @Convert(converter = TrustStatusConverter.class)
    private TrustStatus trustStatus;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret trustSecret = Secret.EMPTY;

    private String operationId;

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

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }

    public String getRemoteEnvironmentCrn() {
        return remoteEnvironmentCrn;
    }

    public void setRemoteEnvironmentCrn(String remoteEnvironmentCrn) {
        this.remoteEnvironmentCrn = remoteEnvironmentCrn;
    }

    public KdcType getKdcType() {
        return kdcType;
    }

    public void setKdcType(KdcType kdcType) {
        this.kdcType = kdcType;
    }

    public String getKdcFqdn() {
        return kdcFqdn;
    }

    public void setKdcFqdn(String kdcFqdn) {
        this.kdcFqdn = kdcFqdn;
    }

    public String getKdcIp() {
        return kdcIp;
    }

    public void setKdcIp(String kdcIp) {
        this.kdcIp = kdcIp;
    }

    public String getKdcRealm() {
        return kdcRealm;
    }

    public void setKdcRealm(String kdcRealm) {
        this.kdcRealm = kdcRealm;
    }

    public String getDnsIp() {
        return dnsIp;
    }

    public void setDnsIp(String dnsIp) {
        this.dnsIp = dnsIp;
    }

    public String getTrustSecret() {
        return trustSecret.getRaw();
    }

    public String getTrustSecretSecret() {
        return trustSecret.getSecret();
    }

    public Secret getTrustSecretSecretObject() {
        return trustSecret;
    }

    public void setTrustSecret(String trustSecret) {
        this.trustSecret = new Secret(trustSecret);
    }

    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    public TrustStatus getTrustStatus() {
        return trustStatus;
    }

    public void setTrustStatus(TrustStatus trustStatus) {
        this.trustStatus = trustStatus;
    }

    @Override
    public String getAccountId() {
        return Objects.requireNonNull(Crn.fromString(environmentCrn)).getAccountId();
    }

    @Override
    public String toString() {
        return "CrossRealmTrust{" +
                "id=" + id +
                ", environmentCrn='" + environmentCrn + '\'' +
                ", remoteEnvironmentCrn='" + remoteEnvironmentCrn + '\'' +
                ", kdcType=" + kdcType +
                ", kdcFqdn='" + kdcFqdn + '\'' +
                ", kdcIp='" + kdcIp + '\'' +
                ", kdcRealm='" + kdcRealm + '\'' +
                ", dnsIp='" + dnsIp + '\'' +
                ", trustStatus=" + trustStatus +
                ", operationId='" + operationId + '\'' +
                '}';
    }
}
