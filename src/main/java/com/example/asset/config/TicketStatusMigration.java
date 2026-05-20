package com.example.asset.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Removes legacy CLOSED ticket status from the database by mapping it to RESOLVED.
 * The application enum only supports OPEN, IN_PROGRESS, and RESOLVED.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TicketStatusMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        int updated = jdbcTemplate.update("""
                UPDATE tickets
                SET status = 'RESOLVED',
                    resolved_at = COALESCE(resolved_at, NOW())
                WHERE UPPER(status) = 'CLOSED'
                """);

        if (updated > 0) {
            log.info("Migrated {} legacy ticket(s) from CLOSED to RESOLVED", updated);
        }
    }
}
