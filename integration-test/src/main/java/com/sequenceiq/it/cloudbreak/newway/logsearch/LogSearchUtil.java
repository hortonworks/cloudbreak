package com.sequenceiq.it.cloudbreak.newway.logsearch;

import static com.sequenceiq.it.cloudbreak.newway.CloudbreakTest.LOG_SEARCH_QUERY_TYPES;
import static com.sequenceiq.it.cloudbreak.newway.CloudbreakTest.LOG_SEARCH_URL_PREFIX;

import java.util.List;
import java.util.Optional;

import org.assertj.core.util.Lists;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.it.IntegrationTestContext;

public class LogSearchUtil {

    public static final String LOG_SEARCH_CBID_QUERY_TYPE = "cbid";

    public static final Long LOG_SEARCH_CBID_ID = 1L;

    public static final String LOG_SEARCH_CBNAME_QUERY_TYPE = "cbname";

    public static final String LOG_SEARCH_COMPONENT_QUERY_TYPE = "component";

    private LogSearchUtil() {

    }

    public static void addQueryModelForLogSearchUrlToContext(IntegrationTestContext context, Long id, String type, String value) {
        ((List<QueryType>) context.getContextParam(LOG_SEARCH_QUERY_TYPES, List.class))
                .stream()
                .filter(logSearchQueryType -> org.apache.commons.lang3.StringUtils.equals(logSearchQueryType.getId(), type))
                .findFirst()
                .ifPresent(queryType -> context.putContextParam(type, QueryModel.create(id, queryType, value)));
    }

    public static Optional<String> createLogSearchUrl(IntegrationTestContext context) {
        List<QueryModel> queryModels = Lists.newArrayList();
        addToQueryModelByType(context, queryModels, LOG_SEARCH_CBID_QUERY_TYPE);
        addToQueryModelByType(context, queryModels, LOG_SEARCH_CBNAME_QUERY_TYPE);
        addToQueryModelByType(context, queryModels, LOG_SEARCH_COMPONENT_QUERY_TYPE);
        if (!queryModels.isEmpty()) {
            Optional<String> urlQueryString = UrlParameters.getUrlQueryString(queryModels, new ObjectMapper());
            return urlQueryString.isPresent() ? Optional.of(context.getContextParam(LOG_SEARCH_URL_PREFIX) + urlQueryString.get()) : Optional.empty();
        }
        return Optional.empty();
    }

    private static void addToQueryModelByType(IntegrationTestContext context, List<QueryModel> queryModels, String logSearchQueryType) {
        if (context.getContextParam(logSearchQueryType, QueryModel.class) != null) {
            queryModels.add(context.getContextParam(logSearchQueryType, QueryModel.class));
        }
    }

}
