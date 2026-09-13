package com.wsmith.dosingcontrol.model;

import com.wsmith.dosingcontrol.converter.ChicagoInstantConverter;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "process_readings")
public class ProcessReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tag_name", nullable = false)
    private String tagName;

    @Column(nullable = false, precision = 14, scale = 4)
    private BigDecimal value;

    @Convert(converter = ChicagoInstantConverter.class)
    @Column(name = "reading_time", nullable = false)
    private Instant readingTime;

    protected ProcessReading() {
        // JPA
    }

    public ProcessReading(String tagName, BigDecimal value, Instant readingTime) {
        this.tagName = tagName;
        this.value = value;
        this.readingTime = readingTime;
    }

    public Long getId() { return id; }

    public String getTagName() { return tagName; }
    public void setTagName(String tagName) { this.tagName = tagName; }

    public BigDecimal getValue() { return value; }
    public void setValue(BigDecimal value) { this.value = value; }

    public Instant getReadingTime() { return readingTime; }
    public void setReadingTime(Instant readingTime) { this.readingTime = readingTime; }
}
