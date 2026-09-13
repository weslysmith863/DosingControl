package com.wsmith.dosingcontrol.controller;

import com.wsmith.dosingcontrol.dto.ProcessReadingRequest;
import com.wsmith.dosingcontrol.model.ProcessReading;
import com.wsmith.dosingcontrol.repository.ProcessReadingRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

/**
 * Receives QC/process values pushed from Ignition (via a Gateway timer script or
 * Transaction Group). Simple enough — just persisting an incoming value — that it
 * talks to the repository directly rather than through a service layer; there's no
 * business rule here the way there is for the formula workflow.
 */
@RestController
@RequestMapping("/api/readings")
public class ProcessReadingController {

    private final ProcessReadingRepository processReadingRepository;

    public ProcessReadingController(ProcessReadingRepository processReadingRepository) {
        this.processReadingRepository = processReadingRepository;
    }

    @GetMapping
    public List<ProcessReading> findAll() {
        return processReadingRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<ProcessReading> push(@Valid @RequestBody ProcessReadingRequest request) {
        Instant readingTime = request.readingTime() != null ? request.readingTime() : Instant.now();
        ProcessReading saved = processReadingRepository.save(
                new ProcessReading(request.tagName(), request.value(), readingTime));
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
}
