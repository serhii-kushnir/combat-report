package org.example.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.ZonedDateTime;

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
    private Integer pilotId;           // НОВЕ ПОЛЕ
    private Integer navigatorId;       // НОВЕ ПОЛЕ
    private String status;
    private String reportType;
    private String militaryUnit;
    private String unitName;
    private String dfBranch;
    private String authorName;
    private String operatorName;
    private String pilotName;          // НОВЕ ПОЛЕ
    private String navigatorName;      // НОВЕ ПОЛЕ

    public CombatReport() {}

    // Геттери
    public Long getId() { return id; }
    public ZonedDateTime getReportDate() { return reportDate; }
    public Integer getUnitId() { return unitId; }
    public Integer getFlightId() { return flightId; }
    public String getFlightNumber() { return flightNumber; }
    public String getPositionId() { return positionId; }
    public String getUnitPositionName() { return unitPositionName; }
    public String getTaskNumber() { return taskNumber; }
    public String getTaskType() { return taskType; }
    public String getTargetNumber() { return targetNumber; }
    public String getTargetNumberSkymap() { return targetNumberSkymap; }
    public Integer getTargetNumberVirazh() { return targetNumberVirazh; }
    public String getTargetType() { return targetType; }
    public String getTargetSubType() { return targetSubType; }
    public String getSide() { return side; }
    public Integer getTargetQty() { return targetQty; }
    public Integer getAltitude() { return altitude; }
    public String getCoordinates() { return coordinates; }
    public String getGeoMarker() { return geoMarker; }
    public ZonedDateTime getContactTime() { return contactTime; }
    public ZonedDateTime getTakeoffTime() { return takeoffTime; }
    public String getDetectionType() { return detectionType; }
    public String getContactOutcome() { return contactOutcome; }
    public String getWeaponId() { return weaponId; }
    public String getWeaponNumber() { return weaponNumber; }
    public String getEffectorStatus() { return effectorStatus; }
    public String getEffectorLossReason() { return effectorLossReason; }
    public String getEffectorReturnReason() { return effectorReturnReason; }
    public Integer getEffectorExpenditure() { return effectorExpenditure; }
    public Integer getEffectorStock() { return effectorStock; }
    public String getPayloadType() { return payloadType; }
    public Integer getPayloadQty() { return payloadQty; }
    public String getPayloadStatus() { return payloadStatus; }
    public String getDescription() { return description; }
    public Integer getAuthorId() { return authorId; }
    public Integer getOperatorId() { return operatorId; }
    public Integer getPilotId() { return pilotId; }
    public Integer getNavigatorId() { return navigatorId; }
    public String getStatus() { return status; }
    public String getReportType() { return reportType; }
    public String getMilitaryUnit() { return militaryUnit; }
    public String getUnitName() { return unitName; }
    public String getDfBranch() { return dfBranch; }
    public String getAuthorName() { return authorName; }
    public String getOperatorName() { return operatorName; }
    public String getPilotName() { return pilotName; }
    public String getNavigatorName() { return navigatorName; }

    // Сеттери
    public void setId(Long id) { this.id = id; }
    public void setReportDate(ZonedDateTime reportDate) { this.reportDate = reportDate; }
    public void setUnitId(Integer unitId) { this.unitId = unitId; }
    public void setFlightId(Integer flightId) { this.flightId = flightId; }
    public void setFlightNumber(String flightNumber) { this.flightNumber = flightNumber; }
    public void setPositionId(String positionId) { this.positionId = positionId; }
    public void setUnitPositionName(String unitPositionName) { this.unitPositionName = unitPositionName; }
    public void setTaskNumber(String taskNumber) { this.taskNumber = taskNumber; }
    public void setTaskType(String taskType) { this.taskType = taskType; }
    public void setTargetNumber(String targetNumber) { this.targetNumber = targetNumber; }
    public void setTargetNumberSkymap(String targetNumberSkymap) { this.targetNumberSkymap = targetNumberSkymap; }
    public void setTargetNumberVirazh(Integer targetNumberVirazh) { this.targetNumberVirazh = targetNumberVirazh; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public void setTargetSubType(String targetSubType) { this.targetSubType = targetSubType; }
    public void setSide(String side) { this.side = side; }
    public void setTargetQty(Integer targetQty) { this.targetQty = targetQty; }
    public void setAltitude(Integer altitude) { this.altitude = altitude; }
    public void setCoordinates(String coordinates) { this.coordinates = coordinates; }
    public void setGeoMarker(String geoMarker) { this.geoMarker = geoMarker; }
    public void setContactTime(ZonedDateTime contactTime) { this.contactTime = contactTime; }
    public void setTakeoffTime(ZonedDateTime takeoffTime) { this.takeoffTime = takeoffTime; }
    public void setDetectionType(String detectionType) { this.detectionType = detectionType; }
    public void setContactOutcome(String contactOutcome) { this.contactOutcome = contactOutcome; }
    public void setWeaponId(String weaponId) { this.weaponId = weaponId; }
    public void setWeaponNumber(String weaponNumber) { this.weaponNumber = weaponNumber; }
    public void setEffectorStatus(String effectorStatus) { this.effectorStatus = effectorStatus; }
    public void setEffectorLossReason(String effectorLossReason) { this.effectorLossReason = effectorLossReason; }
    public void setEffectorReturnReason(String effectorReturnReason) { this.effectorReturnReason = effectorReturnReason; }
    public void setEffectorExpenditure(Integer effectorExpenditure) { this.effectorExpenditure = effectorExpenditure; }
    public void setEffectorStock(Integer effectorStock) { this.effectorStock = effectorStock; }
    public void setPayloadType(String payloadType) { this.payloadType = payloadType; }
    public void setPayloadQty(Integer payloadQty) { this.payloadQty = payloadQty; }
    public void setPayloadStatus(String payloadStatus) { this.payloadStatus = payloadStatus; }
    public void setDescription(String description) { this.description = description; }
    public void setAuthorId(Integer authorId) { this.authorId = authorId; }
    public void setOperatorId(Integer operatorId) { this.operatorId = operatorId; }
    public void setPilotId(Integer pilotId) { this.pilotId = pilotId; }
    public void setNavigatorId(Integer navigatorId) { this.navigatorId = navigatorId; }
    public void setStatus(String status) { this.status = status; }
    public void setReportType(String reportType) { this.reportType = reportType; }
    public void setMilitaryUnit(String militaryUnit) { this.militaryUnit = militaryUnit; }
    public void setUnitName(String unitName) { this.unitName = unitName; }
    public void setDfBranch(String dfBranch) { this.dfBranch = dfBranch; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public void setOperatorName(String operatorName) { this.operatorName = operatorName; }
    public void setPilotName(String pilotName) { this.pilotName = pilotName; }
    public void setNavigatorName(String navigatorName) { this.navigatorName = navigatorName; }
}