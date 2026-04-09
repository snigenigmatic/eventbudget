package com.eventbudget.dto;

import jakarta.validation.constraints.NotBlank;

public record SupportingDocumentRequest(
        @NotBlank String fileName,
        @NotBlank String fileType,
        @NotBlank String storageUrl
) {
}
