package com.sequenceiq.it.cloudbreak.util;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.api.swagger.UsersResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.model.ApiAuthRoleRef;
import com.cloudera.api.swagger.model.ApiUser2;
import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.ClouderaManagerStackDescriptorV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.StackMatrixV4Response;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;

public class ClouderaManagerUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerUtil.class);

    private static final String PRODUCT_NAME = "CDH";

    private static final String CDH_VERSION = "7.0.0";

    private static final String DEFAULT_REPO_BASE_URL = "http://cloudera-build-us-west-1.vpc.cloudera.com/";

    private static final String BASE_URL_FOR_YARN = "http://cloudera-build-3-us-central-1.gce.cloudera.com/";

    private static final String REDHAT_7 = "redhat7";

    private ClouderaManagerUtil() {
    }

    private static ClouderaManagerStackDescriptorV4Response getCMDescriptor(TestContext testContext) {
        StackMatrixV4Response stackMatrix = testContext
                .getCloudbreakClient()
                .getCloudbreakClient()
                .utilV4Endpoint().getStackMatrix();
        return stackMatrix.getCdh().get(CDH_VERSION);
    }

    public static StackTestDto checkClouderaManagerUser(TestContext testContext, StackTestDto stackTestDto, CloudbreakClient cloudbreakClient) {
        String cmIp = stackTestDto.getResponse().getCluster().getServerIp();
        String cmUser = stackTestDto.getRequest().getCluster().getUserName();
        String cmPassword = stackTestDto.getRequest().getCluster().getPassword();
        Integer cmGatewayPort = 7180;
        String userDetails = "";

        ApiClient cmClient = new ApiClient();
        cmClient.setBasePath("http://" + cmIp + ':' + cmGatewayPort + "/api/v31");
        cmClient.setUsername(cmUser);
        cmClient.setPassword(cmPassword);
        cmClient.setVerifyingSsl(false);

        UsersResourceApi usersResourceApi = new UsersResourceApi(cmClient);
        try {
            ApiUser2 testUserDetails = usersResourceApi.readUser2("teszt");
            String testUserName = String.valueOf(testUserDetails.getName());
            List<ApiAuthRoleRef> testUserAuthRoles = testUserDetails.getAuthRoles();
            Optional<ApiAuthRoleRef> testUserDisplayName = testUserAuthRoles.stream()
                    .filter(userAuthRole -> userAuthRole.getDisplayName().equals("Full Administrator"))
                    .findFirst();
            if (testUserDisplayName.isPresent()) {
                LOGGER.info("Test user exist with desired role: {}", testUserDisplayName);
            } else {
                LOGGER.error("Test user exist with desired role: {}", testUserDisplayName);
                throw new TestFailException("Test user exist! However with different role: " + testUserDisplayName);
            }
            userDetails = String.valueOf(testUserDetails);
            if (Strings.isNullOrEmpty(testUserName)) {
                LOGGER.error("Requested user does not exist: " + userDetails);
                throw new TestFailException("Requested user is not exist");
            } else if (!"teszt".equals(testUserName)) {
                LOGGER.error("Requested user details are not valid: {}", userDetails);
                throw new TestFailException("Requested user details are not valid " + userDetails);
            }
        } catch (Exception e) {
            LOGGER.error("Can't get users' list at: {} or test user is not valid with {}", cmClient.getBasePath(), userDetails);
            throw new TestFailException("Can't get users' list at: " + cmClient.getBasePath() + " or test user is not valid with " + userDetails);
        }
        return stackTestDto;
    }

    public static String getRepositoryUrl(TestContext testContext, String cloudProvider) {
        ClouderaManagerStackDescriptorV4Response clouderaManagerStackDescriptorV4Response = getCMDescriptor(testContext);
        String repositoryUrl = clouderaManagerStackDescriptorV4Response.getClouderaManager().getRepository().get(REDHAT_7).getBaseUrl();
        repositoryUrl = "YARN".equals(cloudProvider) ? repositoryUrl.replace(DEFAULT_REPO_BASE_URL, BASE_URL_FOR_YARN) : repositoryUrl;

        return repositoryUrl;
    }

    public static String getRepositoryVersion(TestContext testContext) {
        ClouderaManagerStackDescriptorV4Response clouderaManagerStackDescriptorV4Response = getCMDescriptor(testContext);
        return clouderaManagerStackDescriptorV4Response.getClouderaManager().getVersion();
    }

    public static String getProductParcel(TestContext testContext, String cloudProvider) {
        ClouderaManagerStackDescriptorV4Response clouderaManagerStackDescriptorV4Response = getCMDescriptor(testContext);
        String productParcel = clouderaManagerStackDescriptorV4Response.getRepository().getStack().get(REDHAT_7);
        productParcel = "YARN".equals(cloudProvider) ? productParcel.replace(DEFAULT_REPO_BASE_URL, BASE_URL_FOR_YARN) : productParcel;

        return productParcel;
    }

    public static String getProductVersion(TestContext testContext) {
        ClouderaManagerStackDescriptorV4Response clouderaManagerStackDescriptorV4Response = getCMDescriptor(testContext);
        return clouderaManagerStackDescriptorV4Response.getVersion();
    }

    public static String getProductName(TestContext testContext) {
        return PRODUCT_NAME;
    }
}
