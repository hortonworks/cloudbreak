package com.sequenceiq.cloudbreak.structuredevent.rest.urlparser;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.container.ContainerRequestContext;

/**
 * Extracts URL parameters from a {@code ContainerRequestContext} according to each of the abstract {@code getResource*} methods.
 *
 * The main use of this class is with a single call to {@code fillParams}.
 * Parameters are pulled from the request context and placed into the provided Map.
 *
 * Implementers should override:
 * <ul>
 *     <li>{@code getPattern}</li>
 *     <li>{@code parsedMethods} if they wish to support a different set of HTTP methods</li>
 *     <li>Each of the {@code getResource*} methods</li>
 * </ul>
 *
 * Implementations of the {@code getResource*} methods may return null -- in this case,
 * the parameter map will not be populated with the associated key-value pair.
 */
public abstract class CDPRestUrlParser {

    //todo: make these an enum of ParameterKeys
    public static final String RESOURCE_TYPE = "RESOURCE_TYPE";

    public static final String RESOURCE_ID = "RESOURCE_ID";

    public static final String RESOURCE_NAME = "RESOURCE_NAME";

    public static final String RESOURCE_CRN = "RESOURCE_CRN";

    public static final String RESOURCE_EVENT = "RESOURCE_EVENT";

    public static final String ID_TYPE = "ID_TYPE";

    public String getUrl(ContainerRequestContext requestContext) {
        return requestContext.getUriInfo().getPath();
    }

    private String getMethod(ContainerRequestContext requestContext) {
        return requestContext.getMethod();
    }

    /**
     * Fills a parameters Map with values from the request context if it's one of the allowed methods.
     * @param requestContext request context to check for HTTP method and URL parameters
     * @param params Map which is populated with parameters whose keys are one of the public constants of this class
     * @return true if parameter filling succeeded, false otherwise
     */
    public boolean fillParams(ContainerRequestContext requestContext, Map<String, String> params) {
        if (parsedMethods().contains(getMethod(requestContext))) {
            return fillParams(getUrl(requestContext), params);
        } else {
            return false;
        }
    }

    /**
     * Extracts values from the provided URL and places them in the {@code params} Map.
     *
     * @param url the url to extract values from
     * @param params the Map object to add extracted parameters
     * @return true if parameters were put in the params Map, false otherwise.
     */
    private boolean fillParams(String url, Map<String, String> params) {
        if (checkAntiPattern(url)) {
            return false;
        }

        Pattern pattern = getPattern();
        Matcher matcher = pattern.matcher(url);
        if (matcher.matches()) {
            params.put(RESOURCE_NAME, getResourceName(matcher));
            params.put(RESOURCE_CRN, getResourceCrn(matcher));
            params.put(RESOURCE_ID, getResourceId(matcher));
            params.put(RESOURCE_TYPE, getResourceType(matcher));
            params.put(RESOURCE_EVENT, getResourceEvent(matcher));
            params.put(ID_TYPE, getIdType(matcher));
            return true;
        } else {
            return false;
        }
    }

    // as far as I can tell, antipattern is always null, where do we use this?
    private boolean checkAntiPattern(String url) {
        Pattern antiPattern = getAntiPattern();
        if (antiPattern != null) {
            Matcher antiMatcher = antiPattern.matcher(url);
            return antiMatcher.matches();
        }
        return false;
    }

    protected List<String> parsedMethods() {
        return List.of("DELETE", "POST", "PUT", "GET");
    }

    /**
     * The Pattern used to extract URL parameters.
     */
    protected abstract Pattern getPattern();

    // as far as I can tell, this is always null
    protected Pattern getAntiPattern() {
        return null;
    }

    // todo: the only required implementation looks to be getResourceName, all the others can return null.
    // provide implementations here for the others, but keep getResourceName abstract
    protected abstract String getResourceName(Matcher matcher);

    protected abstract String getResourceCrn(Matcher matcher);

    protected abstract String getResourceId(Matcher matcher);

    protected abstract String getResourceType(Matcher matcher);

    protected abstract String getResourceEvent(Matcher matcher);

    protected abstract String getIdType(Matcher matcher);

}
