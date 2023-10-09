package com.sequenceiq.cloudbreak.exception;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.exception.mapper.BaseExceptionMapper;

@Component
public class ErrorResponseHandler {

    private static final String COMMON_EXCEPTION_MAPPER_PACKAGE = "com.sequenceiq.cloudbreak.exception.mapper";

    @Inject
    private List<BaseExceptionMapper<?>> exceptionMappers;

    private Map<Class, BaseExceptionMapper<?>> exceptionMapperMap;

    @PostConstruct
    public void init() {
        this.exceptionMapperMap = exceptionMappers.stream().collect(Collectors.toMap(BaseExceptionMapper::getExceptionType, Function.identity(),
                (first, second) -> first.getClass().getPackageName().contains(COMMON_EXCEPTION_MAPPER_PACKAGE) ? second : first));
    }

    public void handleErrorResponse(HttpServletResponse response, Exception exception) throws IOException {
        BaseExceptionMapper exceptionMapper = get(exception);
        response.setContentType(MediaType.APPLICATION_JSON);
        if (exceptionMapper != null) {
            response.setStatus(exceptionMapper.getResponseStatus(exception).getStatusCode());
            PrintWriter writer = response.getWriter();
            writer.write(JsonUtil.writeValueAsStringSilent(exceptionMapper.getEntity(exception)));
            writer.flush();
        } else {
            response.setStatus(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
            response.getWriter().flush();
        }
    }

    private BaseExceptionMapper get(Exception exception) {
        return exceptionMapperMap.containsKey(exception.getClass()) ? exceptionMapperMap.get(exception.getClass()) : exceptionMapperMap.get(Exception.class);
    }
}
