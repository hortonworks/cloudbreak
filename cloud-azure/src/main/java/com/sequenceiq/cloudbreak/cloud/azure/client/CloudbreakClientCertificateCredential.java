package com.sequenceiq.cloudbreak.cloud.azure.client;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;
import com.azure.core.util.logging.ClientLogger;
import com.azure.identity.implementation.IdentityClient;
import com.azure.identity.implementation.util.LoggingUtil;

import reactor.core.publisher.Mono;

/**
 * Azure SDK's {@link com.azure.identity.ClientCertificateCredentialBuilder} doesn't allow the developer to pass the certificate,
 * it only allows a file path to the certificate. We don't store the certificate in files but in vault, and we would like to pass
 * it as a string to the credential. This class allows it. Copying the {@link com.azure.identity.ClientCertificateCredentialBuilder}
 * class doesn't work because it workes by reading the provided InputStream multiple times and the problem is that for the second
 * read it will return an empty array, that's why the current implementation creates only one IdentyClient and uses it.
 */
public class CloudbreakClientCertificateCredential implements TokenCredential {

    private static final ClientLogger LOGGER = new ClientLogger(com.azure.identity.ClientCertificateCredential.class);

    private final IdentityClient identityClient;

    public CloudbreakClientCertificateCredential(IdentityClient identityClient) {
        this.identityClient = identityClient;
    }

    @Override
    public Mono<AccessToken> getToken(TokenRequestContext request) {
        return identityClient.authenticateWithConfidentialClientCache(request)
                .onErrorResume(t -> Mono.empty())
                .switchIfEmpty(Mono.defer(() -> identityClient.authenticateWithConfidentialClient(request)))
                .doOnNext(token -> LoggingUtil.logTokenSuccess(LOGGER, request))
                .doOnError(error -> LoggingUtil.logTokenError(LOGGER, identityClient.getIdentityClientOptions(), request, error));
    }

    @Override
    public AccessToken getTokenSync(TokenRequestContext request) {
        return getToken(request).block();
    }
}
