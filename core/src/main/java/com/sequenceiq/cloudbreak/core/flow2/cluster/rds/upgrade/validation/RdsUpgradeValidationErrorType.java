package com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.validation;

import com.sequenceiq.cloudbreak.util.DocumentationLinkProvider;

public enum RdsUpgradeValidationErrorType {
    CONNECTION_ERROR("psql: could not connect to server: Connection timed out", DocumentationLinkProvider.azureFlexibleServerTroubleShootingLink());

    private final String errorMsg;

    private final String documentationLink;

    RdsUpgradeValidationErrorType(String errorMsg, String documentationLink) {
        this.errorMsg = errorMsg;
        this.documentationLink = documentationLink;
    }

    public boolean isErrorMsgMatching(String errorMessage) {
        return errorMessage.contains(errorMsg);
    }

    public String getDocumentationLink() {
        return documentationLink;
    }
}
