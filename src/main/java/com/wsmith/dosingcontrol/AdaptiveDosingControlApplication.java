package com.wsmith.dosingcontrol;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class AdaptiveDosingControlApplication {

	public static void main(String[] args) {
		// Belt-and-suspenders fix for the reading_time/event_time skew bug:
		// entities store java.time.Instant (correct, UTC, timezone-neutral), but the
		// JDBC/Hibernate layer needs an explicit timezone to convert Instant -> SQL
		// DATETIME. Without one, that conversion silently falls back to whatever the
		// JVM's default timezone happens to be (IDE run config, OS, container image -
		// all fragile and easy to have drift from the DB's assumed local time).
		// spring.jpa.properties.hibernate.jdbc.time_zone=America/Chicago in
		// application.properties is the primary fix (explicit, travels with the repo,
		// doesn't depend on IDE VM options). Setting the JVM default here too so any
		// other local-time-sensitive code (logging, future LocalDateTime.now() use)
		// is consistent, even outside the JPA layer.
		TimeZone.setDefault(TimeZone.getTimeZone("America/Chicago"));
		SpringApplication.run(AdaptiveDosingControlApplication.class, args);
	}

}
