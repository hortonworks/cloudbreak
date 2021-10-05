package com.sequenceiq.environment.environment.validation.validators;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.validation.ValidationResult;

@Component
public class EncryptionKeyArnValidator {

    private static final Pattern ENCRYPTION_KEY_ARN_PATTERN = Pattern.compile("^arn:(aws|aws-cn|aws-us-gov):kms:[a-zA-Z0-9-]+:[0-9]+:" +
            "key/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    public ValidationResult validateEncryptionKeyArn(String encryptionKeyArn) {
        ValidationResult.ValidationResultBuilder validationResultBuilder = ValidationResult.builder();
        Matcher matcher = ENCRYPTION_KEY_ARN_PATTERN.matcher(encryptionKeyArn.trim());
        if (!matcher.matches()) {
            validationResultBuilder.error(String.format("The identifier of the AWS Key Management Service (AWS KMS)" +
                    "customer master key (CMK) to use for Amazon EBS encryption.%n" +
                    "You can specify the key ARN in the below format:%n" +
                    "Key ARN: arn:partition:service:region:account-id:resource-type/resource-id. " +
                    "For example, arn:aws:kms:us-east-1:012345678910:key/1234abcd-12ab-34cd-56ef-1234567890ab.%n"));
        }
        return validationResultBuilder.build();
    }
}