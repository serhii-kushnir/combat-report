package org.example;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

public class CombatReport {
    private int id;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime reportDate;

    private int unitId;
    private int flightId;
    private Integer flightNumber;
    private String positionId;
    private String unitPositionName;
    private String taskNumber;
    private String taskType;
    private String targetNumber;
    private String targetNumberSkymap;
    private int targetNumberVirazh;
    private String targetType;
    private String targetSubType;
    private String side;
    private int targetQty;
    private int altitude;
    private String coordinates;
    private String geoMarker;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime contactTime;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime takeoffTime;

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
    private int payloadQty;
    private String payloadStatus;
    private String description;
    private int authorId;
    private int operatorId;
    private String status;
    private String reportType;
    private String militaryUnit;
    private String unitName;
    private String dfBranch;
    private String authorName;
    private String operatorName;

    public CombatReport() {}

    // Геттери
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public LocalDateTime getReportDate() { return reportDate; }
    public void setReportDate(LocalDateTime reportDate) { this.reportDate = reportDate; }

    public int getUnitId() { return unitId; }
    public void setUnitId(int unitId) { this.unitId = unitId; }

    public int getFlightId() { return flightId; }
    public void setFlightId(int flightId) { this.flightId = flightId; }

    public Integer getFlightNumber() { return flightNumber; }
    public void setFlightNumber(Integer flightNumber) { this.flightNumber = flightNumber; }

    public String getPositionId() { return positionId; }
    public void setPositionId(String positionId) { this.positionId = positionId; }

    public String getUnitPositionName() { return unitPositionName; }
    public void setUnitPositionName(String unitPositionName) { this.unitPositionName = unitPositionName; }

    public String getTaskNumber() { return taskNumber; }
    public void setTaskNumber(String taskNumber) { this.taskNumber = taskNumber; }

    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }

    public String getTargetNumber() { return targetNumber; }
    public void setTargetNumber(String targetNumber) { this.targetNumber = targetNumber; }

    public String getTargetNumberSkymap() { return targetNumberSkymap; }
    public void setTargetNumberSkymap(String targetNumberSkymap) { this.targetNumberSkymap = targetNumberSkymap; }

    public int getTargetNumberVirazh() { return targetNumberVirazh; }
    public void setTargetNumberVirazh(int targetNumberVirazh) { this.targetNumberVirazh = targetNumberVirazh; }

    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }

    public String getTargetSubType() { return targetSubType; }
    public void setTargetSubType(String targetSubType) { this.targetSubType = targetSubType; }

    public String getSide() { return side; }
    public void setSide(String side) { this.side = side; }

    public int getTargetQty() { return targetQty; }
    public void setTargetQty(int targetQty) { this.targetQty = targetQty; }

    public int getAltitude() { return altitude; }
    public void setAltitude(int altitude) { this.altitude = altitude; }

    public String getCoordinates() { return coordinates; }
    public void setCoordinates(String coordinates) { this.coordinates = coordinates; }

    public String getGeoMarker() { return geoMarker; }
    public void setGeoMarker(String geoMarker) { this.geoMarker = geoMarker; }

    public LocalDateTime getContactTime() { return contactTime; }
    public void setContactTime(LocalDateTime contactTime) { this.contactTime = contactTime; }

    public LocalDateTime getTakeoffTime() { return takeoffTime; }
    public void setTakeoffTime(LocalDateTime takeoffTime) { this.takeoffTime = takeoffTime; }

    public String getDetectionType() { return detectionType; }
    public void setDetectionType(String detectionType) { this.detectionType = detectionType; }

    public String getContactOutcome() { return contactOutcome; }
    public void setContactOutcome(String contactOutcome) { this.contactOutcome = contactOutcome; }

    public String getWeaponId() { return weaponId; }
    public void setWeaponId(String weaponId) { this.weaponId = weaponId; }

    public String getWeaponNumber() { return weaponNumber; }
    public void setWeaponNumber(String weaponNumber) { this.weaponNumber = weaponNumber; }

    public String getEffectorStatus() { return effectorStatus; }
    public void setEffectorStatus(String effectorStatus) { this.effectorStatus = effectorStatus; }

    public String getEffectorLossReason() { return effectorLossReason; }
    public void setEffectorLossReason(String effectorLossReason) { this.effectorLossReason = effectorLossReason; }

    public String getEffectorReturnReason() { return effectorReturnReason; }
    public void setEffectorReturnReason(String effectorReturnReason) { this.effectorReturnReason = effectorReturnReason; }

    public Integer getEffectorExpenditure() { return effectorExpenditure; }
    public void setEffectorExpenditure(Integer effectorExpenditure) { this.effectorExpenditure = effectorExpenditure; }

    public Integer getEffectorStock() { return effectorStock; }
    public void setEffectorStock(Integer effectorStock) { this.effectorStock = effectorStock; }

    public String getPayloadType() { return payloadType; }
    public void setPayloadType(String payloadType) { this.payloadType = payloadType; }

    public int getPayloadQty() { return payloadQty; }
    public void setPayloadQty(int payloadQty) { this.payloadQty = payloadQty; }

    public String getPayloadStatus() { return payloadStatus; }
    public void setPayloadStatus(String payloadStatus) { this.payloadStatus = payloadStatus; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getAuthorId() { return authorId; }
    public void setAuthorId(int authorId) { this.authorId = authorId; }

    public int getOperatorId() { return operatorId; }
    public void setOperatorId(int operatorId) { this.operatorId = operatorId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getReportType() { return reportType; }
    public void setReportType(String reportType) { this.reportType = reportType; }

    public String getMilitaryUnit() { return militaryUnit; }
    public void setMilitaryUnit(String militaryUnit) { this.militaryUnit = militaryUnit; }

    public String getUnitName() { return unitName; }
    public void setUnitName(String unitName) { this.unitName = unitName; }

    public String getDfBranch() { return dfBranch; }
    public void setDfBranch(String dfBranch) { this.dfBranch = dfBranch; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getOperatorName() { return operatorName; }
    public void setOperatorName(String operatorName) { this.operatorName = operatorName; }
}