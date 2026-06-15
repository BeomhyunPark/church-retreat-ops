package com.gmc.retreat.registration.mapper;

import com.gmc.retreat.registration.domain.Gender;
import com.gmc.retreat.registration.domain.Registration;
import com.gmc.retreat.registration.domain.RegistrationStatus;
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
            SELECT id, name, normalized_name, gender, birth_year, phone_number, phone_last_four,
                   church_cell_department, lookup_key_hash, privacy_consent_agreed, fee_paid,
                   status, created_at, updated_at
            FROM registrations
            WHERE id = #{id}
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
            @Arg(column = "lookup_key_hash", javaType = String.class),
            @Arg(column = "privacy_consent_agreed", javaType = Boolean.class),
            @Arg(column = "fee_paid", javaType = Boolean.class),
            @Arg(column = "status", javaType = RegistrationStatus.class),
            @Arg(column = "created_at", javaType = OffsetDateTime.class),
            @Arg(column = "updated_at", javaType = OffsetDateTime.class)
    })
    Optional<Registration> findById(@Param("id") Long id);

    @Select("""
            SELECT id, name, normalized_name, gender, birth_year, phone_number, phone_last_four,
                   church_cell_department, lookup_key_hash, privacy_consent_agreed, fee_paid,
                   status, created_at, updated_at
            FROM registrations
            WHERE normalized_name = #{normalizedName}
              AND phone_number = #{phoneNumber}
              AND status = 'REGISTERED'
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
            @Arg(column = "lookup_key_hash", javaType = String.class),
            @Arg(column = "privacy_consent_agreed", javaType = Boolean.class),
            @Arg(column = "fee_paid", javaType = Boolean.class),
            @Arg(column = "status", javaType = RegistrationStatus.class),
            @Arg(column = "created_at", javaType = OffsetDateTime.class),
            @Arg(column = "updated_at", javaType = OffsetDateTime.class)
    })
    Optional<Registration> findActiveByNormalizedNameAndPhoneNumber(
            @Param("normalizedName") String normalizedName,
            @Param("phoneNumber") String phoneNumber
    );

    @Select("""
            SELECT id, name, normalized_name, gender, birth_year, phone_number, phone_last_four,
                   church_cell_department, lookup_key_hash, privacy_consent_agreed, fee_paid,
                   status, created_at, updated_at
            FROM registrations
            WHERE normalized_name = #{normalizedName}
              AND phone_last_four = #{phoneLastFour}
              AND status = 'REGISTERED'
            ORDER BY id ASC
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
            @Arg(column = "lookup_key_hash", javaType = String.class),
            @Arg(column = "privacy_consent_agreed", javaType = Boolean.class),
            @Arg(column = "fee_paid", javaType = Boolean.class),
            @Arg(column = "status", javaType = RegistrationStatus.class),
            @Arg(column = "created_at", javaType = OffsetDateTime.class),
            @Arg(column = "updated_at", javaType = OffsetDateTime.class)
    })
    List<Registration> findActiveByNormalizedNameAndPhoneLastFour(
            @Param("normalizedName") String normalizedName,
            @Param("phoneLastFour") String phoneLastFour
    );

    @Select("""
            SELECT id, name, normalized_name, gender, birth_year, phone_number, phone_last_four,
                   church_cell_department, lookup_key_hash, privacy_consent_agreed, fee_paid,
                   status, created_at, updated_at
            FROM registrations
            ORDER BY created_at DESC, id DESC
            LIMIT #{limit}
            OFFSET #{offset}
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
            @Arg(column = "lookup_key_hash", javaType = String.class),
            @Arg(column = "privacy_consent_agreed", javaType = Boolean.class),
            @Arg(column = "fee_paid", javaType = Boolean.class),
            @Arg(column = "status", javaType = RegistrationStatus.class),
            @Arg(column = "created_at", javaType = OffsetDateTime.class),
            @Arg(column = "updated_at", javaType = OffsetDateTime.class)
    })
    List<Registration> findPage(@Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM registrations")
    long countAll();

    @Insert("""
            INSERT INTO registrations (
                name, normalized_name, gender, birth_year, phone_number, phone_last_four,
                church_cell_department, lookup_key_hash, privacy_consent_agreed, fee_paid, status
            )
            VALUES (
                #{name}, #{normalizedName}, #{gender}, #{birthYear}, #{phoneNumber}, #{phoneLastFour},
                #{churchCellDepartment}, #{lookupKeyHash}, #{privacyConsentAgreed}, #{feePaid}, #{status}
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
                updated_at = now()
            WHERE id = #{id}
            """)
    int selfUpdate(RegistrationSelfUpdate registration);

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
                RegistrationStatus status
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
            Boolean privacyConsentAgreed
    ) {
    }

    record RegistrationSelfUpdate(
            Long id,
            Gender gender,
            Integer birthYear,
            String phoneNumber,
            String phoneLastFour,
            String churchCellDepartment
    ) {
    }

}
