CREATE TABLE combat_duty (
                             id BIGINT AUTO_INCREMENT PRIMARY KEY,
                             start_time TIMESTAMP NOT NULL,
                             end_time TIMESTAMP NOT NULL,
                             crew_members VARCHAR(500),
                             weapons VARCHAR(255),
                             duty_officer VARCHAR(255),
                             report_summary VARCHAR(2000),
                             senior_crew_member VARCHAR(255)
);

CREATE TABLE duty_event (
                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                            combat_duty_id BIGINT NOT NULL,
                            event_time TIMESTAMP NOT NULL,
                            reporter VARCHAR(255),
                            content VARCHAR(2000),
                            reported_to VARCHAR(255),
                            decision VARCHAR(2000),
                            FOREIGN KEY (combat_duty_id) REFERENCES combat_duty(id) ON DELETE CASCADE
);