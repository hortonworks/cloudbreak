package com.sequenceiq.freeipa;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.aws.AwsCredentialParameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.aws.RoleBasedParameters;

public class TestUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestUtil.class);

    private TestUtil() {
    }

    public static Path getFilePath(Class<?> clazz, String fileName) {
        try {
            URL resource = clazz.getResource(fileName);
            return Paths.get(resource.toURI());
        } catch (Exception ex) {
            LOGGER.error("{}: {}", ex.getMessage(), ex);
            return null;
        }
    }

    public static S3CloudStorageV1Parameters getS3CloudStorageV1Parameters(String instanceProfile) {
        S3CloudStorageV1Parameters s3CloudStorageV1Parameters = new S3CloudStorageV1Parameters();
        s3CloudStorageV1Parameters.setInstanceProfile(instanceProfile);
        return s3CloudStorageV1Parameters;
    }

    public static RoleBasedParameters getRoleBasedParameters(String roleArn) {
        RoleBasedParameters roleBasedParameters = new RoleBasedParameters();
        roleBasedParameters.setRoleArn(roleArn);
        return roleBasedParameters;
    }

    public static AwsCredentialParameters getAwsCredentialParameters(String roleArn) {
        AwsCredentialParameters awsCredentialParameters = new AwsCredentialParameters();
        awsCredentialParameters.setRoleBased(getRoleBasedParameters(roleArn));
        return awsCredentialParameters;
    }
}
