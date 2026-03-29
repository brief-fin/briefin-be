package com.briefin.domain.disclosures.event;

public record DisclosureSavedEvent(
        Long companyId,
        Long disclosureId,
        String companyName,
        String reportName,
        String category
) {
}