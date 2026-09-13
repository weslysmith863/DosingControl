package com.wsmith.dosingcontrol.repository;

import com.wsmith.dosingcontrol.model.ProcessReading;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessReadingRepository extends JpaRepository<ProcessReading, Long> {
}
