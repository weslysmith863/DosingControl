package com.wsmith.dosingcontrol.controller;

import com.wsmith.dosingcontrol.dto.ApproveRequest;
import com.wsmith.dosingcontrol.dto.DosingFormulaRequest;
import com.wsmith.dosingcontrol.dto.DosingFormulaResponse;
import com.wsmith.dosingcontrol.model.ChemicalType;
import com.wsmith.dosingcontrol.service.DosingFormulaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/formulas")
public class DosingFormulaController {

    private final DosingFormulaService dosingFormulaService;

    public DosingFormulaController(DosingFormulaService dosingFormulaService) {
        this.dosingFormulaService = dosingFormulaService;
    }

    @GetMapping
    public List<DosingFormulaResponse> findAll() {
        return dosingFormulaService.findAll().stream()
                .map(DosingFormulaResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public DosingFormulaResponse findById(@PathVariable Long id) {
        return DosingFormulaResponse.from(dosingFormulaService.findById(id));
    }

    /** This is the endpoint Ignition will poll to get the setpoint-calculation inputs for a pump. */
    @GetMapping("/active/{chemicalType}")
    public DosingFormulaResponse findActive(@PathVariable ChemicalType chemicalType) {
        return DosingFormulaResponse.from(dosingFormulaService.findActive(chemicalType));
    }

    @PostMapping
    public ResponseEntity<DosingFormulaResponse> createDraft(@Valid @RequestBody DosingFormulaRequest request) {
        DosingFormulaResponse response = DosingFormulaResponse.from(dosingFormulaService.createDraft(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}/approve")
    public DosingFormulaResponse approve(@PathVariable Long id, @Valid @RequestBody ApproveRequest request) {
        return DosingFormulaResponse.from(dosingFormulaService.approve(id, request.approvedBy()));
    }

    @PutMapping("/{id}/activate")
    public DosingFormulaResponse activate(@PathVariable Long id) {
        return DosingFormulaResponse.from(dosingFormulaService.activate(id));
    }
}
