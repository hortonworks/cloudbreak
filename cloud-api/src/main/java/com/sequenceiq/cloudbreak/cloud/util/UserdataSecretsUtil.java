package com.sequenceiq.cloudbreak.cloud.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sequenceiq.cloudbreak.cloud.exception.UserdataSecretsException;

public class UserdataSecretsUtil {

    private static final Pattern PATTERN = Pattern.compile("###SECRETS-START\n(.*)\n###SECRETS-END", Pattern.DOTALL);

    private static final String SECRET_ID_EXPORT_FORMAT = "export USERDATA_SECRET_ID=\"%s\"";

    private UserdataSecretsUtil() {
    }

    public static String replaceSecretsWithSecretId(String userdata, String secretId) {
        Matcher matcher = PATTERN.matcher(userdata);
        if (matcher.find()) {
            return matcher.replaceFirst(String.format(SECRET_ID_EXPORT_FORMAT, secretId));
        } else {
            throw new UserdataSecretsException("The userdata does not match the pattern: " + PATTERN);
        }
    }

    public static String getSecretsSection(String userdata) {
        Matcher matcher = PATTERN.matcher(userdata);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            throw new UserdataSecretsException("The userdata does not match the pattern: " + PATTERN);
        }
    }
}
