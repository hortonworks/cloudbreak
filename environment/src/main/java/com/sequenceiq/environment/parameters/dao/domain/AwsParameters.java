package com.sequenceiq.environment.parameters.dao.domain;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("AWS")
public class AwsParameters extends BaseParameters {

    @Column(name = "s3guard_dynamo_table_name")
    private String s3guardTableName;

    public String getS3guardTableName() {
        return s3guardTableName;
    }

    public void setS3guardTableName(String s3guardTableName) {
        this.s3guardTableName = s3guardTableName;
    }
}
