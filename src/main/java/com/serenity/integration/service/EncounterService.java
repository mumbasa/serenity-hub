package com.serenity.integration.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Service;

import com.serenity.integration.models.Encounter;
import com.serenity.integration.repository.EncounterRepository;
@Service
public class EncounterService {

    @Autowired
    EncounterRepository encounterRepository;
    @Autowired
    @Qualifier(value = "hisJdbcTemplate")
    JdbcTemplate hisJdbcTemplate;

    Logger LOGGER = LoggerFactory.getLogger(this.getClass().getCanonicalName());

    public void encounter() {

        List<Encounter> notes = new ArrayList<>();
        String sqlQuery = "SELECT " +
                "    pmh.Transaction_ID AS \"uuid\", " +
                "    DATE_FORMAT(pmh.DateOfVisit, '%Y-%m-%dT%H:%i:%sZ') AS \"created_at\", " +
                "    DATE_FORMAT(pmh.Updatedate, '%Y-%m-%dT%H:%i:%sZ') AS \"updated_at\", " +
                "    CASE " +
                "        WHEN pmh.Type = 'IPD' AND pmh.Admission_Type = 'Emergency' THEN 'emergency' " +
                "        WHEN pmh.Type = 'IPD' AND pmh.Admission_Type <> 'Emergency' THEN 'inpatient-encounter' " +
                "        ELSE 'ambulatory' " +
                "    END AS encounter_class, " +
                "    'finished' AS status, " +
                "    '' AS display, " +
                "    CASE " +
                "        WHEN pmh.Type = 'IPD' AND pmh.Admission_Type = 'Emergency' THEN 'stat' " +
                "        WHEN pmh.Type = 'IPD' AND pmh.Admission_Type <> 'Emergency' THEN 'ASAP' " +
                "        ELSE 'routine' " +
                "    END AS priority, " +
                "    DATE_FORMAT(CAST(CONCAT(CAST(app.Date AS DATE), ' ', CAST(app.Time AS TIME)) AS DATETIME), '%Y-%m-%dT%H:%i:%sZ') AS planned_start, "
                +
                "    DATE_FORMAT(CAST(CONCAT(CAST(app.Date AS DATE), ' ', CAST(app.EndTime AS TIME)) AS DATETIME), '%Y-%m-%dT%H:%i:%sZ') AS planned_end, "
                +
                "    DATE_FORMAT(CAST(CONCAT(CAST(pmh.DateOfVisit AS DATE), ' ', CAST(pmh.Time AS TIME)) AS DATETIME), '%Y-%m-%dT%H:%i:%sZ') AS started_at, "
                +
                "    NULL AS ended_at, " +
                "    pmh.Transaction_ID AS external_id, " +
                "    app.App_ID AS appointment_id, " +
                "    NULL AS location_id, " +
                "    'Nyaho Medical Centre' AS location_name, " +
                "    NULL AS service_type_id, " +
                "    NULL AS service_type_name, " +
                "    NULL AS service_provider_id, " +
                "    pmh.Patient_ID AS patient_mr_number, " +
                "    pmh.Patient_ID AS patient_id, " +
                "    CONCAT(pm.PfirstName, ' ', pm.PLastName) AS patient_full_name, " +
                "    pm.Mobile AS patient_mobile, " +
                "    pm.DOB AS patient_birth_date, " +
                "    pm.Gender AS patient_gender, " +
                "    'departed' AS patient_status, " +
                "    pmh.Doctor_ID AS created_by_id, " +
                "    CONCAT(dm.Title, ' ', dm.Name) AS created_by_name, " +
                "    pmh.Transaction_ID AS user_friendly_id, " +
                "    CONCAT(dm.Title, ' ', dm.Name) AS assigned_to_name, " +
                "    pmh.Doctor_ID AS assigned_to_id, " +
                "    'Nyaho Medical Centre' AS location_name " +
                "FROM " +
                "    patient_medical_history pmh " +
                "INNER JOIN " +
                "    patient_master pm ON pm.Patient_ID = pmh.Patient_ID " +
                "INNER JOIN " +
                "    doctor_master dm ON pmh.Doctor_ID = dm.Doctor_ID " +
                "INNER JOIN " +
                "    f_ledgertransaction lt ON lt.`Transaction_ID` = pmh.`Transaction_ID` " +
                "INNER JOIN " +
                "    appointment app ON app.ledgertnxNo = lt.LedgerTransactionNo LIMIT 1000;";

        SqlRowSet set = hisJdbcTemplate.queryForRowSet(sqlQuery);
        while (set.next()) {
            Encounter note = new Encounter();
            note.setUuid(set.getString(1));
            note.setCreatedAt(LocalDateTime.parse(set.getString(2)));
            note.setEncounterClass(set.getString(4));
            note.setStatus(set.getString(5));
            note.setPriority(set.getString(7));
            note.setPlannedStart(LocalDateTime.parse(set.getString(8)));
            note.setPlannedEnd(LocalDateTime.parse(set.getString(9)));
            note.setStartedAt(LocalDateTime.parse(set.getString(10)));
            note.setEndedAt(null);
            note.setExternalId(set.getString(13));
            note.setAppointmentId(set.getString(14));
            note.setLocationId(null);
            note.setLocationName(set.getString(16));
            note.setPatientMrNumber(set.getString(20));
            note.setPatientId(set.getString(21));
            note.setPatientFullName(set.getString(22));
            note.setPatientMobile(set.getString(23));
            note.setPatientBirthDate(set.getString(24));
            note.setPatientGender(set.getString(26));
            note.setPatientStatus(set.getString(27));
            note.setCreatedById(set.getString(28));
            note.setCreatedByName(set.getString(29));
            note.setUserFriendlyId(set.getString(30));
            note.setAssignedToName(set.getString(31));
            note.setAssignedToId(set.getString(32));

            notes.add(note);
        }
        int rounds = Math.round(notes.size() / 1000);
        for (int i = 0; i < rounds; i++) {
            LOGGER.info("adding round presenting illness " + rounds);
            try {
                encounterRepository.saveAllAndFlush(notes.subList(i * 1000, (i * 1000) + 1000));
            } catch (Exception e) {

            }

        }

    }

}
