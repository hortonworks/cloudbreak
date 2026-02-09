package com.sequenceiq.cloudbreak.cmtemplate.utils;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.json.JsonUtil;

@Component
public class KerberosAuthToLocalUtils {

    public String generateForTrustedRealm(String realm) {
        return String.format("""
                RULE:[1:$1@$0](.*@\\Q%1$s\\E$)s/@\\Q%1$s\\E$//
                RULE:[2:$1@$0](.*@\\Q%1$s\\E$)s/@\\Q%1$s\\E$//
                DEFAULT""", realm);
    }

    public String generateEscapedForTrustedRealm(String realm) {
        String authToLocalForTrustedRealm = generateForTrustedRealm(realm);
        String jsonStringValue = JsonUtil.writeValueAsStringSilent(authToLocalForTrustedRealm);
        return jsonStringValue.substring(1, jsonStringValue.length() - 1);
    }
}
