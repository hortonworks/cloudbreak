package com.sequenceiq.cloudbreak.altus;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AltusDatabusConfiguration {

    private final String altusDatabusEndpoint;

    private final String altusDatabusS3BucketName;

    private final boolean useSharedAltusCredential;

    private final String sharedAccessKey;

    private final char[] sharedSecretKey;

    public AltusDatabusConfiguration(
            @Value("${altus.databus.endpoint:}") String altusDatabusEndpoint,
            @Value("${altus.databus.s3.bucket:}") String altusDatabusS3BucketName,
            @Value("${altus.databus.shared.credential.enabled:false}") boolean useSharedAltusCredential,
            @Value("${altus.databus.shared.accessKey:}") String sharedAccessKey,
            @Value("${altus.databus.shared.secretKey:}") String sharedSecretKey) {
        this.altusDatabusEndpoint = altusDatabusEndpoint;
        this.altusDatabusS3BucketName = altusDatabusS3BucketName;
        this.useSharedAltusCredential = useSharedAltusCredential;
        this.sharedAccessKey = sharedAccessKey;
        this.sharedSecretKey = StringUtils.isBlank(sharedSecretKey) ? null : sharedSecretKey.toCharArray();
    }

    public String getAltusDatabusEndpoint() {
        return altusDatabusEndpoint;
    }

    public String getAltusDatabusS3BucketName() {
        return altusDatabusS3BucketName;
    }

    public boolean isUseSharedAltusCredential() {
        return useSharedAltusCredential;
    }

    public String getSharedAccessKey() {
        return sharedAccessKey;
    }

    public char[] getSharedSecretKey() {
        return sharedSecretKey;
    }
}
