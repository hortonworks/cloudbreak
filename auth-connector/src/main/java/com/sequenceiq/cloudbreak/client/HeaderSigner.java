package com.sequenceiq.cloudbreak.client;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import com.sequenceiq.cloudbreak.common.json.JsonUtil;

public abstract class HeaderSigner {

    protected static final String AUTH_METHOD_ED25519 = "ed25519v1";

    protected static final String AUTH_METHOD_ECDSA = "ecdsav1";

    public String authHeader(String accessKeyID, String privateKey, String contentType, String method, String path, String date) {
        return urlsafeMeta(accessKeyID, getAuthMethod()) + "." + urlsafeSignature(privateKey, contentType, method, path, date);
    }

    protected abstract String urlsafeSignature(String privateKey, String contentType, String method, String path, String date);

    protected abstract String getAuthMethod();

    private String urlsafeMeta(String accessKeyID, String authMethod) {
        String metadata = JsonUtil.writeValueAsStringSilent(new AccessKeyAuthMethod(accessKeyID, authMethod));
        return new String(Base64.getUrlEncoder().encode(metadata.getBytes(StandardCharsets.UTF_8)));
    }
}
