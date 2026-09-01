package com.gmc.retreat.registration.mapper;

import com.gmc.retreat.registration.domain.AttendanceType;
import com.gmc.retreat.registration.domain.Gender;
import com.gmc.retreat.registration.domain.RegistrationStatus;
import com.gmc.retreat.registration.domain.TransportationMethod;
import com.gmc.retreat.registration.domain.WorshipBusRideSlot;
import java.time.OffsetDateTime;

public class RegistrationInsert {
    private Long id;
    private final Long retreatId;
    private final String name;
    private final String normalizedName;
    private final Gender gender;
    private final Integer birthYear;
    private final String phoneNumber;
    private final String phoneLastFour;
    private final String churchCellDepartment;
    private final String lookupKeyHash;
    private final Boolean privacyConsentAgreed;
    private final Boolean feePaid;
    private final RegistrationStatus status;
    private final AttendanceType attendanceType;
    private final OffsetDateTime plannedArrivalAt;
    private final OffsetDateTime plannedDepartureAt;
    private final String partialAttendanceNote;
    private final Boolean lodgingNight1;
    private final Boolean lodgingNight2;
    private final Boolean attendDay1Morning;
    private final Boolean attendDay1Afternoon;
    private final Boolean attendDay1Worship;
    private final Boolean attendDay2Morning;
    private final Boolean attendDay2Afternoon;
    private final Boolean attendDay2Worship;
    private final Boolean attendDay3Morning;
    private final Boolean attendDay3Afternoon;
    private final TransportationMethod inboundTransportationMethod;
    private final Boolean inboundCarpoolAvailable;
    private final Integer inboundCarpoolSeats;
    private final String inboundCarpoolArea;
    private final String inboundCarpoolRouteArea;
    private final String inboundCarpoolNote;
    private final String inboundCarpoolPreferredArea;
    private final String inboundCarpoolPreferredNote;
    private final WorshipBusRideSlot inboundWorshipBusRideSlot;
    private final TransportationMethod outboundTransportationMethod;
    private final Boolean outboundCarpoolAvailable;
    private final Integer outboundCarpoolSeats;
    private final String outboundCarpoolArea;
    private final String outboundCarpoolRouteArea;
    private final String outboundCarpoolNote;
    private final String outboundCarpoolPreferredArea;
    private final String outboundCarpoolPreferredNote;
    private final WorshipBusRideSlot outboundWorshipBusRideSlot;

    public RegistrationInsert(
            Long retreatId,
            String name,
            String normalizedName,
            Gender gender,
            Integer birthYear,
            String phoneNumber,
            String phoneLastFour,
            String churchCellDepartment,
            String lookupKeyHash,
            Boolean privacyConsentAgreed,
            Boolean feePaid,
            RegistrationStatus status,
            AttendanceType attendanceType,
            OffsetDateTime plannedArrivalAt,
            OffsetDateTime plannedDepartureAt,
            String partialAttendanceNote,
            Boolean lodgingNight1,
            Boolean lodgingNight2,
            Boolean attendDay1Morning,
            Boolean attendDay1Afternoon,
            Boolean attendDay1Worship,
            Boolean attendDay2Morning,
            Boolean attendDay2Afternoon,
            Boolean attendDay2Worship,
            Boolean attendDay3Morning,
            Boolean attendDay3Afternoon,
            TransportationMethod inboundTransportationMethod,
            Boolean inboundCarpoolAvailable,
            Integer inboundCarpoolSeats,
            String inboundCarpoolArea,
            String inboundCarpoolRouteArea,
            String inboundCarpoolNote,
            String inboundCarpoolPreferredArea,
            String inboundCarpoolPreferredNote,
            WorshipBusRideSlot inboundWorshipBusRideSlot,
            TransportationMethod outboundTransportationMethod,
            Boolean outboundCarpoolAvailable,
            Integer outboundCarpoolSeats,
            String outboundCarpoolArea,
            String outboundCarpoolRouteArea,
            String outboundCarpoolNote,
            String outboundCarpoolPreferredArea,
            String outboundCarpoolPreferredNote,
            WorshipBusRideSlot outboundWorshipBusRideSlot
    ) {
        this.retreatId = retreatId;
        this.name = name;
        this.normalizedName = normalizedName;
        this.gender = gender;
        this.birthYear = birthYear;
        this.phoneNumber = phoneNumber;
        this.phoneLastFour = phoneLastFour;
        this.churchCellDepartment = churchCellDepartment;
        this.lookupKeyHash = lookupKeyHash;
        this.privacyConsentAgreed = privacyConsentAgreed;
        this.feePaid = feePaid;
        this.status = status;
        this.attendanceType = attendanceType;
        this.plannedArrivalAt = plannedArrivalAt;
        this.plannedDepartureAt = plannedDepartureAt;
        this.partialAttendanceNote = partialAttendanceNote;
        this.lodgingNight1 = lodgingNight1;
        this.lodgingNight2 = lodgingNight2;
        this.attendDay1Morning = attendDay1Morning;
        this.attendDay1Afternoon = attendDay1Afternoon;
        this.attendDay1Worship = attendDay1Worship;
        this.attendDay2Morning = attendDay2Morning;
        this.attendDay2Afternoon = attendDay2Afternoon;
        this.attendDay2Worship = attendDay2Worship;
        this.attendDay3Morning = attendDay3Morning;
        this.attendDay3Afternoon = attendDay3Afternoon;
        this.inboundTransportationMethod = inboundTransportationMethod;
        this.inboundCarpoolAvailable = inboundCarpoolAvailable;
        this.inboundCarpoolSeats = inboundCarpoolSeats;
        this.inboundCarpoolArea = inboundCarpoolArea;
        this.inboundCarpoolRouteArea = inboundCarpoolRouteArea;
        this.inboundCarpoolNote = inboundCarpoolNote;
        this.inboundCarpoolPreferredArea = inboundCarpoolPreferredArea;
        this.inboundCarpoolPreferredNote = inboundCarpoolPreferredNote;
        this.inboundWorshipBusRideSlot = inboundWorshipBusRideSlot;
        this.outboundTransportationMethod = outboundTransportationMethod;
        this.outboundCarpoolAvailable = outboundCarpoolAvailable;
        this.outboundCarpoolSeats = outboundCarpoolSeats;
        this.outboundCarpoolArea = outboundCarpoolArea;
        this.outboundCarpoolRouteArea = outboundCarpoolRouteArea;
        this.outboundCarpoolNote = outboundCarpoolNote;
        this.outboundCarpoolPreferredArea = outboundCarpoolPreferredArea;
        this.outboundCarpoolPreferredNote = outboundCarpoolPreferredNote;
        this.outboundWorshipBusRideSlot = outboundWorshipBusRideSlot;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRetreatId() {
        return retreatId;
    }

    public String getName() {
        return name;
    }

    public String getNormalizedName() {
        return normalizedName;
    }

    public Gender getGender() {
        return gender;
    }

    public Integer getBirthYear() {
        return birthYear;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getPhoneLastFour() {
        return phoneLastFour;
    }

    public String getChurchCellDepartment() {
        return churchCellDepartment;
    }

    public String getLookupKeyHash() {
        return lookupKeyHash;
    }

    public Boolean getPrivacyConsentAgreed() {
        return privacyConsentAgreed;
    }

    public Boolean getFeePaid() {
        return feePaid;
    }

    public RegistrationStatus getStatus() {
        return status;
    }

    public AttendanceType getAttendanceType() {
        return attendanceType;
    }

    public OffsetDateTime getPlannedArrivalAt() {
        return plannedArrivalAt;
    }

    public OffsetDateTime getPlannedDepartureAt() {
        return plannedDepartureAt;
    }

    public String getPartialAttendanceNote() {
        return partialAttendanceNote;
    }

    public Boolean getLodgingNight1() {
        return lodgingNight1;
    }

    public Boolean getLodgingNight2() {
        return lodgingNight2;
    }

    public Boolean getAttendDay1Morning() {
        return attendDay1Morning;
    }

    public Boolean getAttendDay1Afternoon() {
        return attendDay1Afternoon;
    }

    public Boolean getAttendDay1Worship() {
        return attendDay1Worship;
    }

    public Boolean getAttendDay2Morning() {
        return attendDay2Morning;
    }

    public Boolean getAttendDay2Afternoon() {
        return attendDay2Afternoon;
    }

    public Boolean getAttendDay2Worship() {
        return attendDay2Worship;
    }

    public Boolean getAttendDay3Morning() {
        return attendDay3Morning;
    }

    public Boolean getAttendDay3Afternoon() {
        return attendDay3Afternoon;
    }

    public TransportationMethod getInboundTransportationMethod() {
        return inboundTransportationMethod;
    }

    public Boolean getInboundCarpoolAvailable() {
        return inboundCarpoolAvailable;
    }

    public Integer getInboundCarpoolSeats() {
        return inboundCarpoolSeats;
    }

    public String getInboundCarpoolArea() {
        return inboundCarpoolArea;
    }

    public String getInboundCarpoolRouteArea() {
        return inboundCarpoolRouteArea;
    }

    public String getInboundCarpoolNote() {
        return inboundCarpoolNote;
    }

    public String getInboundCarpoolPreferredArea() {
        return inboundCarpoolPreferredArea;
    }

    public String getInboundCarpoolPreferredNote() {
        return inboundCarpoolPreferredNote;
    }

    public WorshipBusRideSlot getInboundWorshipBusRideSlot() {
        return inboundWorshipBusRideSlot;
    }

    public TransportationMethod getOutboundTransportationMethod() {
        return outboundTransportationMethod;
    }

    public Boolean getOutboundCarpoolAvailable() {
        return outboundCarpoolAvailable;
    }

    public Integer getOutboundCarpoolSeats() {
        return outboundCarpoolSeats;
    }

    public String getOutboundCarpoolArea() {
        return outboundCarpoolArea;
    }

    public String getOutboundCarpoolRouteArea() {
        return outboundCarpoolRouteArea;
    }

    public String getOutboundCarpoolNote() {
        return outboundCarpoolNote;
    }

    public String getOutboundCarpoolPreferredArea() {
        return outboundCarpoolPreferredArea;
    }

    public String getOutboundCarpoolPreferredNote() {
        return outboundCarpoolPreferredNote;
    }

    public WorshipBusRideSlot getOutboundWorshipBusRideSlot() {
        return outboundWorshipBusRideSlot;
    }
}
