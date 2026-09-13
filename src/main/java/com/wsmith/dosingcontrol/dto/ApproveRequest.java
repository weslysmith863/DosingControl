package com.wsmith.dosingcontrol.dto;

import jakarta.validation.constraints.NotBlank;

public record ApproveRequest(
        @NotBlank String approvedBy
) {
}
