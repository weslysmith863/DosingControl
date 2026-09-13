package com.wsmith.dosingcontrol.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

/** eventTime is optional — if omitted, the server stamps it with the time the request arrived. */
public record AlarmLogRequest(
        @NotBlank String alarmName,
        @NotBlank String priority,
        @NotBlank String state,
        Instant eventTime,
        String sourcePath
) {
}
