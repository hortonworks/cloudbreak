package com.sequenceiq.cloudbreak.structuredevent.rest.urlparsers;

import static com.sequenceiq.cloudbreak.structuredevent.rest.urlparsers.RestUrlParser.RESOURCE_EVENT;
import static com.sequenceiq.cloudbreak.structuredevent.rest.urlparsers.RestUrlParser.RESOURCE_TYPE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
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

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.UriInfo;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.reflect.Whitebox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import com.sequenceiq.cloudbreak.controller.EndpointConfig;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = RestUrlParserConfig.class)
public class RestUrlParserTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestUrlParserTest.class);

    private static final String WORKSPACE_ID = "1111";

    private static final String RESOURCE_ID = "2222";

    private static final String RESOURCE_NAME = "Some_Stupid:Resource Name:";

    private static final String WORKSPACE_NAME = "some-stupid-workspace-name-34";

    private static final String PATH_AUTOSCALE = "autoscale";

    private static final String PATH_V_4 = "v4";

    private static final String PATH_CREDENTIALS = "credentials";

    private static final String PATH_CODE_GRANT_FLOW = "code_grant_flow";

    private static final String ID_REGEX = "\\d+";

    private static final String SLASH = "/";

    @Mock
    private ContainerRequestContext containerRequestContext;

    @Mock
    private UriInfo uriInfo;

    @Autowired
    private List<RestUrlParser> restUrlParsers;

    private String[] excludes = {};

    @Test
    public void testEventUrlParser() {
        when(containerRequestContext.getMethod()).thenReturn("DELETE");
        when(containerRequestContext.getUriInfo()).thenReturn(uriInfo);
        RestUrlParser eventUrlParser = new V4ExistingResourceEventRestUrlParser();

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
    public void testIfRestUrlParserMissing() {
        List<Class<?>> controllers = Whitebox.getInternalState(EndpointConfig.class, "CONTROLLERS");
        for (Class<?> controller : controllers) {
            Optional<Class<?>> endpointClass = Arrays.stream(controller.getInterfaces())
                    .filter(interf -> interf.getSimpleName().contains("Endpoint"))
                    .findFirst();
            if (endpointClass.isPresent()) {
                String rootPath = checkAndReturnPath(endpointClass);
                if (Arrays.stream(excludes).parallel().noneMatch(rootPath::contains)) {
                    for (Method method : endpointClass.get().getMethods()) {
                        String methodPath = getMethodPath(rootPath, method);
                        String requestMethod = getRequestMethod(method);
                        if (requestMethod != null) {
                            List<RestUrlParser> matchedParsers = new ArrayList<>();
                            Map<String, String> params = new HashMap<>();
                            for (RestUrlParser restUrlParser : restUrlParsers) {
                                setupMocks(methodPath, requestMethod);
                                if (restUrlParser.fillParams(containerRequestContext, params)) {
                                    matchedParsers.add(restUrlParser);
                                }
                            }
                            LOGGER.info("Matched URL: " + methodPath + "\nslurped params: " + params);
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

    private void setupMocks(String methodPath, String requestMethod) {
        when(containerRequestContext.getMethod()).thenReturn(requestMethod);
        when(uriInfo.getPath()).thenReturn(methodPath.substring(1));
        when(containerRequestContext.getUriInfo()).thenReturn(uriInfo);
    }

    private void checkResourceEvent(String methodPath, Map<String, String> params) {
        String[] parts = methodPath.substring(1).split(SLASH);
        String resourceEvent = null;
        if (parts.length >= 4 && PATH_V_4.equals(parts[0]) && !("audits".equals(parts[2]) && parts[3].matches(ID_REGEX))
                && !(PATH_CREDENTIALS.equals(parts[2]) && PATH_CODE_GRANT_FLOW.equals(parts[3])) && !RESOURCE_NAME.equals(parts[parts.length - 1])) {
            // Skip v4/{workspaceId}/audits/{auditId}
            // Skip v4/{workspaceId}/clusterdefinitions/{name} and similars
            // Skip v4/{workspaceId}/credentials/code_grant_flow/init
            // Skip v4/{workspaceId}/credentials/code_grant_flow/init/{name}
            // Skip v4/{workspaceId}/credentials/code_grant_flow/authorization/{cloudPlatform}
            // Match v4/{workspaceId}/audits/zip and similars
            // Match v4/{workspaceId}/clusterdefinitions/{name}/parameters and similars
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
        } else if ((parts.length == 2 || parts.length == 3 && !parts[2].matches(ID_REGEX)) && PATH_AUTOSCALE.equals(parts[0])) {
            // Skip autoscale/stack/{id}
            // Match autoscale/ambari and autoscale/stack/all
            resourceEvent = parts.length == 2 ? parts[1] : String.join(SLASH, parts[1], parts[2]);
        } else if (parts.length >= 4 && PATH_AUTOSCALE.equals(parts[0])) {
            // Match autoscale/stack/{id}/{userId} and similars
            String[] eventParts = new String[parts.length - 3];
            System.arraycopy(parts, 3, eventParts, 0, eventParts.length);
            resourceEvent = String.join(SLASH, eventParts);
        }

        if (resourceEvent == null) {
            assertNull("params must not contain resource event", params.get(RESOURCE_EVENT));
        } else {
            assertEquals("params should contain resource event", resourceEvent, params.get(RESOURCE_EVENT));
        }
    }

    private void checkResourceType(String methodPath, Map<String, String> params) {
        String[] parts = methodPath.substring(1).split(SLASH);
        String resourceType = null;
        if (parts.length > 0) {
            resourceType = parts[0];
            if (parts.length > 2 && PATH_AUTOSCALE.equals(parts[0]) && parts[2].matches(ID_REGEX)) {
                resourceType = parts[1];
            } else if (parts.length > 1 && PATH_V_4.equals(parts[0])) {
                resourceType = parts.length > 2 && parts[1].matches(ID_REGEX) ? parts[2] : parts[1];
            }
        }
        assertNotNull("Could not determine expected resource type for path: " + methodPath, resourceType);

        assertEquals("params should contain resource type", resourceType, params.get(RESOURCE_TYPE));
    }

    private void checkResourceIdOrName(String methodPath, Map<String, String> params) {
        if (methodPath.contains(RESOURCE_ID)) {
            assertEquals("params should contain resource ID",
                    RESOURCE_ID, params.get(RestUrlParser.RESOURCE_ID));
            assertNull("params must not contain resource name when having a resource ID", params.get(RestUrlParser.RESOURCE_NAME));
        } else if (methodPath.contains(RESOURCE_NAME)) {
            assertEquals("params should contain resource name",
                    RESOURCE_NAME, params.get(RestUrlParser.RESOURCE_NAME));
            assertNull("params must not contain resource ID when having a resource name", params.get(RestUrlParser.RESOURCE_ID));
        } else if (methodPath.contains(WORKSPACE_NAME)) {
            assertEquals("params should contain workspace name",
                    WORKSPACE_NAME, params.get(RestUrlParser.RESOURCE_NAME));
            assertNull("params must not contain resource ID when having a resource name", params.get(RestUrlParser.RESOURCE_ID));
        } else {
            assertNull("params must not contain resource ID", params.get(RestUrlParser.RESOURCE_ID));
            assertNull("params must not contain resource name", params.get(RestUrlParser.RESOURCE_NAME));
        }
    }

    private void checkWorkspaceId(String methodPath, Map<String, String> params) {
        if (methodPath.contains(WORKSPACE_ID)) {
            assertEquals("params should contain workspace ID",
                    WORKSPACE_ID, params.get(RestUrlParser.WORKSPACE_ID));
        }
    }

    private void checkIfMoreThanOneParserDetected(String methodPath, List<RestUrlParser> matchedParsers) {
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
                    methodPath = methodPath.startsWith("/v4/workspaces")
                            ? methodPath.replace('{' + pathParamValue + '}', WORKSPACE_NAME)
                            : methodPath.replace('{' + pathParamValue + '}', RESOURCE_NAME);
                }
            }
        }
        return methodPath;
    }

}