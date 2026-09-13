package com.wsmith.dosingcontrol.repository;

import com.wsmith.dosingcontrol.model.ChemicalType;
import com.wsmith.dosingcontrol.model.DosingFormula;
import com.wsmith.dosingcontrol.model.FormulaStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DosingFormulaRepository extends JpaRepository<DosingFormula, Long> {

    /**
     * Used by the Ignition-facing endpoint to fetch the live formula for a pump.
     * Only one ACTIVE row per chemical type is expected at a time — that rule
     * should be enforced in the service layer when a new version is activated
     * (flip the previous ACTIVE row to RETIRED in the same transaction).
     */
    Optional<DosingFormula> findByChemicalTypeAndStatus(ChemicalType chemicalType, FormulaStatus status);

    /** Used to compute the next version number when a new draft is created. */
    Optional<DosingFormula> findTopByChemicalTypeOrderByVersionDesc(ChemicalType chemicalType);
}
