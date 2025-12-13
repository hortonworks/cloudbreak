package com.sequenceiq.it.cloudbreak.assertion.util;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sequenceiq.cloudbreak.api.endpoint.v4.util.requests.RepoConfigValidationV4Request;
import com.sequenceiq.it.cloudbreak.assertion.CommonAssert;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.util.RepoConfigValidationTestDto;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public enum RepoConfigValidationTestAssertion {

    AMBARI_BASE_URL {
        @Override
        public RepoConfigValidationV4Request request() {
            RepoConfigValidationV4Request request = new RepoConfigValidationV4Request();
            request.setAmbariBaseUrl(DEFAULT_URL_VALUE);
            return request;
        }

        @Override
        public RepoConfigValidationTestDto resultValidation(TestContext tc, RepoConfigValidationTestDto entity, CloudbreakClient cc) {
            CommonAssert.responseExists(tc, entity, cc);
            assertTrue(entity.getResponse().getAmbariBaseUrl());

            assertNull(entity.getResponse().getMpackUrl());
            assertNull(entity.getResponse().getStackBaseURL());
            assertNull(entity.getResponse().getUtilsBaseURL());
            assertNull(entity.getResponse().getAmbariGpgKeyUrl());
            assertNull(entity.getResponse().getVersionDefinitionFileUrl());
            return entity;
        }
    },

    AMBARI_REPO_GPG_KEY {
        @Override
        public RepoConfigValidationV4Request request() {
            RepoConfigValidationV4Request request = new RepoConfigValidationV4Request();
            request.setAmbariGpgKeyUrl(DEFAULT_URL_VALUE);
            return request;
        }

        @Override
        public RepoConfigValidationTestDto resultValidation(TestContext tc, RepoConfigValidationTestDto entity, CloudbreakClient cc) {
            CommonAssert.responseExists(tc, entity, cc);
            assertTrue(entity.getResponse().getAmbariGpgKeyUrl());

            assertNull(entity.getResponse().getMpackUrl());
            assertNull(entity.getResponse().getStackBaseURL());
            assertNull(entity.getResponse().getUtilsBaseURL());
            assertNull(entity.getResponse().getAmbariBaseUrl());
            assertNull(entity.getResponse().getVersionDefinitionFileUrl());
            return entity;
        }
    },

    STACK_BASE_URL {
        @Override
        public RepoConfigValidationV4Request request() {
            RepoConfigValidationV4Request request = new RepoConfigValidationV4Request();
            request.setStackBaseURL(DEFAULT_URL_VALUE);
            return request;
        }

        @Override
        public RepoConfigValidationTestDto resultValidation(TestContext tc, RepoConfigValidationTestDto entity, CloudbreakClient cc) {
            CommonAssert.responseExists(tc, entity, cc);
            assertTrue(entity.getResponse().getStackBaseURL());

            assertNull(entity.getResponse().getMpackUrl());
            assertNull(entity.getResponse().getUtilsBaseURL());
            assertNull(entity.getResponse().getAmbariBaseUrl());
            assertNull(entity.getResponse().getAmbariGpgKeyUrl());
            assertNull(entity.getResponse().getVersionDefinitionFileUrl());
            return entity;
        }
    },

    UTILS_BASE_URL {
        @Override
        public RepoConfigValidationV4Request request() {
            RepoConfigValidationV4Request request = new RepoConfigValidationV4Request();
            request.setUtilsBaseURL(DEFAULT_URL_VALUE);
            return request;
        }

        @Override
        public RepoConfigValidationTestDto resultValidation(TestContext tc, RepoConfigValidationTestDto entity, CloudbreakClient cc) {
            CommonAssert.responseExists(tc, entity, cc);
            assertTrue(entity.getResponse().getUtilsBaseURL());

            assertNull(entity.getResponse().getMpackUrl());
            assertNull(entity.getResponse().getStackBaseURL());
            assertNull(entity.getResponse().getAmbariBaseUrl());
            assertNull(entity.getResponse().getAmbariGpgKeyUrl());
            assertNull(entity.getResponse().getVersionDefinitionFileUrl());
            return entity;
        }
    },

    VDF_URL {
        @Override
        public RepoConfigValidationV4Request request() {
            RepoConfigValidationV4Request request = new RepoConfigValidationV4Request();
            request.setVersionDefinitionFileUrl(DEFAULT_URL_VALUE);
            return request;
        }

        @Override
        public RepoConfigValidationTestDto resultValidation(TestContext tc, RepoConfigValidationTestDto entity, CloudbreakClient cc) {
            CommonAssert.responseExists(tc, entity, cc);
            assertTrue(entity.getResponse().getVersionDefinitionFileUrl());

            assertNull(entity.getResponse().getMpackUrl());
            assertNull(entity.getResponse().getStackBaseURL());
            assertNull(entity.getResponse().getUtilsBaseURL());
            assertNull(entity.getResponse().getAmbariBaseUrl());
            assertNull(entity.getResponse().getAmbariGpgKeyUrl());
            return entity;
        }
    },

    MPACK_URL {
        @Override
        public RepoConfigValidationV4Request request() {
            RepoConfigValidationV4Request request = new RepoConfigValidationV4Request();
            request.setMpackUrl(DEFAULT_URL_VALUE);
            return request;
        }

        @Override
        public RepoConfigValidationTestDto resultValidation(TestContext tc, RepoConfigValidationTestDto entity, CloudbreakClient cc) {
            CommonAssert.responseExists(tc, entity, cc);
            assertTrue(entity.getResponse().getMpackUrl());

            assertNull(entity.getResponse().getStackBaseURL());
            assertNull(entity.getResponse().getUtilsBaseURL());
            assertNull(entity.getResponse().getAmbariBaseUrl());
            assertNull(entity.getResponse().getAmbariGpgKeyUrl());
            assertNull(entity.getResponse().getVersionDefinitionFileUrl());
            return entity;
        }
    },

    NONE {
        @Override
        public RepoConfigValidationV4Request request() {
            return new RepoConfigValidationV4Request();
        }

        @Override
        public RepoConfigValidationTestDto resultValidation(TestContext tc, RepoConfigValidationTestDto entity, CloudbreakClient cc) {
            assertNull(entity.getResponse().getMpackUrl());
            assertNull(entity.getResponse().getStackBaseURL());
            assertNull(entity.getResponse().getUtilsBaseURL());
            assertNull(entity.getResponse().getAmbariBaseUrl());
            assertNull(entity.getResponse().getAmbariGpgKeyUrl());
            assertNull(entity.getResponse().getVersionDefinitionFileUrl());
            return entity;
        }
    };

    private static final String DEFAULT_URL_VALUE = "http://someurl.com";

    public abstract RepoConfigValidationV4Request request();

    public abstract RepoConfigValidationTestDto resultValidation(TestContext tc, RepoConfigValidationTestDto entity, CloudbreakClient cc);

}
