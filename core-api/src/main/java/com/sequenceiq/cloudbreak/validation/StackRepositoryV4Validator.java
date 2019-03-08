package com.sequenceiq.cloudbreak.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari.stackrepository.StackRepositoryV4Request;
import com.sequenceiq.cloudbreak.cloud.VersionComparator;
import com.sequenceiq.cloudbreak.cloud.model.Versioned;

public class StackRepositoryV4Validator implements ConstraintValidator<ValidAmbariStack, StackRepositoryV4Request> {

    public static final String MIN_HDP_VERSION = "2.7";

    @Override
    public void initialize(ValidAmbariStack constraintAnnotation) {
    }

    @Override
    public boolean isValid(StackRepositoryV4Request stackRepo, ConstraintValidatorContext context) {
        if (!repositorySpecificationFieldsExist(stackRepo, context)) {
            return false;
        }

        if ("HDP".equalsIgnoreCase(stackRepo.getStack())) {
            Versioned hdpVersion = () -> stackRepo.getVersion().length() > MIN_HDP_VERSION.length()
                    ? stackRepo.getVersion().substring(0, MIN_HDP_VERSION.length()) : stackRepo.getVersion();
            Versioned minVersion = () -> MIN_HDP_VERSION;
            int compared = new VersionComparator().compare(minVersion, hdpVersion);
            if (compared > 0) {
                context.buildConstraintViolationWithTemplate("HDP version is not valid. Only " + MIN_HDP_VERSION + " and later versions are supported")
                        .addPropertyNode("version")
                        .addConstraintViolation();
                return false;
            }
        }
        return true;
    }

    private boolean repositorySpecificationFieldsExist(StackRepositoryV4Request stackRepo, ConstraintValidatorContext context) {
        boolean vdfFileSpecified = StringUtils.isNoneEmpty(stackRepo.getVersionDefinitionFileUrl());
        boolean mpackSpecified = StringUtils.isNoneEmpty(stackRepo.getMpackUrl())
                || !CollectionUtils.isEmpty(stackRepo.getMpacks());
        boolean repositoriesSpecified = stackRepo.getRepository() != null && StringUtils.isNoneEmpty(stackRepo.getRepository().getBaseUrl())
                && StringUtils.isNoneEmpty(stackRepo.getUtilsBaseURL())
                && StringUtils.isNoneEmpty(stackRepo.getUtilsRepoId());

        if (!vdfFileSpecified && !repositoriesSpecified && !mpackSpecified) {
            buildConstraintValidations(stackRepo, context);
            return false;
        }
        return true;
    }

    private void buildConstraintValidations(StackRepositoryV4Request stackRepo, ConstraintValidatorContext context) {
        boolean anyRepositoryDetailsSpecified = stackRepo.getRepository() != null && StringUtils.isNoneEmpty(stackRepo.getRepository().getBaseUrl())
                || StringUtils.isNoneEmpty(stackRepo.getUtilsBaseURL())
                || StringUtils.isNoneEmpty(stackRepo.getUtilsRepoId());

        if (anyRepositoryDetailsSpecified) {
            if (stackRepo.getRepository() == null || (stackRepo.getRepository() != null && StringUtils.isEmpty(stackRepo.getRepository().getBaseUrl()))) {
                context.buildConstraintViolationWithTemplate("Base URL needs to be specified.")
                        .addPropertyNode("repository")
                        .addPropertyNode("baseUrl")
                        .addConstraintViolation();
            }
            if (StringUtils.isEmpty(stackRepo.getUtilsBaseURL())) {
                context.buildConstraintViolationWithTemplate("Base URL of Utils Repo needs to be specified.")
                        .addPropertyNode("utilsBaseURL")
                        .addConstraintViolation();
            }
            if (StringUtils.isEmpty(stackRepo.getUtilsRepoId())) {
                context.buildConstraintViolationWithTemplate("Id of Utils Repo needs to be specified.")
                        .addPropertyNode("utilsRepoId")
                        .addConstraintViolation();
            }
        } else {
            String messageTemplate = "The 'versionDefinitionFileUrl' or the repository definitions('repository.baseUrl', 'utilsBaseURL', 'utilsRepoId') "
                    + "field(s) should be specified.";
            context.buildConstraintViolationWithTemplate(messageTemplate)
                    .addConstraintViolation();
        }
    }
}
