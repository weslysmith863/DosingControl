package com.wsmith.dosingcontrol.repository;

import com.wsmith.dosingcontrol.model.AlarmLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlarmLogRepository extends JpaRepository<AlarmLog, Long> {
}
