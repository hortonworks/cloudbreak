package com.sequenceiq.it.cloudbreak.util;

import static java.lang.String.format;

import java.io.IOException;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.util.ResourceUtil;

@Component
public class RecipeUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeUtil.class);

    private static final String PRE_SERVICE_DEPLOYMENT = "classpath:/recipes/pre-service-deployment.sh";

    private static final String POST_CLOUDERA_MANAGER_START = "classpath:/recipes/post-cm-start.sh";

    private static final String POST_SERVICE_DEPLOYMENT = "classpath:/recipes/post-service-deployment.sh";

    private static final String PRE_TERMINATION = "classpath:/recipes/pre-termination.sh";

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
