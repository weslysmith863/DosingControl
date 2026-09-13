package com.wsmith.dosingcontrol.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Converts between java.time.Instant (used everywhere in application code -
 * correct, UTC, timezone-neutral) and java.time.LocalDateTime (what actually
 * gets written to / read from the datetime(6) columns in MySQL:
 * process_readings.reading_time, alarm_log.event_time,
 * dosing_formulas.created_at / approved_at).
 *
 * Why this exists: MySQL's DATETIME type has no timezone concept - whatever
 * wall-clock digits get sent are exactly what's stored and read back, with
 * zero conversion by MySQL. Relying on hibernate.jdbc.time_zone /
 * Connector-J's connectionTimeZone to do that Instant -> local-digits
 * conversion turned out to be unreliable for Instant-typed columns in this
 * Hibernate/Connector-J combination (verified empirically: still writing
 * UTC-shaped digits after configuring both). Doing the conversion explicitly
 * here removes any dependency on JVM default timezone, Hibernate version
 * quirks, or JDBC driver defaults - it's just deterministic code that always
 * does the same thing regardless of environment.
 *
 * The plant / this whole app treats "wall clock" as America/Chicago,
 * matching what MySQL's own SELECT NOW() returns - so all stored DATETIME
 * digits are meant to be Chicago local time, not UTC.
 */
@Converter(autoApply = false)
public class ChicagoInstantConverter implements AttributeConverter<Instant, LocalDateTime> {

    private static final ZoneId PLANT_ZONE = ZoneId.of("America/Chicago");

    @Override
    public LocalDateTime convertToDatabaseColumn(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, PLANT_ZONE);
    }

    @Override
    public Instant convertToEntityAttribute(LocalDateTime localDateTime) {
        return localDateTime == null ? null : localDateTime.atZone(PLANT_ZONE).toInstant();
    }
}
