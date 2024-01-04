package com.sequenceiq.it.cloudbreak.util;

import static java.lang.String.format;

import java.io.IOException;

import jakarta.inject.Inject;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonCloudProperties;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.aws.amazons3.client.S3Client;
import com.sequenceiq.it.util.ResourceUtil;

@Component
public class RecipeUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeUtil.class);

    private static final String PRE_SERVICE_DEPLOYMENT = "classpath:/recipes/pre-service-deployment.sh";

    private static final String POST_CLOUDERA_MANAGER_START = "classpath:/recipes/post-cm-start.sh";

    private static final String POST_SERVICE_DEPLOYMENT = "classpath:/recipes/post-service-deployment.sh";

    private static final String PRE_TERMINATION = "classpath:/recipes/pre-termination.sh";

    private static final String E2E_PRE_TERMINATION = "classpath:/recipes/e2e-pre-termination.sh";

    @Inject
    private CommonCloudProperties commonCloudProperties;

    @Inject
    private S3Client s3Client;

    public String generatePreDeploymentRecipeContent(ApplicationContext applicationContext) {
        try {
            String recipeContentFromFile = ResourceUtil.readResourceAsString(applicationContext, PRE_SERVICE_DEPLOYMENT);
            return Base64.encodeBase64String(recipeContentFromFile.getBytes());
        } catch (IOException e) {
            LOGGER.error("Cannot generate PRE_SERVICE_DEPLOYMENT recipe content! Cannot find recipe file at path: {} throws: {}!",
                    PRE_SERVICE_DEPLOYMENT, e.getMessage(), e);
            throw new TestFailException(format(" Cannot generate PRE_SERVICE_DEPLOYMENT recipe content! Cannot find recipe file at path: %s",
                    PRE_SERVICE_DEPLOYMENT), e);
        }
    }

    public String generatePostCmStartRecipeContent(ApplicationContext applicationContext) {
        try {
            String recipeContentFromFile = ResourceUtil.readResourceAsString(applicationContext, POST_CLOUDERA_MANAGER_START);
            return Base64.encodeBase64String(recipeContentFromFile.getBytes());
        } catch (IOException e) {
            LOGGER.error("Cannot generate POST_CLOUDERA_MANAGER_START recipe content! Cannot find recipe file at path: {} throws: {}!",
                    POST_CLOUDERA_MANAGER_START, e.getMessage(), e);
            throw new TestFailException(format(" Cannot generate POST_CLOUDERA_MANAGER_START recipe content! Cannot find recipe file at path: %s",
                    POST_CLOUDERA_MANAGER_START), e);
        }
    }

    public String generatePostDeploymentRecipeContent(ApplicationContext applicationContext) {
        try {
            String recipeContentFromFile = ResourceUtil.readResourceAsString(applicationContext, POST_SERVICE_DEPLOYMENT);
            return Base64.encodeBase64String(recipeContentFromFile.getBytes());
        } catch (IOException e) {
            LOGGER.error("Cannot generate POST_SERVICE_DEPLOYMENT recipe content! Cannot find recipe file at path: {} throws: {}!",
                    POST_SERVICE_DEPLOYMENT, e.getMessage(), e);
            throw new TestFailException(format(" Cannot generate POST_SERVICE_DEPLOYMENT recipe content! Cannot find recipe file at path: %s",
                    POST_SERVICE_DEPLOYMENT), e);
        }
    }

    public String generatePreTerminationRecipeContent(ApplicationContext applicationContext) {
        try {
            String recipeContentFromFile = ResourceUtil.readResourceAsString(applicationContext, PRE_TERMINATION);
            return Base64.encodeBase64String(recipeContentFromFile.getBytes());
        } catch (IOException e) {
            LOGGER.error("Cannot generate PRE_TERMINATION recipe content! Cannot find recipe file at path: {} throws: {}!",
                    PRE_TERMINATION, e.getMessage(), e);
            throw new TestFailException(format(" Cannot generate PRE_TERMINATION recipe content! Cannot find recipe file at path: %s",
                    PRE_TERMINATION), e);
        }
    }

    public String generatePreTerminationRecipeContentForE2E(ApplicationContext applicationContext, String preTerminationRecipeName) {
        String cloudProvider = commonCloudProperties.getCloudProvider();
        String cloudStorageCopy;

        if (StringUtils.equalsIgnoreCase(cloudProvider, CloudPlatform.AWS.name())) {
            cloudStorageCopy = format("aws s3 cp /e2e-pre-termination s3://%s/pre-termination/%s/", s3Client.getDefaultBucketName(), preTerminationRecipeName);
        } else if (StringUtils.equalsIgnoreCase(cloudProvider, CloudPlatform.GCP.name())) {
            cloudStorageCopy = format("gsutil cp /e2e-pre-termination gs://cloudbreak-dev/pre-termination/%s/", preTerminationRecipeName);
        } else {
            throw new TestFailException(format("Cannot generate Pre-Termination recipe for '%s' provider!", cloudProvider));
        }

        try {
            String recipeContentFromFile = ResourceUtil.readResourceAsString(applicationContext, E2E_PRE_TERMINATION);
            recipeContentFromFile = recipeContentFromFile.replaceAll("COPY_TO_OBJECT", cloudStorageCopy);
            return Base64.encodeBase64String(recipeContentFromFile.getBytes());
        } catch (IOException e) {
            LOGGER.error("Cannot generate E2E_PRE_TERMINATION recipe content! Cannot find recipe file at path: {} throws: {}!",
                    E2E_PRE_TERMINATION, e.getMessage(), e);
            throw new TestFailException(format(" Cannot generate E2E_PRE_TERMINATION recipe content! Cannot find recipe file at path: %s",
                    E2E_PRE_TERMINATION), e);
        }
    }

    public String generateRecipeContentFromFile(ApplicationContext applicationContext, String recipeLocation) {
        try {
            String recipeContentFromFile = ResourceUtil.readResourceAsString(applicationContext, recipeLocation);
            return Base64.encodeBase64String(recipeContentFromFile.getBytes());
        } catch (IOException e) {
            LOGGER.error("Cannot generate recipe content! Cannot find recipe file at path: {} throws: {}!", recipeLocation, e.getMessage(), e);
            throw new TestFailException(format(" Cannot generate recipe content! Cannot find recipe file at path: %s", recipeLocation), e);
        }
    }

    public String generateRecipeContent(String recipeContent) {
        try {
            return Base64.encodeBase64String(recipeContent.getBytes());
        } catch (Exception e) {
            LOGGER.error("Cannot generate recipe from [{}] throws: {}!", recipeContent, e.getMessage(), e);
            throw new TestFailException(format(" Cannot generate recipe from [%s]", recipeContent), e);
        }
    }
}
