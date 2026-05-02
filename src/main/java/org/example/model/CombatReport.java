package org.example.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.time.ZonedDateTime;

// [ВИПРАВЛЕННЯ #9] @Data генерує всі геттери, сеттери, equals, hashCode, toString
// Замінює ~100 рядків шаблонного коду
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CombatReport {
    private Long id;
    private ZonedDateTime reportDate;
    private Integer unitId;
    private Integer flightId;
    private String flightNumber;
    private String positionId;
    private String unitPositionName;
    private String taskNumber;
    private String taskType;
    private String targetNumber;
    private String targetNumberSkymap;
    private Integer targetNumberVirazh;
    private String targetType;
    private String targetSubType;
    private String side;
    private Integer targetQty;
    private Integer altitude;
    private String coordinates;
    private String geoMarker;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX")
    private ZonedDateTime contactTime;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX")
    private ZonedDateTime takeoffTime;

    private String detectionType;
    private String contactOutcome;
    private String weaponId;
    private String weaponNumber;
    private String effectorStatus;
    private String effectorLossReason;
    private String effectorReturnReason;
    private Integer effectorExpenditure;
    private Integer effectorStock;
    private String payloadType;
    private Integer payloadQty;
    private String payloadStatus;
    private String description;
    private Integer authorId;
    private Integer operatorId;
    private Integer pilotId;
    private Integer navigatorId;
    private String status;
    private String reportType;
    private String militaryUnit;
    private String unitName;
    private String dfBranch;
    private String authorName;
    private String operatorName;
    private String pilotName;
    private String navigatorName;
}