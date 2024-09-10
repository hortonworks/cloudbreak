package com.sequenceiq.environment.parameters.dao.domain;

import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sequenceiq.cloudbreak.common.dal.model.AccountIdAwareResource;
import com.sequenceiq.cloudbreak.service.secret.SecretValue;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.service.secret.domain.SecretToString;
import com.sequenceiq.environment.parameter.dto.s3guard.S3GuardTableCreation;

@Entity
@DiscriminatorValue("AWS")
public class AwsParameters extends BaseParameters implements AccountIdAwareResource {

    @Deprecated
    @Column(name = "s3guard_dynamo_table_name")
    private String s3guardTableName;

    @Deprecated
    @Column(name = "s3guard_dynamo_table_creation")
    private S3GuardTableCreation s3guardTableCreation;

    @Column(name = "freeipa_spot_percentage")
    private Integer freeIpaSpotPercentage;

    @Column(name = "freeipa_spot_max_price")
    private Double freeIpaSpotMaxPrice;

    @Column(name = "encryption_key_arn")
    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret encryptionKeyArn = Secret.EMPTY;

    @Deprecated
    public String getS3guardTableName() {
        return s3guardTableName;
    }

    @Deprecated
    public void setS3guardTableName(String s3guardTableName) {
        this.s3guardTableName = s3guardTableName;
    }

    @Deprecated
    public S3GuardTableCreation getS3guardTableCreation() {
        return s3guardTableCreation;
    }

    @Deprecated
    public void setS3guardTableCreation(S3GuardTableCreation s3guardTableCreation) {
        this.s3guardTableCreation = s3guardTableCreation;
    }

    public int getFreeIpaSpotPercentage() {
        return Objects.requireNonNullElse(freeIpaSpotPercentage, 0);
    }

    public void setFreeIpaSpotPercentage(Integer freeIpaSpotPercentage) {
        this.freeIpaSpotPercentage = Objects.requireNonNullElse(freeIpaSpotPercentage, 0);
    }

    public Double getFreeIpaSpotMaxPrice() {
        return freeIpaSpotMaxPrice;
    }

    public void setFreeIpaSpotMaxPrice(Double freeIpaSpotMaxPrice) {
        this.freeIpaSpotMaxPrice = freeIpaSpotMaxPrice;
    }

    public String getEncryptionKeyArn() {
        return getIfNotNull(encryptionKeyArn, Secret::getRaw);
    }

    @JsonIgnore
    public String getEncryptionKeyArnSecret() {
        return getIfNotNull(encryptionKeyArn, Secret::getSecret);
    }

    public void setEncryptionKeyArn(String encryptionKeyArn) {
        this.encryptionKeyArn = new Secret(encryptionKeyArn);
    }
}
