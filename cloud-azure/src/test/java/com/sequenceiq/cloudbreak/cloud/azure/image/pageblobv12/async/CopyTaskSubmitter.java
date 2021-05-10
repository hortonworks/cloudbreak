package com.sequenceiq.cloudbreak.cloud.azure.image.pageblobv12.async;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;

import com.azure.core.http.HttpHeaders;
import com.azure.core.http.HttpRequest;
import com.azure.core.http.rest.Response;
import com.azure.storage.blob.models.PageBlobItem;
import com.azure.storage.blob.models.PageBlobRequestConditions;
import com.azure.storage.blob.models.PageRange;
import com.azure.storage.blob.specialized.PageBlobAsyncClient;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class CopyTaskSubmitter {

    private final PageBlobAsyncClient destinationClient;

    private final String sourceBlobUrl;

    private final PageBlobRequestConditions destinationRequestConditions;

    public CopyTaskSubmitter(PageBlobAsyncClient destinationClient, String sourceBlobUrl, PageBlobRequestConditions destinationRequestConditions) {
        this.destinationClient = destinationClient;
        this.sourceBlobUrl = sourceBlobUrl;
        this.destinationRequestConditions = destinationRequestConditions;
    }

    public void submitAll(List<PageRange> copyRanges, long fileSize) {
        List<PageRange> submitted = new ArrayList<>();
        List<Mono<Response<PageBlobItem>>> responses = new ArrayList<>();
        List<PageRange> failedRanges = new ArrayList<>();
        List<Throwable> errors = new ArrayList<>();

        Mono<PageBlobItem> resulthere = destinationClient.create(fileSize, true);
//                .doOnError(t -> System.out.println("Error when creating file: " + t))
//                .doOnSuccess(p -> System.out.println("Could create the item: " + p))
//                .subscribe(m -> System.out.println("The destination file is: " + m));

        if(1==1) {
            throw new RuntimeException();
        }
        ResponseConsumer responseConsumer = new ResponseConsumer();
        ErrorConsumer errorConsumer = new ErrorConsumer();
        Flux<Response<PageBlobItem>> flux =
                Flux.fromIterable(copyRanges.subList(0, 5))
//                Flux.just(copyRanges.subList(0, 5))
                        .flatMap(r -> {
                                    submitted.add(r);
                                    Mono<Response<PageBlobItem>> response = sendToServer(r)
                                            .doOnSuccess(resp -> System.out.println("response code is" + resp.getStatusCode()))
                                            .doOnError(e -> {
                                                failedRanges.add(r);
                                                errors.add(e);
                                            })
                                            .onErrorResume(t -> Mono.empty());
                                    responses.add(response);
                                    return response;
                                }
                                , 1, 1);

        flux.subscribe(responseConsumer, errorConsumer);


        System.out.println("Let's check what succeeded, what not");

    }

    private Mono<Response<PageBlobItem>> sendToServer(PageRange r) {
        return destinationClient.uploadPagesFromUrlWithResponse(
                r,
                sourceBlobUrl,
                r.getStart(),
                null,
                destinationRequestConditions,
                null
        );
    }

    private Mono<Response<PageBlobItem>> sendToServerFake(PageRange r) {
        return Mono.just(getResponse(200));
    }

    @NotNull
    private Response<PageBlobItem> getResponse(int status) {
        return new Response<PageBlobItem>() {
            @Override
            public int getStatusCode() {
                return status;
            }

            @Override
            public HttpHeaders getHeaders() {
                return null;
            }

            @Override
            public HttpRequest getRequest() {
                return null;
            }

            @Override
            public PageBlobItem getValue() {
                return null;
            }
        };
    }

    private Mono<Response<PageBlobItem>> sendToServerFakeError(PageRange r) {
        return Mono.error(new NotFoundException(""));
    }


    private static class ResponseConsumer implements Consumer<Response<PageBlobItem>> {

        List<Integer> responseCodes = new ArrayList<>();

        @Override
        public void accept(Response<PageBlobItem> pageBlobItemResponse) {
            responseCodes.add(pageBlobItemResponse.getStatusCode());
        }

        public List<Integer> getResponseCodes() {
            return responseCodes;
        }
    }

    private static class ErrorConsumer implements Consumer<Throwable> {

        List<Throwable> errors = new ArrayList<>();

        @Override
        public void accept(Throwable t) {
            errors.add(t);
        }

        public List<Throwable> getErrors() {
            return errors;
        }
    }

}

