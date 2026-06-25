package com.gmc.retreat.registration.mapper;

import com.gmc.retreat.registration.domain.AttendanceType;
import com.gmc.retreat.registration.domain.Gender;
import com.gmc.retreat.registration.domain.Registration;
import com.gmc.retreat.registration.domain.RegistrationStatus;
import com.gmc.retreat.registration.domain.TransportationMethod;
import com.gmc.retreat.registration.domain.WorshipBusRideSlot;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface RegistrationMapper {

    @Select("""
            SELECT r.id, r.name, r.normalized_name, r.gender, r.birth_year, r.phone_number, r.phone_last_four,
                   r.church_cell_department, r.church_cell_id, cc.name AS church_cell_name,
                   mg.id AS middle_group_id, mg.name AS middle_group_name,
                   rg.id AS retreat_group_id, rg.name AS retreat_group_name, rgm.leader AS retreat_group_leader,
                   r.lookup_key_hash, r.privacy_consent_agreed, r.fee_paid,
                   r.status, r.admin_memo, r.newcomer, r.care_target,
                   COALESCE(ci.checked_in, FALSE) AS checked_in, r.created_at, r.updated_at,
                   r.attendance_type,
                   r.planned_arrival_at, r.planned_departure_at, r.partial_attendance_note,
                   r.lodging_night1, r.lodging_night2,
                   r.attend_day1_morning, r.attend_day1_afternoon, r.attend_day1_worship,
                   r.attend_day2_morning, r.attend_day2_afternoon, r.attend_day2_worship,
                   r.attend_day3_morning, r.attend_day3_afternoon,
                   r.inbound_transportation_method, r.inbound_carpool_available, r.inbound_carpool_seats,
                   r.inbound_carpool_area, r.inbound_carpool_route_area, r.inbound_carpool_note,
                   r.inbound_carpool_preferred_area, r.inbound_carpool_preferred_note, r.inbound_worship_bus_ride_slot,
                   r.outbound_transportation_method, r.outbound_carpool_available, r.outbound_carpool_seats,
                   r.outbound_carpool_area, r.outbound_carpool_route_area, r.outbound_carpool_note,
                   r.outbound_carpool_preferred_area, r.outbound_carpool_preferred_note, r.outbound_worship_bus_ride_slot
            FROM registrations r
            LEFT JOIN church_cells cc ON cc.id = r.church_cell_id
            LEFT JOIN church_middle_groups mg ON mg.id = cc.church_middle_group_id
            LEFT JOIN retreat_group_members rgm ON rgm.registration_id = r.id
            LEFT JOIN retreat_groups rg ON rg.id = rgm.retreat_group_id
            LEFT JOIN retreat_check_ins ci ON ci.participant_id = r.id
            WHERE r.id = #{id}
            """)
    @ConstructorArgs({
            @Arg(column = "id", javaType = Long.class),
            @Arg(column = "name", javaType = String.class),
            @Arg(column = "normalized_name", javaType = String.class),
            @Arg(column = "gender", javaType = Gender.class),
            @Arg(column = "birth_year", javaType = Integer.class),
            @Arg(column = "phone_number", javaType = String.class),
            @Arg(column = "phone_last_four", javaType = String.class),
            @Arg(column = "church_cell_department", javaType = String.class),
            @Arg(column = "church_cell_id", javaType = Long.class),
            @Arg(column = "church_cell_name", javaType = String.class),
            @Arg(column = "middle_group_id", javaType = Long.class),
            @Arg(column = "middle_group_name", javaType = String.class),
            @Arg(column = "retreat_group_id", javaType = Long.class),
            @Arg(column = "retreat_group_name", javaType = String.class),
            @Arg(column = "retreat_group_leader", javaType = Boolean.class),
            @Arg(column = "lookup_key_hash", javaType = String.class),
            @Arg(column = "privacy_consent_agreed", javaType = Boolean.class),
            @Arg(column = "fee_paid", javaType = Boolean.class),
            @Arg(column = "status", javaType = RegistrationStatus.class),
            @Arg(column = "admin_memo", javaType = String.class),
            @Arg(column = "newcomer", javaType = Boolean.class),
            @Arg(column = "care_target", javaType = Boolean.class),
            @Arg(column = "checked_in", javaType = Boolean.class),
            @Arg(column = "created_at", javaType = OffsetDateTime.class),
            @Arg(column = "updated_at", javaType = OffsetDateTime.class),
            @Arg(column = "attendance_type", javaType = AttendanceType.class),
            @Arg(column = "planned_arrival_at", javaType = OffsetDateTime.class),
            @Arg(column = "planned_departure_at", javaType = OffsetDateTime.class),
            @Arg(column = "partial_attendance_note", javaType = String.class),
            @Arg(column = "lodging_night1", javaType = Boolean.class),
            @Arg(column = "lodging_night2", javaType = Boolean.class),
            @Arg(column = "attend_day1_morning", javaType = Boolean.class),
            @Arg(column = "attend_day1_afternoon", javaType = Boolean.class),
            @Arg(column = "attend_day1_worship", javaType = Boolean.class),
            @Arg(column = "attend_day2_morning", javaType = Boolean.class),
            @Arg(column = "attend_day2_afternoon", javaType = Boolean.class),
            @Arg(column = "attend_day2_worship", javaType = Boolean.class),
            @Arg(column = "attend_day3_morning", javaType = Boolean.class),
            @Arg(column = "attend_day3_afternoon", javaType = Boolean.class),
            @Arg(column = "inbound_transportation_method", javaType = TransportationMethod.class),
            @Arg(column = "inbound_carpool_available", javaType = Boolean.class),
            @Arg(column = "inbound_carpool_seats", javaType = Integer.class),
            @Arg(column = "inbound_carpool_area", javaType = String.class),
            @Arg(column = "inbound_carpool_route_area", javaType = String.class),
            @Arg(column = "inbound_carpool_note", javaType = String.class),
            @Arg(column = "inbound_carpool_preferred_area", javaType = String.class),
            @Arg(column = "inbound_carpool_preferred_note", javaType = String.class),
            @Arg(column = "inbound_worship_bus_ride_slot", javaType = WorshipBusRideSlot.class),
            @Arg(column = "outbound_transportation_method", javaType = TransportationMethod.class),
            @Arg(column = "outbound_carpool_available", javaType = Boolean.class),
            @Arg(column = "outbound_carpool_seats", javaType = Integer.class),
            @Arg(column = "outbound_carpool_area", javaType = String.class),
            @Arg(column = "outbound_carpool_route_area", javaType = String.class),
            @Arg(column = "outbound_carpool_note", javaType = String.class),
            @Arg(column = "outbound_carpool_preferred_area", javaType = String.class),
            @Arg(column = "outbound_carpool_preferred_note", javaType = String.class),
            @Arg(column = "outbound_worship_bus_ride_slot", javaType = WorshipBusRideSlot.class)
    })
    Optional<Registration> findById(@Param("id") Long id);

    @Select("""
            SELECT r.id, r.name, r.normalized_name, r.gender, r.birth_year, r.phone_number, r.phone_last_four,
                   r.church_cell_department, r.church_cell_id, cc.name AS church_cell_name,
                   mg.id AS middle_group_id, mg.name AS middle_group_name,
                   rg.id AS retreat_group_id, rg.name AS retreat_group_name, rgm.leader AS retreat_group_leader,
                   r.lookup_key_hash, r.privacy_consent_agreed, r.fee_paid,
                   r.status, r.admin_memo, r.newcomer, r.care_target,
                   COALESCE(ci.checked_in, FALSE) AS checked_in, r.created_at, r.updated_at,
                   r.attendance_type,
                   r.planned_arrival_at, r.planned_departure_at, r.partial_attendance_note,
                   r.lodging_night1, r.lodging_night2,
                   r.attend_day1_morning, r.attend_day1_afternoon, r.attend_day1_worship,
                   r.attend_day2_morning, r.attend_day2_afternoon, r.attend_day2_worship,
                   r.attend_day3_morning, r.attend_day3_afternoon,
                   r.inbound_transportation_method, r.inbound_carpool_available, r.inbound_carpool_seats,
                   r.inbound_carpool_area, r.inbound_carpool_route_area, r.inbound_carpool_note,
                   r.inbound_carpool_preferred_area, r.inbound_carpool_preferred_note, r.inbound_worship_bus_ride_slot,
                   r.outbound_transportation_method, r.outbound_carpool_available, r.outbound_carpool_seats,
                   r.outbound_carpool_area, r.outbound_carpool_route_area, r.outbound_carpool_note,
                   r.outbound_carpool_preferred_area, r.outbound_carpool_preferred_note, r.outbound_worship_bus_ride_slot
            FROM registrations r
            LEFT JOIN church_cells cc ON cc.id = r.church_cell_id
            LEFT JOIN church_middle_groups mg ON mg.id = cc.church_middle_group_id
            LEFT JOIN retreat_group_members rgm ON rgm.registration_id = r.id
            LEFT JOIN retreat_groups rg ON rg.id = rgm.retreat_group_id
            LEFT JOIN retreat_check_ins ci ON ci.participant_id = r.id
            WHERE r.normalized_name = #{normalizedName}
              AND r.phone_number = #{phoneNumber}
              AND r.status = 'REGISTERED'
            """)
    @ConstructorArgs({
            @Arg(column = "id", javaType = Long.class),
            @Arg(column = "name", javaType = String.class),
            @Arg(column = "normalized_name", javaType = String.class),
            @Arg(column = "gender", javaType = Gender.class),
            @Arg(column = "birth_year", javaType = Integer.class),
            @Arg(column = "phone_number", javaType = String.class),
            @Arg(column = "phone_last_four", javaType = String.class),
            @Arg(column = "church_cell_department", javaType = String.class),
            @Arg(column = "church_cell_id", javaType = Long.class),
            @Arg(column = "church_cell_name", javaType = String.class),
            @Arg(column = "middle_group_id", javaType = Long.class),
            @Arg(column = "middle_group_name", javaType = String.class),
            @Arg(column = "retreat_group_id", javaType = Long.class),
            @Arg(column = "retreat_group_name", javaType = String.class),
            @Arg(column = "retreat_group_leader", javaType = Boolean.class),
            @Arg(column = "lookup_key_hash", javaType = String.class),
            @Arg(column = "privacy_consent_agreed", javaType = Boolean.class),
            @Arg(column = "fee_paid", javaType = Boolean.class),
            @Arg(column = "status", javaType = RegistrationStatus.class),
            @Arg(column = "admin_memo", javaType = String.class),
            @Arg(column = "newcomer", javaType = Boolean.class),
            @Arg(column = "care_target", javaType = Boolean.class),
            @Arg(column = "checked_in", javaType = Boolean.class),
            @Arg(column = "created_at", javaType = OffsetDateTime.class),
            @Arg(column = "updated_at", javaType = OffsetDateTime.class),
            @Arg(column = "attendance_type", javaType = AttendanceType.class),
            @Arg(column = "planned_arrival_at", javaType = OffsetDateTime.class),
            @Arg(column = "planned_departure_at", javaType = OffsetDateTime.class),
            @Arg(column = "partial_attendance_note", javaType = String.class),
            @Arg(column = "lodging_night1", javaType = Boolean.class),
            @Arg(column = "lodging_night2", javaType = Boolean.class),
            @Arg(column = "attend_day1_morning", javaType = Boolean.class),
            @Arg(column = "attend_day1_afternoon", javaType = Boolean.class),
            @Arg(column = "attend_day1_worship", javaType = Boolean.class),
            @Arg(column = "attend_day2_morning", javaType = Boolean.class),
            @Arg(column = "attend_day2_afternoon", javaType = Boolean.class),
            @Arg(column = "attend_day2_worship", javaType = Boolean.class),
            @Arg(column = "attend_day3_morning", javaType = Boolean.class),
            @Arg(column = "attend_day3_afternoon", javaType = Boolean.class),
            @Arg(column = "inbound_transportation_method", javaType = TransportationMethod.class),
            @Arg(column = "inbound_carpool_available", javaType = Boolean.class),
            @Arg(column = "inbound_carpool_seats", javaType = Integer.class),
            @Arg(column = "inbound_carpool_area", javaType = String.class),
            @Arg(column = "inbound_carpool_route_area", javaType = String.class),
            @Arg(column = "inbound_carpool_note", javaType = String.class),
            @Arg(column = "inbound_carpool_preferred_area", javaType = String.class),
            @Arg(column = "inbound_carpool_preferred_note", javaType = String.class),
            @Arg(column = "inbound_worship_bus_ride_slot", javaType = WorshipBusRideSlot.class),
            @Arg(column = "outbound_transportation_method", javaType = TransportationMethod.class),
            @Arg(column = "outbound_carpool_available", javaType = Boolean.class),
            @Arg(column = "outbound_carpool_seats", javaType = Integer.class),
            @Arg(column = "outbound_carpool_area", javaType = String.class),
            @Arg(column = "outbound_carpool_route_area", javaType = String.class),
            @Arg(column = "outbound_carpool_note", javaType = String.class),
            @Arg(column = "outbound_carpool_preferred_area", javaType = String.class),
            @Arg(column = "outbound_carpool_preferred_note", javaType = String.class),
            @Arg(column = "outbound_worship_bus_ride_slot", javaType = WorshipBusRideSlot.class)
    })
    Optional<Registration> findActiveByNormalizedNameAndPhoneNumber(
            @Param("normalizedName") String normalizedName,
            @Param("phoneNumber") String phoneNumber
    );

    @Select("""
            SELECT r.id, r.name, r.normalized_name, r.gender, r.birth_year, r.phone_number, r.phone_last_four,
                   r.church_cell_department, r.church_cell_id, cc.name AS church_cell_name,
                   mg.id AS middle_group_id, mg.name AS middle_group_name,
                   rg.id AS retreat_group_id, rg.name AS retreat_group_name, rgm.leader AS retreat_group_leader,
                   r.lookup_key_hash, r.privacy_consent_agreed, r.fee_paid,
                   r.status, r.admin_memo, r.newcomer, r.care_target,
                   COALESCE(ci.checked_in, FALSE) AS checked_in, r.created_at, r.updated_at,
                   r.attendance_type,
                   r.planned_arrival_at, r.planned_departure_at, r.partial_attendance_note,
                   r.lodging_night1, r.lodging_night2,
                   r.attend_day1_morning, r.attend_day1_afternoon, r.attend_day1_worship,
                   r.attend_day2_morning, r.attend_day2_afternoon, r.attend_day2_worship,
                   r.attend_day3_morning, r.attend_day3_afternoon,
                   r.inbound_transportation_method, r.inbound_carpool_available, r.inbound_carpool_seats,
                   r.inbound_carpool_area, r.inbound_carpool_route_area, r.inbound_carpool_note,
                   r.inbound_carpool_preferred_area, r.inbound_carpool_preferred_note, r.inbound_worship_bus_ride_slot,
                   r.outbound_transportation_method, r.outbound_carpool_available, r.outbound_carpool_seats,
                   r.outbound_carpool_area, r.outbound_carpool_route_area, r.outbound_carpool_note,
                   r.outbound_carpool_preferred_area, r.outbound_carpool_preferred_note, r.outbound_worship_bus_ride_slot
            FROM registrations r
            LEFT JOIN church_cells cc ON cc.id = r.church_cell_id
            LEFT JOIN church_middle_groups mg ON mg.id = cc.church_middle_group_id
            LEFT JOIN retreat_group_members rgm ON rgm.registration_id = r.id
            LEFT JOIN retreat_groups rg ON rg.id = rgm.retreat_group_id
            LEFT JOIN retreat_check_ins ci ON ci.participant_id = r.id
            WHERE r.normalized_name = #{normalizedName}
              AND r.phone_last_four = #{phoneLastFour}
              AND r.status = 'REGISTERED'
            ORDER BY r.id ASC
            """)
    @ConstructorArgs({
            @Arg(column = "id", javaType = Long.class),
            @Arg(column = "name", javaType = String.class),
            @Arg(column = "normalized_name", javaType = String.class),
            @Arg(column = "gender", javaType = Gender.class),
            @Arg(column = "birth_year", javaType = Integer.class),
            @Arg(column = "phone_number", javaType = String.class),
            @Arg(column = "phone_last_four", javaType = String.class),
            @Arg(column = "church_cell_department", javaType = String.class),
            @Arg(column = "church_cell_id", javaType = Long.class),
            @Arg(column = "church_cell_name", javaType = String.class),
            @Arg(column = "middle_group_id", javaType = Long.class),
            @Arg(column = "middle_group_name", javaType = String.class),
            @Arg(column = "retreat_group_id", javaType = Long.class),
            @Arg(column = "retreat_group_name", javaType = String.class),
            @Arg(column = "retreat_group_leader", javaType = Boolean.class),
            @Arg(column = "lookup_key_hash", javaType = String.class),
            @Arg(column = "privacy_consent_agreed", javaType = Boolean.class),
            @Arg(column = "fee_paid", javaType = Boolean.class),
            @Arg(column = "status", javaType = RegistrationStatus.class),
            @Arg(column = "admin_memo", javaType = String.class),
            @Arg(column = "newcomer", javaType = Boolean.class),
            @Arg(column = "care_target", javaType = Boolean.class),
            @Arg(column = "checked_in", javaType = Boolean.class),
            @Arg(column = "created_at", javaType = OffsetDateTime.class),
            @Arg(column = "updated_at", javaType = OffsetDateTime.class),
            @Arg(column = "attendance_type", javaType = AttendanceType.class),
            @Arg(column = "planned_arrival_at", javaType = OffsetDateTime.class),
            @Arg(column = "planned_departure_at", javaType = OffsetDateTime.class),
            @Arg(column = "partial_attendance_note", javaType = String.class),
            @Arg(column = "lodging_night1", javaType = Boolean.class),
            @Arg(column = "lodging_night2", javaType = Boolean.class),
            @Arg(column = "attend_day1_morning", javaType = Boolean.class),
            @Arg(column = "attend_day1_afternoon", javaType = Boolean.class),
            @Arg(column = "attend_day1_worship", javaType = Boolean.class),
            @Arg(column = "attend_day2_morning", javaType = Boolean.class),
            @Arg(column = "attend_day2_afternoon", javaType = Boolean.class),
            @Arg(column = "attend_day2_worship", javaType = Boolean.class),
            @Arg(column = "attend_day3_morning", javaType = Boolean.class),
            @Arg(column = "attend_day3_afternoon", javaType = Boolean.class),
            @Arg(column = "inbound_transportation_method", javaType = TransportationMethod.class),
            @Arg(column = "inbound_carpool_available", javaType = Boolean.class),
            @Arg(column = "inbound_carpool_seats", javaType = Integer.class),
            @Arg(column = "inbound_carpool_area", javaType = String.class),
            @Arg(column = "inbound_carpool_route_area", javaType = String.class),
            @Arg(column = "inbound_carpool_note", javaType = String.class),
            @Arg(column = "inbound_carpool_preferred_area", javaType = String.class),
            @Arg(column = "inbound_carpool_preferred_note", javaType = String.class),
            @Arg(column = "inbound_worship_bus_ride_slot", javaType = WorshipBusRideSlot.class),
            @Arg(column = "outbound_transportation_method", javaType = TransportationMethod.class),
            @Arg(column = "outbound_carpool_available", javaType = Boolean.class),
            @Arg(column = "outbound_carpool_seats", javaType = Integer.class),
            @Arg(column = "outbound_carpool_area", javaType = String.class),
            @Arg(column = "outbound_carpool_route_area", javaType = String.class),
            @Arg(column = "outbound_carpool_note", javaType = String.class),
            @Arg(column = "outbound_carpool_preferred_area", javaType = String.class),
            @Arg(column = "outbound_carpool_preferred_note", javaType = String.class),
            @Arg(column = "outbound_worship_bus_ride_slot", javaType = WorshipBusRideSlot.class)
    })
    List<Registration> findActiveByNormalizedNameAndPhoneLastFour(
            @Param("normalizedName") String normalizedName,
            @Param("phoneLastFour") String phoneLastFour
    );

    @Select("""
            SELECT r.id, r.name, r.normalized_name, r.gender, r.birth_year, r.phone_number, r.phone_last_four,
                   r.church_cell_department, r.church_cell_id, cc.name AS church_cell_name,
                   mg.id AS middle_group_id, mg.name AS middle_group_name,
                   rg.id AS retreat_group_id, rg.name AS retreat_group_name, rgm.leader AS retreat_group_leader,
                   r.lookup_key_hash, r.privacy_consent_agreed, r.fee_paid,
                   r.status, r.admin_memo, r.newcomer, r.care_target,
                   COALESCE(ci.checked_in, FALSE) AS checked_in, r.created_at, r.updated_at,
                   r.attendance_type,
                   r.planned_arrival_at, r.planned_departure_at, r.partial_attendance_note,
                   r.lodging_night1, r.lodging_night2,
                   r.attend_day1_morning, r.attend_day1_afternoon, r.attend_day1_worship,
                   r.attend_day2_morning, r.attend_day2_afternoon, r.attend_day2_worship,
                   r.attend_day3_morning, r.attend_day3_afternoon,
                   r.inbound_transportation_method, r.inbound_carpool_available, r.inbound_carpool_seats,
                   r.inbound_carpool_area, r.inbound_carpool_route_area, r.inbound_carpool_note,
                   r.inbound_carpool_preferred_area, r.inbound_carpool_preferred_note, r.inbound_worship_bus_ride_slot,
                   r.outbound_transportation_method, r.outbound_carpool_available, r.outbound_carpool_seats,
                   r.outbound_carpool_area, r.outbound_carpool_route_area, r.outbound_carpool_note,
                   r.outbound_carpool_preferred_area, r.outbound_carpool_preferred_note, r.outbound_worship_bus_ride_slot
            FROM registrations r
            LEFT JOIN church_cells cc ON cc.id = r.church_cell_id
            LEFT JOIN church_middle_groups mg ON mg.id = cc.church_middle_group_id
            LEFT JOIN retreat_group_members rgm ON rgm.registration_id = r.id
            LEFT JOIN retreat_groups rg ON rg.id = rgm.retreat_group_id
            LEFT JOIN retreat_check_ins ci ON ci.participant_id = r.id
            WHERE r.normalized_name = #{normalizedName}
              AND r.status = 'REGISTERED'
            ORDER BY r.id ASC
            """)
    @ConstructorArgs({
            @Arg(column = "id", javaType = Long.class),
            @Arg(column = "name", javaType = String.class),
            @Arg(column = "normalized_name", javaType = String.class),
            @Arg(column = "gender", javaType = Gender.class),
            @Arg(column = "birth_year", javaType = Integer.class),
            @Arg(column = "phone_number", javaType = String.class),
            @Arg(column = "phone_last_four", javaType = String.class),
            @Arg(column = "church_cell_department", javaType = String.class),
            @Arg(column = "church_cell_id", javaType = Long.class),
            @Arg(column = "church_cell_name", javaType = String.class),
            @Arg(column = "middle_group_id", javaType = Long.class),
            @Arg(column = "middle_group_name", javaType = String.class),
            @Arg(column = "retreat_group_id", javaType = Long.class),
            @Arg(column = "retreat_group_name", javaType = String.class),
            @Arg(column = "retreat_group_leader", javaType = Boolean.class),
            @Arg(column = "lookup_key_hash", javaType = String.class),
            @Arg(column = "privacy_consent_agreed", javaType = Boolean.class),
            @Arg(column = "fee_paid", javaType = Boolean.class),
            @Arg(column = "status", javaType = RegistrationStatus.class),
            @Arg(column = "admin_memo", javaType = String.class),
            @Arg(column = "newcomer", javaType = Boolean.class),
            @Arg(column = "care_target", javaType = Boolean.class),
            @Arg(column = "checked_in", javaType = Boolean.class),
            @Arg(column = "created_at", javaType = OffsetDateTime.class),
            @Arg(column = "updated_at", javaType = OffsetDateTime.class),
            @Arg(column = "attendance_type", javaType = AttendanceType.class),
            @Arg(column = "planned_arrival_at", javaType = OffsetDateTime.class),
            @Arg(column = "planned_departure_at", javaType = OffsetDateTime.class),
            @Arg(column = "partial_attendance_note", javaType = String.class),
            @Arg(column = "lodging_night1", javaType = Boolean.class),
            @Arg(column = "lodging_night2", javaType = Boolean.class),
            @Arg(column = "attend_day1_morning", javaType = Boolean.class),
            @Arg(column = "attend_day1_afternoon", javaType = Boolean.class),
            @Arg(column = "attend_day1_worship", javaType = Boolean.class),
            @Arg(column = "attend_day2_morning", javaType = Boolean.class),
            @Arg(column = "attend_day2_afternoon", javaType = Boolean.class),
            @Arg(column = "attend_day2_worship", javaType = Boolean.class),
            @Arg(column = "attend_day3_morning", javaType = Boolean.class),
            @Arg(column = "attend_day3_afternoon", javaType = Boolean.class),
            @Arg(column = "inbound_transportation_method", javaType = TransportationMethod.class),
            @Arg(column = "inbound_carpool_available", javaType = Boolean.class),
            @Arg(column = "inbound_carpool_seats", javaType = Integer.class),
            @Arg(column = "inbound_carpool_area", javaType = String.class),
            @Arg(column = "inbound_carpool_route_area", javaType = String.class),
            @Arg(column = "inbound_carpool_note", javaType = String.class),
            @Arg(column = "inbound_carpool_preferred_area", javaType = String.class),
            @Arg(column = "inbound_carpool_preferred_note", javaType = String.class),
            @Arg(column = "inbound_worship_bus_ride_slot", javaType = WorshipBusRideSlot.class),
            @Arg(column = "outbound_transportation_method", javaType = TransportationMethod.class),
            @Arg(column = "outbound_carpool_available", javaType = Boolean.class),
            @Arg(column = "outbound_carpool_seats", javaType = Integer.class),
            @Arg(column = "outbound_carpool_area", javaType = String.class),
            @Arg(column = "outbound_carpool_route_area", javaType = String.class),
            @Arg(column = "outbound_carpool_note", javaType = String.class),
            @Arg(column = "outbound_carpool_preferred_area", javaType = String.class),
            @Arg(column = "outbound_carpool_preferred_note", javaType = String.class),
            @Arg(column = "outbound_worship_bus_ride_slot", javaType = WorshipBusRideSlot.class)
    })
    List<Registration> findActiveByNormalizedName(@Param("normalizedName") String normalizedName);

    @Select("""
            <script>
            SELECT r.id, r.name, r.normalized_name, r.gender, r.birth_year, r.phone_number, r.phone_last_four,
                   r.church_cell_department, r.church_cell_id, cc.name AS church_cell_name,
                   mg.id AS middle_group_id, mg.name AS middle_group_name,
                   rg.id AS retreat_group_id, rg.name AS retreat_group_name, rgm.leader AS retreat_group_leader,
                   r.lookup_key_hash, r.privacy_consent_agreed, r.fee_paid,
                   r.status, r.admin_memo, r.newcomer, r.care_target,
                   COALESCE(ci.checked_in, FALSE) AS checked_in, r.created_at, r.updated_at,
                   r.attendance_type,
                   r.planned_arrival_at, r.planned_departure_at, r.partial_attendance_note,
                   r.lodging_night1, r.lodging_night2,
                   r.attend_day1_morning, r.attend_day1_afternoon, r.attend_day1_worship,
                   r.attend_day2_morning, r.attend_day2_afternoon, r.attend_day2_worship,
                   r.attend_day3_morning, r.attend_day3_afternoon,
                   r.inbound_transportation_method, r.inbound_carpool_available, r.inbound_carpool_seats,
                   r.inbound_carpool_area, r.inbound_carpool_route_area, r.inbound_carpool_note,
                   r.inbound_carpool_preferred_area, r.inbound_carpool_preferred_note, r.inbound_worship_bus_ride_slot,
                   r.outbound_transportation_method, r.outbound_carpool_available, r.outbound_carpool_seats,
                   r.outbound_carpool_area, r.outbound_carpool_route_area, r.outbound_carpool_note,
                   r.outbound_carpool_preferred_area, r.outbound_carpool_preferred_note, r.outbound_worship_bus_ride_slot
            FROM registrations r
            LEFT JOIN church_cells cc ON cc.id = r.church_cell_id
            LEFT JOIN church_middle_groups mg ON mg.id = cc.church_middle_group_id
            LEFT JOIN retreat_group_members rgm ON rgm.registration_id = r.id
            LEFT JOIN retreat_groups rg ON rg.id = rgm.retreat_group_id
            LEFT JOIN retreat_check_ins ci ON ci.participant_id = r.id
            <where>
                <if test="keyword != null and keyword != ''">
                    AND (
                        lower(r.name) LIKE lower(concat('%', #{keyword}, '%'))
                        OR r.phone_last_four = #{keyword}
                        OR lower(r.church_cell_department) LIKE lower(concat('%', #{keyword}, '%'))
                        OR lower(cc.name) LIKE lower(concat('%', #{keyword}, '%'))
                        OR lower(mg.name) LIKE lower(concat('%', #{keyword}, '%'))
                        OR lower(rg.name) LIKE lower(concat('%', #{keyword}, '%'))
                    )
                </if>
                <if test="status != null">
                    AND r.status = #{status}
                </if>
                <if test="feePaid != null">
                    AND r.fee_paid = #{feePaid}
                </if>
                <if test="newcomer != null">
                    AND r.newcomer = #{newcomer}
                </if>
                <if test="careTarget != null">
                    AND r.care_target = #{careTarget}
                </if>
                <if test="checkedIn != null">
                    AND COALESCE(ci.checked_in, FALSE) = #{checkedIn}
                </if>
                <if test="retreatGroupAssigned != null and retreatGroupAssigned">
                    AND rg.id IS NOT NULL
                </if>
                <if test="retreatGroupAssigned != null and !retreatGroupAssigned">
                    AND rg.id IS NULL
                </if>
                <if test="churchCellAssigned != null and churchCellAssigned">
                    AND r.church_cell_id IS NOT NULL
                </if>
                <if test="churchCellAssigned != null and !churchCellAssigned">
                    AND r.church_cell_id IS NULL
                </if>
                <if test="attendanceType != null">
                    AND r.attendance_type = #{attendanceType}
                </if>
                <if test="transportationNeed != null and transportationNeed == 'CARPOOL_NEEDED'">
                    AND (
                        r.inbound_transportation_method = 'CARPOOL_NEEDED'
                        OR r.outbound_transportation_method = 'CARPOOL_NEEDED'
                    )
                </if>
                <if test="transportationNeed != null and transportationNeed == 'CARPOOL_AVAILABLE'">
                    AND (
                        r.inbound_carpool_available = TRUE
                        OR r.outbound_carpool_available = TRUE
                    )
                </if>
            </where>
            <choose>
                <when test="sort == 'name_asc'">
                    ORDER BY r.name ASC, r.id ASC
                </when>
                <when test="sort == 'fee_unpaid_first'">
                    ORDER BY r.fee_paid ASC, r.created_at DESC, r.id DESC
                </when>
                <when test="sort == 'check_in_pending_first'">
                    ORDER BY COALESCE(ci.checked_in, FALSE) ASC, r.created_at DESC, r.id DESC
                </when>
                <when test="sort == 'group_asc'">
                    ORDER BY rg.display_order ASC NULLS LAST, rg.name ASC NULLS LAST, r.name ASC, r.id ASC
                </when>
                <otherwise>
                    ORDER BY r.created_at DESC, r.id DESC
                </otherwise>
            </choose>
            LIMIT #{limit}
            OFFSET #{offset}
            </script>
            """)
    @ConstructorArgs({
            @Arg(column = "id", javaType = Long.class),
            @Arg(column = "name", javaType = String.class),
            @Arg(column = "normalized_name", javaType = String.class),
            @Arg(column = "gender", javaType = Gender.class),
            @Arg(column = "birth_year", javaType = Integer.class),
            @Arg(column = "phone_number", javaType = String.class),
            @Arg(column = "phone_last_four", javaType = String.class),
            @Arg(column = "church_cell_department", javaType = String.class),
            @Arg(column = "church_cell_id", javaType = Long.class),
            @Arg(column = "church_cell_name", javaType = String.class),
            @Arg(column = "middle_group_id", javaType = Long.class),
            @Arg(column = "middle_group_name", javaType = String.class),
            @Arg(column = "retreat_group_id", javaType = Long.class),
            @Arg(column = "retreat_group_name", javaType = String.class),
            @Arg(column = "retreat_group_leader", javaType = Boolean.class),
            @Arg(column = "lookup_key_hash", javaType = String.class),
            @Arg(column = "privacy_consent_agreed", javaType = Boolean.class),
            @Arg(column = "fee_paid", javaType = Boolean.class),
            @Arg(column = "status", javaType = RegistrationStatus.class),
            @Arg(column = "admin_memo", javaType = String.class),
            @Arg(column = "newcomer", javaType = Boolean.class),
            @Arg(column = "care_target", javaType = Boolean.class),
            @Arg(column = "checked_in", javaType = Boolean.class),
            @Arg(column = "created_at", javaType = OffsetDateTime.class),
            @Arg(column = "updated_at", javaType = OffsetDateTime.class),
            @Arg(column = "attendance_type", javaType = AttendanceType.class),
            @Arg(column = "planned_arrival_at", javaType = OffsetDateTime.class),
            @Arg(column = "planned_departure_at", javaType = OffsetDateTime.class),
            @Arg(column = "partial_attendance_note", javaType = String.class),
            @Arg(column = "lodging_night1", javaType = Boolean.class),
            @Arg(column = "lodging_night2", javaType = Boolean.class),
            @Arg(column = "attend_day1_morning", javaType = Boolean.class),
            @Arg(column = "attend_day1_afternoon", javaType = Boolean.class),
            @Arg(column = "attend_day1_worship", javaType = Boolean.class),
            @Arg(column = "attend_day2_morning", javaType = Boolean.class),
            @Arg(column = "attend_day2_afternoon", javaType = Boolean.class),
            @Arg(column = "attend_day2_worship", javaType = Boolean.class),
            @Arg(column = "attend_day3_morning", javaType = Boolean.class),
            @Arg(column = "attend_day3_afternoon", javaType = Boolean.class),
            @Arg(column = "inbound_transportation_method", javaType = TransportationMethod.class),
            @Arg(column = "inbound_carpool_available", javaType = Boolean.class),
            @Arg(column = "inbound_carpool_seats", javaType = Integer.class),
            @Arg(column = "inbound_carpool_area", javaType = String.class),
            @Arg(column = "inbound_carpool_route_area", javaType = String.class),
            @Arg(column = "inbound_carpool_note", javaType = String.class),
            @Arg(column = "inbound_carpool_preferred_area", javaType = String.class),
            @Arg(column = "inbound_carpool_preferred_note", javaType = String.class),
            @Arg(column = "inbound_worship_bus_ride_slot", javaType = WorshipBusRideSlot.class),
            @Arg(column = "outbound_transportation_method", javaType = TransportationMethod.class),
            @Arg(column = "outbound_carpool_available", javaType = Boolean.class),
            @Arg(column = "outbound_carpool_seats", javaType = Integer.class),
            @Arg(column = "outbound_carpool_area", javaType = String.class),
            @Arg(column = "outbound_carpool_route_area", javaType = String.class),
            @Arg(column = "outbound_carpool_note", javaType = String.class),
            @Arg(column = "outbound_carpool_preferred_area", javaType = String.class),
            @Arg(column = "outbound_carpool_preferred_note", javaType = String.class),
            @Arg(column = "outbound_worship_bus_ride_slot", javaType = WorshipBusRideSlot.class)
    })
    List<Registration> findPage(
            @Param("keyword") String keyword,
            @Param("status") RegistrationStatus status,
            @Param("feePaid") Boolean feePaid,
            @Param("newcomer") Boolean newcomer,
            @Param("careTarget") Boolean careTarget,
            @Param("checkedIn") Boolean checkedIn,
            @Param("retreatGroupAssigned") Boolean retreatGroupAssigned,
            @Param("churchCellAssigned") Boolean churchCellAssigned,
            @Param("attendanceType") AttendanceType attendanceType,
            @Param("transportationNeed") String transportationNeed,
            @Param("sort") String sort,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM registrations r
            LEFT JOIN church_cells cc ON cc.id = r.church_cell_id
            LEFT JOIN church_middle_groups mg ON mg.id = cc.church_middle_group_id
            LEFT JOIN retreat_group_members rgm ON rgm.registration_id = r.id
            LEFT JOIN retreat_groups rg ON rg.id = rgm.retreat_group_id
            LEFT JOIN retreat_check_ins ci ON ci.participant_id = r.id
            <where>
                <if test="keyword != null and keyword != ''">
                    AND (
                        lower(r.name) LIKE lower(concat('%', #{keyword}, '%'))
                        OR r.phone_last_four = #{keyword}
                        OR lower(r.church_cell_department) LIKE lower(concat('%', #{keyword}, '%'))
                        OR lower(cc.name) LIKE lower(concat('%', #{keyword}, '%'))
                        OR lower(mg.name) LIKE lower(concat('%', #{keyword}, '%'))
                        OR lower(rg.name) LIKE lower(concat('%', #{keyword}, '%'))
                    )
                </if>
                <if test="status != null">
                    AND r.status = #{status}
                </if>
                <if test="feePaid != null">
                    AND r.fee_paid = #{feePaid}
                </if>
                <if test="newcomer != null">
                    AND r.newcomer = #{newcomer}
                </if>
                <if test="careTarget != null">
                    AND r.care_target = #{careTarget}
                </if>
                <if test="checkedIn != null">
                    AND COALESCE(ci.checked_in, FALSE) = #{checkedIn}
                </if>
                <if test="retreatGroupAssigned != null and retreatGroupAssigned">
                    AND rg.id IS NOT NULL
                </if>
                <if test="retreatGroupAssigned != null and !retreatGroupAssigned">
                    AND rg.id IS NULL
                </if>
                <if test="churchCellAssigned != null and churchCellAssigned">
                    AND r.church_cell_id IS NOT NULL
                </if>
                <if test="churchCellAssigned != null and !churchCellAssigned">
                    AND r.church_cell_id IS NULL
                </if>
                <if test="attendanceType != null">
                    AND r.attendance_type = #{attendanceType}
                </if>
                <if test="transportationNeed != null and transportationNeed == 'CARPOOL_NEEDED'">
                    AND (
                        r.inbound_transportation_method = 'CARPOOL_NEEDED'
                        OR r.outbound_transportation_method = 'CARPOOL_NEEDED'
                    )
                </if>
                <if test="transportationNeed != null and transportationNeed == 'CARPOOL_AVAILABLE'">
                    AND (
                        r.inbound_carpool_available = TRUE
                        OR r.outbound_carpool_available = TRUE
                    )
                </if>
            </where>
            </script>
            """)
    long countAll(
            @Param("keyword") String keyword,
            @Param("status") RegistrationStatus status,
            @Param("feePaid") Boolean feePaid,
            @Param("newcomer") Boolean newcomer,
            @Param("careTarget") Boolean careTarget,
            @Param("checkedIn") Boolean checkedIn,
            @Param("retreatGroupAssigned") Boolean retreatGroupAssigned,
            @Param("churchCellAssigned") Boolean churchCellAssigned,
            @Param("attendanceType") AttendanceType attendanceType,
            @Param("transportationNeed") String transportationNeed
    );

    @Insert("""
            INSERT INTO registrations (
                name, normalized_name, gender, birth_year, phone_number, phone_last_four,
                church_cell_department, lookup_key_hash, privacy_consent_agreed, fee_paid, status,
                attendance_type,
                planned_arrival_at, planned_departure_at, partial_attendance_note,
                lodging_night1, lodging_night2,
                attend_day1_morning, attend_day1_afternoon, attend_day1_worship,
                attend_day2_morning, attend_day2_afternoon, attend_day2_worship,
                attend_day3_morning, attend_day3_afternoon,
                inbound_transportation_method, inbound_carpool_available, inbound_carpool_seats,
                inbound_carpool_area, inbound_carpool_route_area, inbound_carpool_note,
                inbound_carpool_preferred_area, inbound_carpool_preferred_note, inbound_worship_bus_ride_slot,
                outbound_transportation_method, outbound_carpool_available, outbound_carpool_seats,
                outbound_carpool_area, outbound_carpool_route_area, outbound_carpool_note,
                outbound_carpool_preferred_area, outbound_carpool_preferred_note, outbound_worship_bus_ride_slot
            )
            VALUES (
                #{name}, #{normalizedName}, #{gender}, #{birthYear}, #{phoneNumber}, #{phoneLastFour},
                #{churchCellDepartment}, #{lookupKeyHash}, #{privacyConsentAgreed}, #{feePaid}, #{status},
                #{attendanceType},
                #{plannedArrivalAt}, #{plannedDepartureAt}, #{partialAttendanceNote},
                #{lodgingNight1}, #{lodgingNight2},
                #{attendDay1Morning}, #{attendDay1Afternoon}, #{attendDay1Worship},
                #{attendDay2Morning}, #{attendDay2Afternoon}, #{attendDay2Worship},
                #{attendDay3Morning}, #{attendDay3Afternoon},
                #{inboundTransportationMethod}, #{inboundCarpoolAvailable}, #{inboundCarpoolSeats},
                #{inboundCarpoolArea}, #{inboundCarpoolRouteArea}, #{inboundCarpoolNote},
                #{inboundCarpoolPreferredArea}, #{inboundCarpoolPreferredNote}, #{inboundWorshipBusRideSlot},
                #{outboundTransportationMethod}, #{outboundCarpoolAvailable}, #{outboundCarpoolSeats},
                #{outboundCarpoolArea}, #{outboundCarpoolRouteArea}, #{outboundCarpoolNote},
                #{outboundCarpoolPreferredArea}, #{outboundCarpoolPreferredNote}, #{outboundWorshipBusRideSlot}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(RegistrationInsert registration);

    @Update("""
            UPDATE registrations
            SET name = #{name},
                normalized_name = #{normalizedName},
                gender = #{gender},
                birth_year = #{birthYear},
                phone_number = #{phoneNumber},
                phone_last_four = #{phoneLastFour},
                church_cell_department = #{churchCellDepartment},
                lookup_key_hash = #{lookupKeyHash},
                privacy_consent_agreed = #{privacyConsentAgreed},
                attendance_type = #{attendanceType},
                planned_arrival_at = #{plannedArrivalAt},
                planned_departure_at = #{plannedDepartureAt},
                partial_attendance_note = #{partialAttendanceNote},
                lodging_night1 = #{lodgingNight1},
                lodging_night2 = #{lodgingNight2},
                attend_day1_morning = #{attendDay1Morning},
                attend_day1_afternoon = #{attendDay1Afternoon},
                attend_day1_worship = #{attendDay1Worship},
                attend_day2_morning = #{attendDay2Morning},
                attend_day2_afternoon = #{attendDay2Afternoon},
                attend_day2_worship = #{attendDay2Worship},
                attend_day3_morning = #{attendDay3Morning},
                attend_day3_afternoon = #{attendDay3Afternoon},
                inbound_transportation_method = #{inboundTransportationMethod},
                inbound_carpool_available = #{inboundCarpoolAvailable},
                inbound_carpool_seats = #{inboundCarpoolSeats},
                inbound_carpool_area = #{inboundCarpoolArea},
                inbound_carpool_route_area = #{inboundCarpoolRouteArea},
                inbound_carpool_note = #{inboundCarpoolNote},
                inbound_carpool_preferred_area = #{inboundCarpoolPreferredArea},
                inbound_carpool_preferred_note = #{inboundCarpoolPreferredNote},
                inbound_worship_bus_ride_slot = #{inboundWorshipBusRideSlot},
                outbound_transportation_method = #{outboundTransportationMethod},
                outbound_carpool_available = #{outboundCarpoolAvailable},
                outbound_carpool_seats = #{outboundCarpoolSeats},
                outbound_carpool_area = #{outboundCarpoolArea},
                outbound_carpool_route_area = #{outboundCarpoolRouteArea},
                outbound_carpool_note = #{outboundCarpoolNote},
                outbound_carpool_preferred_area = #{outboundCarpoolPreferredArea},
                outbound_carpool_preferred_note = #{outboundCarpoolPreferredNote},
                outbound_worship_bus_ride_slot = #{outboundWorshipBusRideSlot},
                updated_at = now()
            WHERE id = #{id}
            """)
    int overwrite(RegistrationOverwrite registration);

    @Update("""
            UPDATE registrations
            SET gender = #{gender},
                birth_year = #{birthYear},
                phone_number = #{phoneNumber},
                phone_last_four = #{phoneLastFour},
                church_cell_department = #{churchCellDepartment},
                attendance_type = #{attendanceType},
                planned_arrival_at = #{plannedArrivalAt},
                planned_departure_at = #{plannedDepartureAt},
                partial_attendance_note = #{partialAttendanceNote},
                lodging_night1 = #{lodgingNight1},
                lodging_night2 = #{lodgingNight2},
                attend_day1_morning = #{attendDay1Morning},
                attend_day1_afternoon = #{attendDay1Afternoon},
                attend_day1_worship = #{attendDay1Worship},
                attend_day2_morning = #{attendDay2Morning},
                attend_day2_afternoon = #{attendDay2Afternoon},
                attend_day2_worship = #{attendDay2Worship},
                attend_day3_morning = #{attendDay3Morning},
                attend_day3_afternoon = #{attendDay3Afternoon},
                inbound_transportation_method = #{inboundTransportationMethod},
                inbound_carpool_available = #{inboundCarpoolAvailable},
                inbound_carpool_seats = #{inboundCarpoolSeats},
                inbound_carpool_area = #{inboundCarpoolArea},
                inbound_carpool_route_area = #{inboundCarpoolRouteArea},
                inbound_carpool_note = #{inboundCarpoolNote},
                inbound_carpool_preferred_area = #{inboundCarpoolPreferredArea},
                inbound_carpool_preferred_note = #{inboundCarpoolPreferredNote},
                inbound_worship_bus_ride_slot = #{inboundWorshipBusRideSlot},
                outbound_transportation_method = #{outboundTransportationMethod},
                outbound_carpool_available = #{outboundCarpoolAvailable},
                outbound_carpool_seats = #{outboundCarpoolSeats},
                outbound_carpool_area = #{outboundCarpoolArea},
                outbound_carpool_route_area = #{outboundCarpoolRouteArea},
                outbound_carpool_note = #{outboundCarpoolNote},
                outbound_carpool_preferred_area = #{outboundCarpoolPreferredArea},
                outbound_carpool_preferred_note = #{outboundCarpoolPreferredNote},
                outbound_worship_bus_ride_slot = #{outboundWorshipBusRideSlot},
                updated_at = now()
            WHERE id = #{id}
            """)
    int selfUpdate(RegistrationSelfUpdate registration);

    @Update("""
            UPDATE registrations
            SET fee_paid = #{feePaid},
                updated_at = now()
            WHERE id = #{id}
            """)
    int updateFeePaid(@Param("id") Long id, @Param("feePaid") Boolean feePaid);

    @Update("""
            UPDATE registrations
            SET status = #{status},
                updated_at = now()
            WHERE id = #{id}
            """)
    int updateStatus(@Param("id") Long id, @Param("status") RegistrationStatus status);

    @Update("""
            UPDATE registrations
            SET admin_memo = #{adminMemo},
                newcomer = #{newcomer},
                care_target = #{careTarget},
                updated_at = now()
            WHERE id = #{id}
            """)
    int updateManagement(RegistrationManagementUpdate registration);

    @Update("""
            UPDATE registrations
            SET church_cell_id = #{churchCellId},
                updated_at = now()
            WHERE id = #{id}
            """)
    int updateChurchCell(@Param("id") Long id, @Param("churchCellId") Long churchCellId);

    class RegistrationInsert {
        private Long id;
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

    record RegistrationOverwrite(
            Long id,
            String name,
            String normalizedName,
            Gender gender,
            Integer birthYear,
            String phoneNumber,
            String phoneLastFour,
            String churchCellDepartment,
            String lookupKeyHash,
            Boolean privacyConsentAgreed,
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
    }

    record RegistrationSelfUpdate(
            Long id,
            Gender gender,
            Integer birthYear,
            String phoneNumber,
            String phoneLastFour,
            String churchCellDepartment,
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
    }

    record RegistrationManagementUpdate(
            Long id,
            String adminMemo,
            Boolean newcomer,
            Boolean careTarget
    ) {
    }

}
