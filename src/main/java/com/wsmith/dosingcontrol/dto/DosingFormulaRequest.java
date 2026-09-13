package com.wsmith.dosingcontrol.dto;

import com.wsmith.dosingcontrol.model.ChemicalType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

/** Body for creating a new draft formula. Version/status/timestamps are assigned server-side. */
public record DosingFormulaRequest(
        @NotNull ChemicalType chemicalType,
        @NotNull BigDecimal baseDose,
        @NotNull BigDecimal minDose,
        @NotNull BigDecimal maxDose,
        @NotBlank String createdBy,
        String notes,
        @NotEmpty @Valid List<FormulaInputDto> inputs
) {
}
