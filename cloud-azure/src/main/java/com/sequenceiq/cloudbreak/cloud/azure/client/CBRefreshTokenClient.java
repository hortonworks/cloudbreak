package com.sequenceiq.cloudbreak.cloud.azure.client;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.rest.LogLevel;
import com.microsoft.rest.interceptors.LoggingInterceptor;
import com.microsoft.rest.serializer.JacksonAdapter;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;

import okhttp3.JavaNetAuthenticator;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit.Builder;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;
import retrofit2.http.Path;
import rx.Observable;

public class CBRefreshTokenClient {

    private final RefreshTokenService service;

    public CBRefreshTokenClient(String baseUrl) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder().addInterceptor(new LoggingInterceptor(LogLevel.BODY_AND_HEADERS));
        builder.proxyAuthenticator(new JavaNetAuthenticator());
        service = new Builder()
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(new JacksonAdapter().converterFactory())
                .baseUrl(baseUrl)
                .client(builder.build())
                .build().create(RefreshTokenService.class);
    }

    public AuthenticationResult refreshToken(String tenant, String clientId, String clientSecret, String resource, String refreshToken,
            boolean multipleResourceRefreshToken) {
        try {
            RefreshTokenResult result = service.refreshToken(tenant, clientId, clientSecret, "refresh_token", resource, refreshToken)
                    .toBlocking().single();
            if (result == null) {
                return null;
            }
            return new AuthenticationResult(
                    result.tokenType,
                    result.accessToken,
                    result.refreshToken,
                    result.expiresIn,
                    null,
                    null,
                    multipleResourceRefreshToken);
        } catch (Exception e) {
            String msg = String.format("Could refresh token for application '%s' in tenant '%s' for resource '%s'", clientId, tenant, resource);
            throw new CloudbreakServiceException(msg, e);
        }
    }

    private interface RefreshTokenService {
        @FormUrlEncoded
        @POST("{tenant}/oauth2/token")
        Observable<RefreshTokenResult> refreshToken(
                @Path("tenant") String tenant,
                @Field("client_id") String clientId,
                @Field("client_secret") String clientSecret,
                @Field("grant_type") String grantType,
                @Field("resource") String resource,
                @Field("refresh_token") String refreshToken);
    }

    private static class RefreshTokenResult {

        @JsonProperty("token_type")
        private String tokenType;

        @JsonProperty("expires_in")
        private long expiresIn;

        @JsonProperty("expires_on")
        private Date expiresOn;

        @JsonProperty("access_token")
        private String accessToken;

        @JsonProperty("refresh_token")
        private String refreshToken;
    }
}
