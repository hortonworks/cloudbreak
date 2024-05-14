package com.sequenceiq.cloudbreak.cloud.azure.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.azure.core.http.rest.PagedIterable;
import com.azure.core.http.rest.PagedResponse;
import com.sequenceiq.cloudbreak.cloud.azure.util.AzureExceptionHandler;
import com.sequenceiq.cloudbreak.cloud.azure.util.AzureExceptionHandlerParameters;

public class AzureListResult<T> {

    private final PagedIterable<T> pagedIterable;

    private final AzureExceptionHandler azureExceptionHandler;

    public AzureListResult(PagedIterable<T> pagedIterable, AzureExceptionHandler azureExceptionHandler) {
        this.pagedIterable = Objects.requireNonNull(pagedIterable);
        this.azureExceptionHandler = azureExceptionHandler;
    }

    public Stream<T> getStream() {
        return azureExceptionHandler.handleException(pagedIterable::stream, Stream.of());
    }

    public Stream<T> getStream(AzureExceptionHandlerParameters azureExceptionHandlerParameters) {
        return azureExceptionHandler.handleException(pagedIterable::stream, Stream.of(), azureExceptionHandlerParameters);
    }

    public List<T> getAll() {
        return getStream().collect(Collectors.toList());
    }

    public List<T> getWhile(Predicate<List<T>> predicate) {
        List<T> result = new ArrayList<>();
        return azureExceptionHandler.handleException(() -> {
            for (PagedResponse<T> page : pagedIterable.iterableByPage()) {
                result.addAll(page.getValue());
                if (predicate.test(result)) {
                    return result;
                }
            }
            return result;
        });
    }

}
