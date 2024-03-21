package com.sequenceiq.cloudbreak.common.exception.writer;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.exception.ExceptionResponse;

@Component
@Provider
@Produces(MediaType.TEXT_PLAIN)
public class ExceptionResponseWriter implements MessageBodyWriter<ExceptionResponse> {

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return ExceptionResponse.class.isAssignableFrom(type);
    }

    @Override
    public void writeTo(ExceptionResponse exceptionResponse, Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        String response = exceptionResponse.getMessage();
        entityStream.write(response.getBytes());
    }
}
