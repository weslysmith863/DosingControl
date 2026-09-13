package com.wsmith.dosingcontrol.model;

import com.wsmith.dosingcontrol.converter.ChicagoInstantConverter;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "alarm_log")
public class AlarmLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "alarm_name", nullable = false)
    private String alarmName;

    @Column(nullable = false)
    private String priority;

    @Column(nullable = false)
    private String state;

    @Convert(converter = ChicagoInstantConverter.class)
    @Column(name = "event_time", nullable = false)
    private Instant eventTime;

    @Column(name = "source_path")
    private String sourcePath;

    protected AlarmLog() {
        // JPA
    }

    public AlarmLog(String alarmName, String priority, String state, Instant eventTime, String sourcePath) {
        this.alarmName = alarmName;
        this.priority = priority;
        this.state = state;
        this.eventTime = eventTime;
        this.sourcePath = sourcePath;
    }

    public Long getId() { return id; }

    public String getAlarmName() { return alarmName; }
    public void setAlarmName(String alarmName) { this.alarmName = alarmName; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public Instant getEventTime() { return eventTime; }
    public void setEventTime(Instant eventTime) { this.eventTime = eventTime; }

    public String getSourcePath() { return sourcePath; }
    public void setSourcePath(String sourcePath) { this.sourcePath = sourcePath; }
}
