package com.serenity.integration.service;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Service;

import com.serenity.integration.models.Doctors;
import com.serenity.integration.models.Encounter;
import com.serenity.integration.models.EncounterNote;
import com.serenity.integration.models.PatientData;
import com.serenity.integration.models.Visits;
import com.serenity.integration.repository.DoctorRepository;
import com.serenity.integration.repository.EncounterNoteRepository;
import com.serenity.integration.repository.EncounterRepository;
import com.serenity.integration.repository.PatientRepository;
import com.serenity.integration.repository.VisitRepository;

@Service
public class NoteService {
    @Autowired
    @Qualifier(value = "hisJdbcTemplate")
    JdbcTemplate hisJdbcTemplate;

    @Autowired
    @Qualifier("serenityJdbcTemplate")
    JdbcTemplate serenityJdbcTemplate;

    @Autowired
    @Qualifier("vectorJdbcTemplate")
    JdbcTemplate vectorJdbcTemplate;

    @Autowired
    EncounterNoteRepository encounterNoteRepository;
    Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    VisitRepository visitRepository;

    @Autowired
    PatientRepository patientRepository;

    @Autowired
    DoctorRepository doctorRepository;

    @Autowired
    EncounterRepository encounterRepository;

    @Autowired
    @Qualifier(value = "legJdbcTemplate")
    JdbcTemplate legJdbcTemplate;

    public List<EncounterNote> getHisNote(List<String> numbers) {
        List<EncounterNote> notes = new ArrayList<>();
        String queryDetails = String.join("' OR source.patient_mr_number='", numbers.toArray(String[]::new));
        String sql = "SELECT `source`.`created_at` AS `created_at`, " +
                "`source`.`updated_at` AS `updated_at`, " +
                "`source`.`note` AS `note`, " +
                "`source`.`note_type` AS `note_type`, " +
                "`source`.`encounter_date` AS `encounter_date`, " +
                "`source`.`patient_mr_number` AS `patient_mr_number`, " +
                "`source`.`encounter_type` AS `encounter_type`, " +
                "`source`.`is_recalled` AS `is_recalled`, " +
                "`source`.`practitioner_role_type` AS `practitioner_role_type`, " +
                "`source`.`practitioner_name` AS `practitioner_name`, " +
                "`source`.`practitioner_id` AS `practitioner_id`, " +
                "`source`.`is_edited` AS `is_edited`, " +
                "`pm`.`PName` AS `patient_name`, " +
                "`pm`.`Mobile` AS `patient_mobile`, " +
                "`pm`.`DOB` AS `patient_dob`, " +
                "`pm`.`Age` AS `patient_age`, " +
                "`pm`.`Gender` AS `patient_gender` " +
                "FROM ( " +
                "    SELECT CONCAT( " +
                "        progress_notes.TransactionId, " +
                "        \"_nyaho_his_nursing_doctorprogressnote_\", " +
                "        progress_notes.ID " +
                "    ) AS `uuid`, " +
                "    DATE_FORMAT(progress_notes.EntryDate, '%d-%b-%Y %l:%i %p') AS `created_at`, " +
                "    progress_notes.UpdateDate AS `updated_at`, " +
                "    progress_notes.ProgressNote AS `note`, " +
                "    FALSE AS `is_formatted`, " +
                "    'progress note' AS `note_type`, " +
                "    progress_notes.TransactionId AS `encounter_id`, " +
                "    DATE_FORMAT(progress_notes.NoteDate, '%d-%b-%Y %l:%i %p') AS `encounter_date`, " +
                "    patients.Patient_ID AS `patient_mr_number`, " +
                "    'progress note' AS `encounter_type`, " +
                "    FALSE AS `is_edited`, " +
                "    FALSE AS `is_recalled`, " +
                "    'unknown' AS `practitioner_role_type`, " +
                "    CONCAT(practitioners.title, ' ', practitioners.Name) AS `practitioner_name`, " +
                "    progress_notes.UserID AS `practitioner_id` " +
                "FROM nursing_doctorprogressnote AS progress_notes " +
                "INNER JOIN patient_ipd_profile AS admissions " +
                "    ON admissions.Transaction_ID = progress_notes.TransactionId " +
                "INNER JOIN patient_master AS patients " +
                "    ON admissions.PatientID = patients.Patient_ID " +
                "LEFT JOIN employee_master AS practitioners " +
                "    ON progress_notes.UserID = practitioners.Employee_ID " +
                "ORDER BY progress_notes.EntryDate DESC " +
                ") AS `source` " +
                "LEFT JOIN `patient_master` AS `pm` " +
                "    ON `source`.`patient_mr_number` = `pm`.`Patient_ID` " +
                "WHERE source.patient_mr_number ='" + queryDetails + "'";
        SqlRowSet set = hisJdbcTemplate.queryForRowSet(sql);
        while (set.next()) {
            System.err.println(set.getString(1));
            EncounterNote note = new EncounterNote();
            note.setUuid(UUID.randomUUID().toString());
            note.setEncounterDate(set.getString(1));

            note.setCreatedAt(set.getString(1));
            note.setUpdatedAt(set.getString(2));
            note.setNote(set.getString(3));
            note.setNoteType(set.getString(4));
            note.setEncounterDate(set.getString(5));
            note.setPatientMrNumber(set.getString(6));
            note.setRecalled(set.getBoolean(7));
            note.setPractitionerRoleType(set.getString(8));
            note.setPractitionerName(set.getString(9));
            note.setPractitionerId(set.getString(10));
            note.setEdited(set.getBoolean(11));
            note.setExternalSystem("his");
            notes.add(note);
        }

        encounterNoteRepository.saveAllAndFlush(notes);
        return notes;

    }

    public int getChiefNotes() {

        Map<String, PatientData> patientDataMap = patientRepository.findAll().stream()
                .collect(Collectors.toMap(e -> e.getExternalId(), e -> e));
        Map<String, String> doctorMap = doctorRepository.findHisPractitioners().stream()
                .collect(Collectors.toMap(e -> e.getExternalId(), e -> e.getSerenityUUid()));

        final String sqlQuery = """
                        SELECT Transaction_ID AS "uuid",
                  Transaction_ID AS "encounter_id",
                  PatientID AS "patient_mr_number",
                  MainComplaint AS "note",
                  EntryBy AS "practitioner_id",
                  EntryDate AS "encounter_date",
                  NULL AS "created_at",
                  NULL AS "updated_at",
                  "chief-complaint" AS "note_type",
                  "outpatient-consultation" AS "encounter_type",
                  FALSE AS is_edited,
                  FALSE AS is_recalled,
                  'unknown' AS practitioner_role_type,
                  CONCAT(practitioners.title, ' ', practitioners.Name) AS "practitioner_name",
                  NULL AS "edit_history"
                FROM cpoe_hpexam
                  LEFT JOIN employee_master AS practitioners ON cpoe_hpexam.EntryBy = practitioners.Employee_ID
                where MainComplaint <> ''

                    """;

        List<EncounterNote> notes = new ArrayList<>();
        SqlRowSet rs = hisJdbcTemplate.queryForRowSet(sqlQuery);
        while (rs.next()) {
            EncounterNote note = new EncounterNote();
            note.setUuid(UUID.randomUUID().toString());
            note.setEncounterId(note.getUuid());
            note.setCreatedAt(cleanString(rs.getString("created_at")));
            note.setUpdatedAt(cleanString(rs.getString("updated_at")));
            note.setNote(cleanString(rs.getString("note")));
            note.setNoteType(cleanString(rs.getString("note_type")));
            note.setEncounterDate(cleanString(rs.getString("encounter_date")));
            note.setPatientMrNumber(patientDataMap.get(rs.getString("patient_mr_number")).getMrNumber());
            note.setPatientId(patientDataMap.get(rs.getString("patient_mr_number")).getUuid());
            note.setEncounterType(cleanString(rs.getString("encounter_type")));
            note.setRecalled(false);
            note.setPatientGender(patientDataMap.get(rs.getString("patient_mr_number")).getGender());
            note.setPatientBirthDate(patientDataMap.get(rs.getString("patient_mr_number")).getBirthDate());
            note.setPatientFullName(patientDataMap.get(rs.getString("patient_mr_number")).getFullName());
            note.setPatientMobile(patientDataMap.get(rs.getString("patient_mr_number")).getMobile());
            note.setPractitionerName(cleanString(rs.getString("practitioner_name")));
            note.setHisVisitId(rs.getString("uuid"));
            try {
                note.setPractitionerId(doctorMap.get(rs.getString("practitioner_id")));
            } catch (Exception e) {

            }
            note.setExternalId(rs.getString("uuid"));
            note.setEdited(false);
            note.setExternalSystem("his");
            note.setLocationId("23f59485-8518-4f4e-9146-d061dfe58175");
            note.setLocationName("Airport Primary Care");
            notes.add(note);

        }

        ExecutorService executorService = Executors.newFixedThreadPool(10);
        try {
            List<Future<Integer>> futures = executorService.invokeAll(submitNote(notes, 20000));
            for (Future<Integer> future : futures) {
                System.out.println("future.get = " + future.get());
            }
        } catch (InterruptedException | ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        executorService.shutdown();
        System.err.println("patiend count is ");

        return 1;
    }

    public Set<Callable<Integer>> submitNote(List<EncounterNote> notes, int batchSize) {

        Set<Callable<Integer>> callables = new HashSet<>();
        int totalSize = notes.size();
        int batches = (totalSize + batchSize - 1) / batchSize; // Ceiling division

        for (int i = 0; i < batches; i++) {
            final int batchNumber = i; // For use in lambda

            callables.add(() -> {
                int startIndex = batchNumber * batchSize;
                int endIndex = Math.min(startIndex + batchSize, totalSize);
                logger.debug("Processing batch {}/{}, indices [{}]",
                        batchNumber + 1, batches, startIndex);
                encounterNoteRepository.saveAll(notes.subList(startIndex, endIndex));
                return 1;
            });
        }

        return callables;
    }

    public void getPresentingIllness() {
        Map<String, PatientData> mps = patientRepository.findAll().stream()
                .collect(Collectors.toMap(e -> e.getExternalId(), e -> e));
        Map<String, String> doc = doctorRepository.findHisPractitioners().stream()
                .collect(Collectors.toMap(e -> e.getExternalId(), e -> e.getSerenityUUid()));

        List<EncounterNote> notes = new ArrayList<>();
        String query = "SELECT " +
                "    Transaction_ID AS \"uuid\", " +
                "    Transaction_ID AS \"encounter_id\", " +
                "    PatientID AS \"patient_mr_number\", " +
                "    ProgressionComplaint AS \"note\", " +
                "    EntryBy AS \"practitioner_id\", " +
                "    DATE_FORMAT(EntryDate, '%Y-%m-%dT%TZ') AS \"encounter_date\", " +
                "    NULL AS \"created_at\", " +
                "    NULL AS \"updated_at\", " +
                "    'history-of-presenting-illness' AS \"note-type\", " +
                "    'outpatient-consultation' AS \"encounter-type\", " +
                "    FALSE AS is_edited, " +
                "    FALSE AS is_recalled, " +
                "    'unknown' AS practitioner_role_type, " +
                "    CONCAT(practitioners.title, ' ', practitioners.Name) AS \"practitioner_name\", " +
                "    NULL AS \"edit_history\" " +
                "FROM " +
                "    cpoe_hpexam " +
                "LEFT JOIN " +
                "    employee_master AS practitioners " +
                "    ON cpoe_hpexam.EntryBy = practitioners.Employee_ID " +
                "WHERE " +
                "    ProgressionComplaint <> '' ";

        SqlRowSet set = hisJdbcTemplate.queryForRowSet(query);
        while (set.next()) {
            EncounterNote note = new EncounterNote();
            note.setUuid(UUID.randomUUID().toString());
            note.setEncounterId(note.getUuid());
            note.setCreatedAt(cleanString(set.getString(7)));
            note.setUpdatedAt(cleanString(set.getString(8)));
            note.setNote(cleanString(set.getString(4)));
            note.setNoteType(cleanString(set.getString(9)));
            note.setEncounterDate(cleanString(set.getString(6)));
            note.setPatientMrNumber(mps.get(set.getString(3)).getMrNumber());
            note.setEncounterType(cleanString(set.getString(10)));
            note.setPatientGender(mps.get(set.getString("patient_mr_number")).getGender());
            note.setPatientBirthDate(mps.get(set.getString("patient_mr_number")).getBirthDate());
            note.setPatientFullName(mps.get(set.getString("patient_mr_number")).getFullName());
            note.setPatientMobile(mps.get(set.getString("patient_mr_number")).getMobile());
            note.setPractitionerName(cleanString(set.getString("practitioner_name")));
            note.setRecalled(false);
            note.setPractitionerRoleType("doctor");
            try {
                note.setPractitionerName(set.getString("practitioner_name"));
                note.setPractitionerId(doc.get(set.getString("practitioner_id")));
            } catch (Exception e) {
                logger.info("Doctor not found");
            }
            note.setExternalId(set.getString("uuid"));
            note.setEdited(false);
            note.setExternalSystem("his");
            note.setLocationId("23f59485-8518-4f4e-9146-d061dfe58175");
            note.setLocationName("Airport Primary Care");
            notes.add(note);
        }
        logger.info("add presenting illness");

        ExecutorService executorService = Executors.newFixedThreadPool(10);
        try {
            List<Future<Integer>> futures = executorService.invokeAll(submitNote(notes, 20000));
            for (Future<Integer> future : futures) {
                System.out.println("future.get = " + future.get());
            }
        } catch (InterruptedException | ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        executorService.shutdown();
        System.err.println("patiend count is ");

    }

    public void getCarePlan() {
        List<EncounterNote> notes = new ArrayList<>();
        Map<String, PatientData> mps = patientRepository.findAll().stream()
                .collect(Collectors.toMap(e -> e.getExternalId(), e -> e));
        Map<String, String> doc = doctorRepository.findHisPractitioners().stream()
                .collect(Collectors.toMap(e -> e.getExternalId(), e -> e.getSerenityUUid()));

        String query = "SELECT " +
                "  cc.TransactionID AS \"uuid\", " +
                "  cc.TransactionID AS \"encounter_id\", " +
                "  cc.PatientID AS \"mr_number\", " +
                "  cc.CarePlan AS \"note\", " +
                "  cc.EntryBy AS \"practitioner_id\", " +
                "  DATE_FORMAT(cc.EntryDate,'%Y-%m-%dT%TZ') AS \"encounter_date\", " +
                "  NULL AS \"created_at\", " +
                "  DATE_FORMAT(cc.UpdateDate,'%Y-%m-%dT%TZ') AS \"updated_at\", " +
                "  'plan-of-care' AS \"note-type\", " +
                "  'outpatient-consultation' AS \"encounter_type\", " +
                "  FALSE AS \"is_edited\", " +
                "  FALSE AS \"is_recalled\", " +
                "  'unknown' AS \"practitioner_role_type\", " +
                "  CONCAT(em.title, ' ', em.Name) AS \"practitioner_name\", " +
                "  NULL AS \"edit_history\" " +
                "FROM " +
                "  cpoe_careplan cc " +
                "LEFT JOIN " +
                "  employee_master em " +
                "ON " +

                "  cc.EntryBy = em.Employee_ID";

        SqlRowSet set = hisJdbcTemplate.queryForRowSet(query);
        while (set.next()) {
            EncounterNote note = new EncounterNote();
            note.setUuid(UUID.randomUUID().toString());
            note.setEncounterId(note.getUuid());
            note.setCreatedAt(cleanString(set.getString(7)));
            note.setUpdatedAt(cleanString(set.getString(8)));
            note.setNote(cleanString(set.getString(4)));
            note.setNoteType(cleanString(set.getString(9)));
            note.setEncounterDate(cleanString(set.getString(6)));
            note.setPatientMrNumber(mps.get(set.getString(3)).getMrNumber());
            note.setEncounterType(cleanString(set.getString(10)));
            note.setPatientGender(mps.get(set.getString("mr_number")).getGender());
            note.setPatientBirthDate(mps.get(set.getString("mr_number")).getBirthDate());
            note.setPatientFullName(mps.get(set.getString("mr_number")).getFullName());
            note.setPatientMobile(mps.get(set.getString("mr_number")).getMobile());
            note.setRecalled(false);
            note.setPractitionerRoleType("doctor");
            note.setPractitionerName(cleanString(set.getString("practitioner_name")));
            note.setPractitionerId(doc.get(set.getString("practitioner_id")));
            note.setExternalId(set.getString("uuid"));
            note.setEdited(false);
            note.setExternalSystem("his");

            notes.add(note);
        }

        ExecutorService executorService = Executors.newFixedThreadPool(10);
        try {
            List<Future<Integer>> futures = executorService.invokeAll(submitNote(notes, 20000));
            for (Future<Integer> future : futures) {
                System.out.println("future.get = " + future.get());
            }
        } catch (InterruptedException | ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        executorService.shutdown();
        System.err.println("patiend count is ");
    }

    public void getProgressNote() {

        Map<String, PatientData> patientDataMap = patientRepository.findAll().stream()
                .collect(Collectors.toMap(e -> e.getExternalId(), e -> e));
        Map<String, String> doctorMap = doctorRepository.findHisPractitioners().stream()
                .collect(Collectors.toMap(e -> e.getExternalId(), e -> e.getSerenityUUid()));

        List<EncounterNote> notes = new ArrayList<>();
        String sqlQuery = "SELECT " +
                "    `source`.`created_at` AS `created_at`, " +
                "    `source`.`updated_at` AS `updated_at`, " +
                "    `source`.`note` AS `note`, " +
                "    `source`.`note_type` AS `note_type`, " +
                "    `source`.`encounter_date` AS `encounter_date`, " +
                "    `source`.`patient_mr_number` AS `patient_mr_number`, " +
                "    `source`.`encounter_type` AS `encounter_type`, " +
                "    `source`.`is_recalled` AS `is_recalled`, " +
                "    `source`.`practitioner_role_type` AS `practitioner_role_type`, " +
                "    `source`.`practitioner_name` AS `practitioner_name`, " +
                "    `source`.`practitioner_id` AS `practitioner_id`, " +
                "    `source`.`is_edited` AS `is_edited`, " +
                "    `pm`.`PName` AS `patient_name`, " +
                "    source.uuid AS `uuid` " +
                "FROM " +
                "( " +
                "    SELECT " +
                "        CONCAT(progress_notes.TransactionId, '_nyaho_his_nursing_doctorprogressnote_', progress_notes.ID) AS `uuid`, "
                +
                "        DATE_FORMAT(progress_notes.EntryDate, '%Y-%m-%dT%H:%i:%sZ') AS `created_at`, " +
                "        DATE_FORMAT(progress_notes.UpdateDate, '%Y-%m-%dT%H:%i:%sZ') AS `updated_at`, " +
                "        progress_notes.ProgressNote AS `note`, " +
                "        'progress-note' AS `note_type`, " +
                "        DATE_FORMAT(progress_notes.NoteDate, '%Y-%m-%dT%H:%i:%sZ') AS `encounter_date`, " +
                "        patients.Patient_ID AS `patient_mr_number`, " +
                "        'progress note' AS `encounter_type`, " +
                "        CONCAT(practitioners.title, ' ', practitioners.Name) AS `practitioner_name`, " +
                "        practitioners.Employee_ID AS `practitioner_id`, " +
                "        FALSE AS `is_edited`, " +
                "        FALSE AS `is_recalled`, " +
                "        'unknown' AS `practitioner_role_type` " +
                "    FROM " +
                "        nursing_doctorprogressnote AS progress_notes " +
                "    INNER JOIN " +
                "        patient_ipd_profile AS admissions " +
                "        ON admissions.Transaction_ID = progress_notes.TransactionId " +
                "    INNER JOIN " +
                "        patient_master AS patients " +
                "        ON admissions.PatientID = patients.Patient_ID " +
                "    LEFT JOIN " +
                "        employee_master AS practitioners " +
                "        ON progress_notes.UserID = practitioners.Employee_ID " +
                "    ORDER BY " +
                "        progress_notes.EntryDate DESC " +
                ") AS `source` " +
                "LEFT JOIN `patient_master` AS `pm` " +
                "ON `source`.`patient_mr_number` = `pm`.`Patient_ID`";

        SqlRowSet rs = hisJdbcTemplate.queryForRowSet(sqlQuery);
        while (rs.next()) {
            EncounterNote note = new EncounterNote();
            note.setUuid(UUID.randomUUID().toString());
            note.setEncounterId(note.getUuid());
            note.setCreatedAt(cleanString(rs.getString("created_at")));
            note.setUpdatedAt(cleanString(rs.getString("updated_at")));
            note.setNote(cleanString(rs.getString("note")));
            note.setNoteType(cleanString(rs.getString("note_type")));
            note.setEncounterDate(cleanString(rs.getString("encounter_date")));
            note.setPatientMrNumber(patientDataMap.get(rs.getString("patient_mr_number")).getMrNumber());
            note.setPatientId(patientDataMap.get(rs.getString("patient_mr_number")).getUuid());
            note.setEncounterType(cleanString(rs.getString("encounter_type")));
            note.setRecalled(false);
            note.setPatientGender(patientDataMap.get(rs.getString("patient_mr_number")).getGender());
            note.setPatientBirthDate(patientDataMap.get(rs.getString("patient_mr_number")).getBirthDate());
            note.setPatientFullName(patientDataMap.get(rs.getString("patient_mr_number")).getFullName());
            note.setPatientMobile(patientDataMap.get(rs.getString("patient_mr_number")).getMobile());
            note.setPractitionerName(cleanString(rs.getString("practitioner_name")));
            note.setHisVisitId(rs.getString("uuid"));
            try {
                note.setPractitionerId(doctorMap.get(rs.getString("practitioner_id")));
            } catch (Exception e) {

            }
            note.setExternalId(rs.getString("uuid"));
            note.setEdited(false);
            note.setExternalSystem("his");
            note.setLocationId("23f59485-8518-4f4e-9146-d061dfe58175");
            note.setLocationName("Airport Primary Care");
            notes.add(note);
        }

        ExecutorService executorService = Executors.newFixedThreadPool(10);
        try {
            List<Future<Integer>> futures = executorService.invokeAll(submitNote(notes, 20000));
            for (Future<Integer> future : futures) {
                System.out.println("future.get = " + future.get());
            }
        } catch (InterruptedException | ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        executorService.shutdown();
        System.err.println("patiend count is ");

    }

    public void saveNotes(List<EncounterNote> notes) {

        String sql = "INSERT INTO public.encounter_notes\n" + //
                "(created_at, updated_at, pk, encounter_id, patient_id,"+
                 "visit_id, \"uuid\", note, is_formatted, note_type,"+ 
                 "is_edited, is_recalled, practitioner_id, encounter_date, practitioner_name,"+
                  "practitioner_role_type, encounter_type, patient_mr_number)\n"
                + //
                "VALUES(to_timestamp(?, 'YYYY-MM-DD HH24:MI:SS'), to_timestamp(?, 'YYYY-MM-DD HH24:MI:SS'), ?, uuid(?), uuid(?)," +
               "uuid(?), uuid(?), ?, ?, ?,"+
                "?, ?, uuid(?), to_timestamp(?, 'YYYY-MM-DD HH24:MI:SS'), ?,"+ 
                "?, ?, ?);";

        serenityJdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {

            @Override
            public void setValues(@SuppressWarnings("null") PreparedStatement ps, int i) throws SQLException {
                EncounterNote note = notes.get(i);
                try{
                    ps.setString(1, notes.get(i).getEncounterDate().replaceAll("T|Z", " ").strip());
                    ps.setString(2, notes.get(i).getEncounterDate().replaceAll("T|Z", " ").strip());
                      
                }catch (Exception e){
                        ps.setString(1, notes.get(i).getCreatedAt()+" 14:55:37");
                        ps.setString(2, notes.get(i).getCreatedAt()+" 14:55:37");

    
                    }
                ps.setLong(3, note.getId());
                ps.setString(4, note.getEncounterId());
                ps.setString(5, note.getPatientId());
                ps.setString(6, note.getVisitId());
                ps.setString(7, note.getUuid());
                ps.setString(8, note.getNote());
                ps.setBoolean(9, false);
                ps.setString(10, note.getNoteType());
                ps.setBoolean(11, false);
                ps.setBoolean(12, false);
                ps.setString(13, note.getPractitionerId());
                try{
                ps.setString(14, note.getEncounterDate().replaceAll("T|Z", " ").strip());
                }catch(Exception e){
                    ps.setString(14, notes.get(i).getCreatedAt()+" 14:55:37");


                }
                ps.setString(15, note.getPractitionerName());
                ps.setString(16, "doctor");
                ps.setString(17, note.getEncounterType());
                ps.setString(18, note.getPatientMrNumber());
            

            }

            @Override
            public int getBatchSize() {
                // TODO Auto-generated method stub
                return notes.size();
            }

        });

    }

    public void noteThread() {
        logger.info("kooooooooooooooading");
        long dataSize =encounterNoteRepository.countByExternalSystem("opd");
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        try {
            List<Future<Integer>> futures = executorService.invokeAll(submitTask2(100, dataSize));
            for (Future<Integer> future : futures) {
                System.out.println("future.get = " + future.get());
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        executorService.shutdown();
        System.err.println("patiend count is " + dataSize);

    }



    public Set<Callable<Integer>> submitTask2(int batchSize, long rows) {

        Set<Callable<Integer>> callables = new HashSet<>();
        int totalSize = (int) rows;
        int batches = (totalSize + batchSize - 1) / batchSize; // Ceiling division

        for (int i = 0; i < batches; i++) {
            final int batchNumber = i; // For use in lambda


            callables.add(() -> {
                int startIndex = batchNumber * batchSize;
                int endIndex = Math.min(startIndex + batchSize, totalSize);
                logger.debug("Processing batch {}/{}, indices [{}]",
                        batchNumber + 1, batches, startIndex);
                        List<EncounterNote> notes = encounterNoteRepository.findOffset(startIndex);
                        System.err.println(notes.get(0).toString());

                try {
                    saveNotes(notes);
                } catch (Exception e) {
                    // TODO: handle exception
                    e.printStackTrace();
                    logger.info("error adding note");

                }

                return 1;
            });
        }

        return callables;
    }

    public String cleanString(String input) {
        if (input == null)
            return null;
        return input.replace("\0", "")
                .replace("\u0000", "");
    }

    public void cleanData() {
        String sql ="""
                
                                update encounternote
                set visitid =visits."uuid"
                from visits
                where encounternote.externalid=visits.externalid
                and encounternote.visitid is null;
                                """;

        vectorJdbcTemplate.update(sql);

    }

    public void truncate() {
        String sql = """
       update encounter 
set assigned_to_id =visits.practitionerid ,assigned_to_name =visits.assignedtoname 
from visits 
where split_part(externalid,'_',1)=visits.externalid 
and encounter.visit_id is null
                                        """;
                                String sql2 ="drop table encounter";
        vectorJdbcTemplate.execute(sql2);

       

    }

    public void getLegacyEncounters() {

        Map<String, PatientData> patientDataMap = patientRepository.findAll().stream()
                .collect(Collectors.toMap(e -> e.getExternalId(), e -> e));
        Map<String, String> doctorMap = doctorRepository.findHisPractitioners().stream()
                .collect(Collectors.toMap(e -> e.getExternalId(), e -> e.getSerenityUUid()));

        List<EncounterNote> encounters = new ArrayList<>();
        String sql = "select * from encounter e join patient p on p.id=e.patient_id";
        SqlRowSet set = legJdbcTemplate.queryForRowSet(sql);
        while (set.next()) {
            PatientData patient = patientDataMap.get(set.getString("mr_number"));
           // Optional<Visits> visit = visitRepository.findByExternalId(set.getString("visit_id"));
            if(set.getString("chief_complaint") != null){
            EncounterNote encounter = new EncounterNote();
            encounter.setUuid(UUID.randomUUID().toString());
            encounter.setEncounterId(set.getString(5));
            encounter.setExternalId(set.getString(5));
            encounter.setCreatedAt(set.getString(2));
            encounter.setEncounterType(set.getString("encounter_class"));
            encounter.setPatientId(patient.getUuid());
            encounter.setNoteType("chief-complaint");
            encounter.setPatientBirthDate(patient.getBirthDate());
            encounter.setPatientFullName(patient.getFullName());
            encounter.setPatientMobile(patient.getMobile());
            encounter.setPatientMrNumber(patient.getMrNumber());
            encounter.setExternalSystem("opd");
            encounter.setNote(set.getString("chief_complaint"));
            encounter.setLocationId(set.getString("primary_location_id"));
            encounter.setVisitId(set.getString("visit_id"));
            encounter.setServiceProviderId("161380e9-22d3-4627-a97f-0f918ce3e4a9");
            encounter.setServiceProviderName("Nyaho Medical Centre");
            encounters.add(encounter);
            }else if(set.getString("history_of_presenting_illness") !=null){

                EncounterNote encounter = new EncounterNote();
                encounter.setUuid(UUID.randomUUID().toString());
                encounter.setEncounterId(set.getString(5));
                encounter.setExternalId(set.getString(5));
                encounter.setCreatedAt(set.getString(2));
                encounter.setEncounterType(set.getString("encounter_class"));
                encounter.setPatientId(patient.getUuid());
                encounter.setNoteType("history-of-presenting-illness");
                encounter.setPatientBirthDate(patient.getBirthDate());
                encounter.setPatientFullName(patient.getFullName());
                encounter.setPatientMobile(patient.getMobile());
                encounter.setPatientMrNumber(patient.getMrNumber());
                encounter.setExternalSystem("opd");
                encounter.setNote(set.getString("history_of_presenting_illness"));
                encounter.setLocationId(set.getString("primary_location_id"));
                encounter.setVisitId(set.getString("visit_id"));
                encounter.setServiceProviderId("161380e9-22d3-4627-a97f-0f918ce3e4a9");
                encounter.setServiceProviderName("Nyaho Medical Centre");
                encounters.add(encounter);
            }
            logger.info("adding encounter");
        }

        ExecutorService executorService = Executors.newFixedThreadPool(10);
        try {
            List<Future<Integer>> futures = executorService.invokeAll(submitNote(encounters, 1000));
            for (Future<Integer> future : futures) {
                System.out.println("future.get = " + future.get());
            }
        } catch (InterruptedException | ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        executorService.shutdown();
        System.err.println("patiend count is ");

    }





    public void getLegacyVisitNotesEncounters() {

        Map<String, PatientData> patientDataMap = patientRepository.findAll().stream()
                .collect(Collectors.toMap(e -> e.getExternalId(), e -> e));
        Map<String, String> doctorMap = doctorRepository.findHisPractitioners().stream()
                .collect(Collectors.toMap(e -> e.getExternalId(), e -> e.getSerenityUUid()));

        List<EncounterNote> encounters = new ArrayList<>();
        String sql = "select  * from encounter_patient_notes e join patient p on p.id=e.patient_id;";
        SqlRowSet set = legJdbcTemplate.queryForRowSet(sql);
        while (set.next()) {
            PatientData patient = patientDataMap.get(set.getString("mr_number"));
           // Optional<Visits> visit = visitRepository.findByExternalId(set.getString("visit_id"));
         
            EncounterNote encounter = new EncounterNote();
            encounter.setUuid(UUID.randomUUID().toString());
            encounter.setEncounterId(set.getString("encounter_id"));
            encounter.setExternalId(set.getString(5));
            encounter.setCreatedAt(set.getString(2));
            encounter.setEncounterType("ambulatory");
            encounter.setPatientId(patient.getUuid());
            encounter.setNoteType(set.getString("note_type"));
            encounter.setPatientBirthDate(patient.getBirthDate());
            encounter.setPatientFullName(patient.getFullName());
            encounter.setPatientMobile(patient.getMobile());
            encounter.setPatientMrNumber(patient.getMrNumber());
            encounter.setExternalSystem("opd");
            encounter.setNote(set.getString("display"));
            encounter.setLocationId("23f59485-8518-4f4e-9146-d061dfe58175");
           // encounter.setVisitId(set.getString("visit_id"));
            encounter.setServiceProviderId("161380e9-22d3-4627-a97f-0f918ce3e4a9");
            encounter.setServiceProviderName("Nyaho Medical Centre");
            encounters.add(encounter);
          
            }
            logger.info("adding encounter");
        

        ExecutorService executorService = Executors.newFixedThreadPool(10);
        try {
            List<Future<Integer>> futures = executorService.invokeAll(submitNote(encounters, 1000));
            for (Future<Integer> future : futures) {
                System.out.println("future.get = " + future.get());
            }
        } catch (InterruptedException | ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        cleanvisitNOte();

        executorService.shutdown();
        System.err.println("patiend count is ");

    }

    public void cleanvisitNOte(){

        String sql ="""
            update encounternote 
set practitionerid =e.assigned_to_id,visitid=e.visit_id,practitionername=e.assigned_to_name 
from encounter  e
where e.uuid =encounternote.encounterid 
and encounternote.practitionerid is null
and encounternote.externalsystem ='opd' and notetype='visit-note'
            """;
    vectorJdbcTemplate.update(sql);
    }

    public void cleanLegacyData(){
        String sql ="""
                update encounternote 
set practitionerid =v.practitionerid 
from visits  v
where v.externalid =encounternote.visitid 
and encounternote.practitionerid is null
and encounternote.externalsystem ='opd'
                """;
        vectorJdbcTemplate.update(sql);
         sql ="""
                update encounternote 
set visitid =v."uuid" 
from visits  v
where v.externalid =encounternote.visitid 
and encounternote.externalsystem ='opd'
                """;
                vectorJdbcTemplate.update(sql);


    }


public void moveVisitNote(){
saveNotes(encounterNoteRepository.findByNoteType("visit-note"));
    
}

}
