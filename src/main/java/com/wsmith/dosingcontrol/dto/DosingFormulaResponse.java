package com.wsmith.dosingcontrol.dto;

import com.wsmith.dosingcontrol.model.ChemicalType;
import com.wsmith.dosingcontrol.model.DosingFormula;
import com.wsmith.dosingcontrol.model.FormulaStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record DosingFormulaResponse(
        Long id,
        ChemicalType chemicalType,
        Integer version,
        FormulaStatus status,
        BigDecimal baseDose,
        BigDecimal minDose,
        BigDecimal maxDose,
        String createdBy,
        String approvedBy,
        Instant createdAt,
        Instant approvedAt,
        String notes,
        List<FormulaInputDto> inputs
) {
    /** Maps the JPA entity to the wire format — keeps entities from leaking straight out over REST. */
    public static DosingFormulaResponse from(DosingFormula formula) {
        List<FormulaInputDto> inputDtos = formula.getInputs().stream()
                .map(input -> new FormulaInputDto(input.getReadingTag(), input.getCoefficient()))
                .toList();

        return new DosingFormulaResponse(
                formula.getId(),
                formula.getChemicalType(),
                formula.getVersion(),
                formula.getStatus(),
                formula.getBaseDose(),
                formula.getMinDose(),
                formula.getMaxDose(),
                formula.getCreatedBy(),
                formula.getApprovedBy(),
                formula.getCreatedAt(),
                formula.getApprovedAt(),
                formula.getNotes(),
                inputDtos
        );
    }
}
