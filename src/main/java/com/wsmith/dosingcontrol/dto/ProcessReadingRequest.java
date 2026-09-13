package com.wsmith.dosingcontrol.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

/** readingTime is optional — if omitted, the server stamps it with the time the request arrived. */
public record ProcessReadingRequest(
        @NotBlank String tagName,
        @NotNull BigDecimal value,
        Instant readingTime
) {
}
