package com.wsmith.dosingcontrol.service;

import com.wsmith.dosingcontrol.dto.DosingFormulaRequest;
import com.wsmith.dosingcontrol.dto.FormulaInputDto;
import com.wsmith.dosingcontrol.exception.InvalidFormulaStateException;
import com.wsmith.dosingcontrol.exception.ResourceNotFoundException;
import com.wsmith.dosingcontrol.model.ChemicalType;
import com.wsmith.dosingcontrol.model.DosingFormula;
import com.wsmith.dosingcontrol.model.FormulaInput;
import com.wsmith.dosingcontrol.model.FormulaStatus;
import com.wsmith.dosingcontrol.repository.DosingFormulaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class DosingFormulaService {

    private final DosingFormulaRepository dosingFormulaRepository;

    public DosingFormulaService(DosingFormulaRepository dosingFormulaRepository) {
        this.dosingFormulaRepository = dosingFormulaRepository;
    }

    public List<DosingFormula> findAll() {
        return dosingFormulaRepository.findAll();
    }

    public DosingFormula findById(Long id) {
        return dosingFormulaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No formula found with id " + id));
    }

    /** What Ignition will call: the one formula currently live for a given pump/chemical. */
    public DosingFormula findActive(ChemicalType chemicalType) {
        return dosingFormulaRepository.findByChemicalTypeAndStatus(chemicalType, FormulaStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No ACTIVE formula found for chemical type " + chemicalType));
    }

    /** Creates a new DRAFT, auto-incrementing the version number for that chemical type. */
    @Transactional
    public DosingFormula createDraft(DosingFormulaRequest request) {
        int nextVersion = dosingFormulaRepository.findTopByChemicalTypeOrderByVersionDesc(request.chemicalType())
                .map(existing -> existing.getVersion() + 1)
                .orElse(1);

        DosingFormula formula = new DosingFormula(
                request.chemicalType(),
                nextVersion,
                FormulaStatus.DRAFT,
                request.baseDose(),
                request.minDose(),
                request.maxDose(),
                request.createdBy()
        );
        formula.setNotes(request.notes());

        for (FormulaInputDto inputDto : request.inputs()) {
            formula.addInput(new FormulaInput(inputDto.readingTag(), inputDto.coefficient()));
        }

        return dosingFormulaRepository.save(formula);
    }

    /** DRAFT -> APPROVED. Does not touch any other formula. */
    @Transactional
    public DosingFormula approve(Long id, String approvedBy) {
        DosingFormula formula = findById(id);

        if (formula.getStatus() != FormulaStatus.DRAFT) {
            throw new InvalidFormulaStateException(
                    "Formula " + id + " is " + formula.getStatus() + ", not DRAFT — cannot approve.");
        }

        formula.setStatus(FormulaStatus.APPROVED);
        formula.setApprovedBy(approvedBy);
        formula.setApprovedAt(Instant.now());
        return dosingFormulaRepository.save(formula);
    }

    /**
     * APPROVED -> ACTIVE. Retires whatever formula was previously ACTIVE for the same
     * chemical type in the same transaction, so there's never more than one ACTIVE row
     * per chemical type at a time — that invariant isn't enforced by the database schema,
     * only here.
     */
    @Transactional
    public DosingFormula activate(Long id) {
        DosingFormula formula = findById(id);

        if (formula.getStatus() != FormulaStatus.APPROVED) {
            throw new InvalidFormulaStateException(
                    "Formula " + id + " is " + formula.getStatus() + ", not APPROVED — cannot activate.");
        }

        dosingFormulaRepository.findByChemicalTypeAndStatus(formula.getChemicalType(), FormulaStatus.ACTIVE)
                .ifPresent(previouslyActive -> {
                    previouslyActive.setStatus(FormulaStatus.RETIRED);
                    dosingFormulaRepository.save(previouslyActive);
                });

        formula.setStatus(FormulaStatus.ACTIVE);
        return dosingFormulaRepository.save(formula);
    }
}
