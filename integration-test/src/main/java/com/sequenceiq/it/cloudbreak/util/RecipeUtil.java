package com.sequenceiq.it.cloudbreak.util;

import java.io.IOException;

import org.apache.commons.codec.binary.Base64;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.sequenceiq.it.util.ResourceUtil;

@Component
public class RecipeUtil {

    private static final String PRE_CLOUDERA_MANAGER_START = "classpath:/recipes/pre-ambari.sh";

    private static final String POST_INSTALL = "classpath:/recipes/post-install.sh";

    private static final String POST_AMBARI = "classpath:/recipes/post-ambari.sh";

    private static final String PRE_TERMINATION = "classpath:/recipes/pre-termination.sh";

    public String generatePreCmStartRecipeContent(ApplicationContext applicationContext) throws IOException {
        String recipeContentFromFile = ResourceUtil.readResourceAsString(applicationContext, PRE_CLOUDERA_MANAGER_START);
        return Base64.encodeBase64String(recipeContentFromFile.getBytes());
    }

    public String generatePostInstallRecipeContent(ApplicationContext applicationContext) throws IOException {
        String recipeContentFromFile = ResourceUtil.readResourceAsString(applicationContext, POST_INSTALL);
        return Base64.encodeBase64String(recipeContentFromFile.getBytes());
    }

    public String generatePostCmStartRecipeContent(ApplicationContext applicationContext) throws IOException {
        String recipeContentFromFile = ResourceUtil.readResourceAsString(applicationContext, POST_AMBARI);
        return Base64.encodeBase64String(recipeContentFromFile.getBytes());
    }

    public String generatePreTerminationRecipeContent(ApplicationContext applicationContext) throws IOException {
        String recipeContentFromFile = ResourceUtil.readResourceAsString(applicationContext, PRE_TERMINATION);
        return Base64.encodeBase64String(recipeContentFromFile.getBytes());
    }

}
