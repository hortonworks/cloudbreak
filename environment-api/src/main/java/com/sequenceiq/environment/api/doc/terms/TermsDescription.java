package com.sequenceiq.environment.api.doc.terms;

public class TermsDescription {

    public static final String GET = "get terms acceptance setting for this account";

    public static final String GET_NOTES = "Get the various terms acceptance setting for this account, e.g. Azure Marketplace terms acceptance " +
            "or Azure default outbound terms acceptance.";

    public static final String PUT = "update terms acceptance setting for this account";

    public static final String PUT_NOTES = "Modify the various terms acceptance setting for this account, e.g. Azure Marketplace terms acceptance " +
            "or Azure default outbound terms acceptance.";

    private TermsDescription() {
    }
}
