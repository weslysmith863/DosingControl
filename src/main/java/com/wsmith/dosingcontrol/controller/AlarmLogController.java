package com.wsmith.dosingcontrol.controller;

import com.wsmith.dosingcontrol.dto.AlarmLogRequest;
import com.wsmith.dosingcontrol.model.AlarmLog;
import com.wsmith.dosingcontrol.repository.AlarmLogRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

/** Receives alarm events pushed from Ignition's Alarm Pipeline, for the persisted compliance/alarm log. */
@RestController
@RequestMapping("/api/alarms")
public class AlarmLogController {

    private final AlarmLogRepository alarmLogRepository;

    public AlarmLogController(AlarmLogRepository alarmLogRepository) {
        this.alarmLogRepository = alarmLogRepository;
    }

    @GetMapping
    public List<AlarmLog> findAll() {
        return alarmLogRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<AlarmLog> push(@Valid @RequestBody AlarmLogRequest request) {
        Instant eventTime = request.eventTime() != null ? request.eventTime() : Instant.now();
        AlarmLog saved = alarmLogRepository.save(new AlarmLog(
                request.alarmName(), request.priority(), request.state(), eventTime, request.sourcePath()));
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
}
