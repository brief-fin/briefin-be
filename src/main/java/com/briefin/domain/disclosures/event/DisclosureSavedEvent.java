package com.briefin.domain.disclosures.event;

public record DisclosureSavedEvent(
        Long companyId,
        String companyName,
        String reportName
) {
}