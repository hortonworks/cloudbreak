package com.sequenceiq.cloudbreak.auth.altus.exception;

import jakarta.ws.rs.core.Response;

public class CustomResponseStatus implements Response.StatusType {

        private final int statusCode;

        private final Response.Status.Family family;

        private final String reason;

        public CustomResponseStatus(int statusCode, Response.Status.Family family, String reason) {
            this.statusCode = statusCode;
            this.family = family;
            this.reason = reason;
        }

        @Override
        public int getStatusCode() {
            return statusCode;
        }

        @Override
        public Response.Status.Family getFamily() {
            return family;
        }

        @Override
        public String getReasonPhrase() {
            return reason;
        }
    }