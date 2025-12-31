package com.sequenceiq.cloudbreak.structuredevent.rest.urlparsers;

import static com.sequenceiq.cloudbreak.structuredevent.rest.urlparser.LegacyRestUrlParser.RESOURCE_EVENT;
import static com.sequenceiq.cloudbreak.structuredevent.rest.urlparser.LegacyRestUrlParser.RESOURCE_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.UriInfo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.controller.EndpointConfig;
import com.sequenceiq.cloudbreak.structuredevent.rest.urlparser.LegacyRestUrlParser;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = RestUrlParserConfig.class)
public class RestUrlParserTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestUrlParserTest.class);

    private static final String WORKSPACE_ID = "1111";

    private static final String RESOURCE_ID = "2222";

    private static final String RESOURCE_NAME = "Some_Stupid:Resource Name:";

    private static final String WORKSPACE_NAME = "some-stupid-workspace-name-34";

    private static final String RESOURCE_CRN = "crn:cdp:cloudbreak:us-west:default:stack:440ac57e-9f21-4b9a-bcfd-3034a5738b12";

    private static final String PATH_AUTOSCALE = "autoscale";

    private static final String PATH_V_4 = "v4";

    private static final String PATH_CREDENTIALS = "credentials";

    private static final String PATH_CODE_GRANT_FLOW = "code_grant_flow";

    private static final String PATH_IMAGE_CATALOGS = "image_catalogs";

    private static final String PATH_CUSTOM_IMAGE_CATALOGS = "custom_image_catalogs";

    private static final String CRN = "crn";

    private static final String NAME = "name";

    private static final String ID_REGEX = "\\d+";

    private static final String CRN_REGEX = "[^/]+";

    private static final String SLASH = "/";

    private static final String[] EXCLUDES = {
            "/v1/distrox",
            "/v1/internal/distrox",
            "/flow-public",
            "/autoscale",
            "/restart_instances",
            "cluster_templates",
            "/v4/events",
            "/v4/diagnostics",
            "/v4/progress",
            "/v4/operation",
            "/v4/custom_configurations",
            "internal/get_database_certificate_status",
            "internal/manage_db_user"
    };

    private static final String[] EXCLUDEPATHS = {
            "/stacks/internal/crn",
            "/govCloud",
            "stacks/internal/used_subnets",
            "stacks/internal/rotate_secret",
            "/singleimage/internal",
            "/imd_update",
            "internal/get_database_certificate_status",
            "internal/manage_db_user",
            "support/internal/defaults",
            "internal/get_clusters_names_by_encryption_profile"
    };

    @Mock
    private ContainerRequestContext containerRequestContext;

    @Mock
    private UriInfo uriInfo;

    @Autowired
    private List<LegacyRestUrlParser> restUrlParsers;

    @Test
    void testEventUrlParser() {
        when(containerRequestContext.getMethod()).thenReturn("DELETE");
        when(containerRequestContext.getUriInfo()).thenReturn(uriInfo);
        LegacyRestUrlParser eventUrlParser = new V4ExistingResourceEventRestUrlParser();

        when(uriInfo.getPath()).thenReturn("v4/2/users/johnny/evict");
        Map<String, String> params = new HashMap<>();
        eventUrlParser.fillParams(containerRequestContext, params);
        assertEquals("users", params.get(RESOURCE_TYPE));
        assertEquals("evict", params.get(RESOURCE_EVENT));

        when(uriInfo.getPath()).thenReturn("v4/2/users/evict");
        params = new HashMap<>();
        eventUrlParser.fillParams(containerRequestContext, params);
        assertEquals(0, params.keySet().size());

        when(uriInfo.getPath()).thenReturn("v4/users/johnny/evict");
        params = new HashMap<>();
        eventUrlParser.fillParams(containerRequestContext, params);
        assertEquals(0, params.keySet().size());

        when(uriInfo.getPath()).thenReturn("v4/2/users/johnny/12");
        params = new HashMap<>();
        eventUrlParser.fillParams(containerRequestContext, params);
        assertEquals(0, params.keySet().size());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testIfRestUrlParserMissing() {
        List<Class<?>> controllers = (List<Class<?>>) ReflectionTestUtils.getField(EndpointConfig.class, "CONTROLLERS");
        for (Class<?> controller : controllers) {
            Optional<Class<?>> endpointClass = Arrays.stream(controller.getInterfaces())
                    .filter(interf -> interf.getSimpleName().contains("Endpoint"))
                    .findFirst();
            if (endpointClass.isPresent()) {
                String rootPath = checkAndReturnPath(endpointClass);
                if (Arrays.stream(EXCLUDES).parallel().noneMatch(rootPath::contains)) {
                    for (Method method : endpointClass.get().getMethods()) {
                        String methodPath = getMethodPath(rootPath, method);
                        String requestMethod = getRequestMethod(method);
                        if (requestMethod != null) {
                            List<LegacyRestUrlParser> matchedParsers = new ArrayList<>();
                            Map<String, String> params = new HashMap<>();
                            for (LegacyRestUrlParser restUrlParser : restUrlParsers) {
                                setupMocks(methodPath, requestMethod);
                                if (restUrlParser.fillParams(containerRequestContext, params)) {
                                    matchedParsers.add(restUrlParser);
                                    LOGGER.info("Found matching parser: {}", restUrlParser.getClass().getName());
                                }
                            }
                            LOGGER.info("Matched URL: {}\nslurped params: {}", methodPath, params);
                            boolean doWeWantToExcludeThePath = doWeWantToExcludeThePath(methodPath);
                            if (!doWeWantToExcludeThePath) {
                                if (matchedParsers.isEmpty()) {
                                    fail("Can not find RestUrlParser for this endpoint: " + endpointClass.get().getSimpleName()
                                            + "\nmethod: " + method.getName()
                                            + "\nurl: " + methodPath);
                                } else {
                                    checkIfMoreThanOneParserDetected(methodPath, matchedParsers);
                                    checkWorkspaceId(methodPath, params);
                                    checkResourceIdOrName(methodPath, params);
                                    checkResourceType(methodPath, params);
                                    checkResourceEvent(methodPath, params);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean doWeWantToExcludeThePath(String methodPath) {
        boolean exclude = false;
        for (String excludePath : EXCLUDEPATHS) {
            if (methodPath.contains(excludePath)) {
                exclude = true;
                break;
            }
        }
        return exclude;
    }

    private void setupMocks(String methodPath, String requestMethod) {
        when(containerRequestContext.getMethod()).thenReturn(requestMethod);
        when(uriInfo.getPath()).thenReturn(methodPath.substring(1));
        when(containerRequestContext.getUriInfo()).thenReturn(uriInfo);
    }

    private void checkResourceEvent(String methodPath, Map<String, String> params) {
        String[] parts = methodPath.substring(1).split(SLASH);
        String resourceEvent = null;

        if (List.of(parts).contains(PATH_IMAGE_CATALOGS) && parts.length > 5) {
            // Specialized image catalog related endpoints that have nothing in the URLs that qualify as a resource event,
            // so this is just a fake for the sake of backward compatibility...
            resourceEvent = PATH_IMAGE_CATALOGS;
        } else if (List.of(parts).contains(PATH_CUSTOM_IMAGE_CATALOGS) && parts.length > 3) {
            // Specialized custom image catalog related endpoints that have nothing in the URLs that qualify as a resource event,
            // so this is just a fake for the sake of backward compatibility...
            resourceEvent = PATH_CUSTOM_IMAGE_CATALOGS;
        } else if (parts.length >= 4 && PATH_V_4.equals(parts[0]) && (parts[3].equals(CRN) || parts[3].equals(NAME))) {
            resourceEvent = parts[2];
        } else if (parts.length >= 4 && PATH_V_4.equals(parts[0]) && !("audits".equals(parts[2]) && parts[3].matches(ID_REGEX))
                && !(PATH_CREDENTIALS.equals(parts[2]) && PATH_CODE_GRANT_FLOW.equals(parts[3])) && !RESOURCE_NAME.equals(parts[parts.length - 1])
                && !("internal".equals(parts[3]) && parts.length == 4)) {
            // Skip v4/{workspaceId}/audits/{auditId}
            // Skip v4/{workspaceId}/blueprints/{name} and similars
            // Skip v4/{workspaceId}/credentials/code_grant_flow/init
            // Skip v4/{workspaceId}/credentials/code_grant_flow/init/{name}
            // Skip v4/{workspaceId}/credentials/code_grant_flow/authorization/{cloudPlatform}
            // Match v4/{workspaceId}/audits/zip and similars
            // Match v4/{workspaceId}/blueprints/{name}/parameters and similars
            resourceEvent = parts[parts.length - 1];
        } else if ((parts.length == 5 || parts.length == 6) && PATH_V_4.equals(parts[0]) && PATH_CREDENTIALS.equals(parts[2])) {
            // Match v4/{workspaceId}/credentials/prerequisites/{cloudPlatform}
            // Match v4/{workspaceId}/credentials/code_grant_flow/init
            // Match v4/{workspaceId}/credentials/code_grant_flow/init/{name}
            // Match v4/{workspaceId}/credentials/code_grant_flow/authorization/{cloudPlatform}
            resourceEvent = parts.length == 5 && !PATH_CODE_GRANT_FLOW.equals(parts[3]) ? parts[3] : String.join(SLASH, parts[3], parts[4]);
        } else if (parts.length == 3 && PATH_V_4.equals(parts[0]) && !parts[1].matches(ID_REGEX) && !WORKSPACE_NAME.equals(parts[2])) {
            // Skip v4/workspaces/{name}
            // Match v4/users/evict and similars
            resourceEvent = parts[2];
        } else if ((parts.length == 2 || parts.length == 3 && "all".equals(parts[2])) && PATH_AUTOSCALE.equals(parts[0])) {
            // Skip autoscale/stack/{crn}
            // Match autoscale/ambari and autoscale/stack/all
            resourceEvent = parts.length == 2 ? parts[1] : String.join(SLASH, parts[1], parts[2]);
        } else if (parts.length >= 4 && PATH_AUTOSCALE.equals(parts[0])) {
            // Match autoscale/stack/{crn}/{userId} and similars
            String[] eventParts = new String[parts.length - 3];
            System.arraycopy(parts, 3, eventParts, 0, eventParts.length);
            resourceEvent = String.join(SLASH, eventParts);
        }

        if (resourceEvent == null) {
            assertNull(params.get(RESOURCE_EVENT), "params must not contain resource event");
        } else {
            assertEquals(resourceEvent, params.get(RESOURCE_EVENT), "params should contain resource event");
        }
    }

    private void checkResourceType(String methodPath, Map<String, String> params) {
        String[] parts = methodPath.substring(1).split(SLASH);
        String resourceType = null;
        if (parts.length > 0) {
            resourceType = parts[0];
            if (parts.length > 2 && PATH_AUTOSCALE.equals(parts[0]) && parts[2].matches(CRN_REGEX) && !"all".equals(parts[2])) {
                // skip autoscale/stack/all - since `all` matches to the crn regex
                resourceType = parts[1];
            } else if (parts.length > 1 && PATH_V_4.equals(parts[0])) {
                resourceType = parts.length > 2 && parts[1].matches(ID_REGEX) ? parts[2] : parts[1];
            }
        }
        assertNotNull(methodPath + "Could not determine expected resource type for path: ", resourceType);

        assertEquals(resourceType, params.get(RESOURCE_TYPE), "params should contain resource type");
    }

    private void checkResourceIdOrName(String methodPath, Map<String, String> params) {
        if (methodPath.contains(RESOURCE_ID)) {
            assertEquals(RESOURCE_ID,
                    params.get(LegacyRestUrlParser.RESOURCE_ID), "params should contain resource ID");
            assertNull(params.get(LegacyRestUrlParser.RESOURCE_NAME), "params must not contain resource name when having a resource ID");
        } else if (methodPath.contains(RESOURCE_NAME)) {
            assertEquals(RESOURCE_NAME,
                    params.get(LegacyRestUrlParser.RESOURCE_NAME), "params should contain resource name");
            assertNull(params.get(LegacyRestUrlParser.RESOURCE_ID), "params must not contain resource ID when having a resource name");
        } else if (methodPath.contains(WORKSPACE_NAME)) {
            assertEquals(WORKSPACE_NAME,
                    params.get(LegacyRestUrlParser.RESOURCE_NAME), "params should contain workspace name");
            assertNull(params.get(LegacyRestUrlParser.RESOURCE_ID), "params must not contain resource ID when having a resource name");
        } else if (methodPath.contains(RESOURCE_CRN)) {
            assertEquals(RESOURCE_CRN, params.get(LegacyRestUrlParser.RESOURCE_NAME), "params should contains resource crn");
            assertNull(params.get(LegacyRestUrlParser.RESOURCE_ID), "params must not contain resource ID when having a resource crn");
        } else {
            assertNull(params.get(LegacyRestUrlParser.RESOURCE_ID), String.format("%s method params must not contain resource ID", methodPath));
            assertNull(params.get(LegacyRestUrlParser.RESOURCE_NAME), String.format("%s method params must not contain resource name", methodPath));
        }
    }

    private void checkWorkspaceId(String methodPath, Map<String, String> params) {
        if (methodPath.contains(WORKSPACE_ID)) {
            assertEquals(WORKSPACE_ID,
                    params.get(LegacyRestUrlParser.WORKSPACE_ID), "params should contain workspace ID");
        }
    }

    private void checkIfMoreThanOneParserDetected(String methodPath, List<LegacyRestUrlParser> matchedParsers) {
        if (matchedParsers.size() > 1) {
            fail("More than one parser match for this URL: " + methodPath
                    + "\nParsers: " + matchedParsers.stream()
                    .map(restUrlParser -> restUrlParser.getClass().getSimpleName())
                    .collect(Collectors.joining(", ")));
        } else {
            LOGGER.info("Parser: {}\n", matchedParsers.get(0).getClass().getSimpleName());
        }
    }

    private String getMethodPath(String rootPath, Method method) {
        Path methodPathAnnotation = AnnotationUtils.findAnnotation(method, Path.class);
        String subpath = methodPathAnnotation == null ? "" : methodPathAnnotation.value();
        if (!subpath.isEmpty() && !subpath.startsWith(SLASH)) {
            subpath = '/' + subpath;
        }
        String methodPath = rootPath + subpath;
        methodPath = replacePathParamsWithExamples(method, methodPath);
        return methodPath;
    }

    private String checkAndReturnPath(Optional<Class<?>> endpointClass) {
        Path controllerPathAnnotation = AnnotationUtils.findAnnotation(endpointClass.get(), Path.class);
        if (controllerPathAnnotation == null) {
            fail("Can not find Path annotation for this controller: " + endpointClass.get().getSimpleName());
        }
        return controllerPathAnnotation.value();
    }

    private String getRequestMethod(Method method) {
        String requestMethod = null;
        if (AnnotationUtils.findAnnotation(method, PUT.class) != null) {
            requestMethod = "PUT";
        }
        if (AnnotationUtils.findAnnotation(method, POST.class) != null) {
            requestMethod = "POST";
        }
        if (AnnotationUtils.findAnnotation(method, DELETE.class) != null) {
            requestMethod = "DELETE";
        }
        if (AnnotationUtils.findAnnotation(method, GET.class) != null) {
            requestMethod = "GET";
        }
        return requestMethod;
    }

    private String replacePathParamsWithExamples(Method method, String methodPath) {
        for (Parameter parameter : method.getParameters()) {
            PathParam pathParam = AnnotationUtils.findAnnotation(parameter, PathParam.class);
            if (pathParam != null) {
                String pathParamValue = pathParam.value();
                Class<?> parameterType = parameter.getType();
                if (Long.class.equals(parameterType)) {
                    methodPath = "workspaceId".equals(pathParamValue)
                            ? methodPath.replace('{' + pathParamValue + '}', WORKSPACE_ID)
                            : methodPath.replace('{' + pathParamValue + '}', RESOURCE_ID);
                } else {
                    if (methodPath.startsWith("/v4/workspaces")) {
                        methodPath = methodPath.replace('{' + pathParamValue + '}', WORKSPACE_NAME);
                    } else if (methodPath.startsWith("/autoscale/stack")) {
                        methodPath = methodPath.replace('{' + pathParamValue + '}', RESOURCE_CRN);
                    } else if (pathParam.value().equals(NAME)) {
                        methodPath = methodPath.replace('{' + pathParamValue + '}', RESOURCE_NAME);
                    }  else if (pathParam.value().equals(CRN)) {
                        methodPath = methodPath.replace('{' + pathParamValue + '}', RESOURCE_CRN);
                    } else if (methodPath.contains("image_catalogs") && pathParam.value().equals("type")) {
                        methodPath = methodPath.replace('{' + pathParamValue + '}', "DATALAKE");
                    } else if (methodPath.contains("image_catalogs") && pathParam.value().equals("provider")) {
                        methodPath = methodPath.replace('{' + pathParamValue + '}', "AWS");
                    } else if (methodPath.contains("image_catalogs") && pathParam.value().equals("runtime")) {
                        methodPath = methodPath.replace('{' + pathParamValue + '}', "7.2.6");
                    } else {
                        methodPath = methodPath.replace('{' + pathParamValue + '}', RESOURCE_NAME);
                    }
                }
            }
        }
        return methodPath;
    }

}
