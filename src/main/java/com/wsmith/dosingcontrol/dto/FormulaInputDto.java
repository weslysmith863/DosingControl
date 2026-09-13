package com.wsmith.dosingcontrol.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** One driving-reading + coefficient pair. Used both to submit and to return formula inputs. */
public record FormulaInputDto(
        @NotBlank String readingTag,
        @NotNull BigDecimal coefficient
) {
}
