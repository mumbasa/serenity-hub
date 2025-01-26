package com.serenity.integration.service;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

    public int getChiefNote(int offset, Map<String, PatientData> patientDataMap, Map<String, String> doctorMap) {
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
        LIMIT ?, 20000
    """;

    List<EncounterNote> notes = new ArrayList<>();

    hisJdbcTemplate.query(sqlQuery, ps -> ps.setInt(1, offset), rs -> {
        String patientId = rs.getString("patient_mr_number");
        PatientData patientData = patientDataMap.get(patientId);
        if (patientData == null) {
            logger.warn("Patient data not found for ID: {}", patientId);
            return;
        }

        EncounterNote note = createEncounterNote(rs, patientData, doctorMap);
        notes.add(note);

       
    });

   
    if (!notes.isEmpty()) {
        encounterNoteRepository.saveAll(notes);
    }
    
    return 1;
}













public int getChiefNotes(int offset, Map<String, PatientData> patientDataMap, Map<String, String> doctorMap) {
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
    while (rs.next()){
        EncounterNote note = new EncounterNote();
        note.setUuid(UUID.randomUUID().toString());
        note.setEncounterId(UUID.randomUUID().toString());
        note.setCreatedAt(cleanString(rs.getString("created_at")));
        note.setUpdatedAt(cleanString(rs.getString("updated_at")));
        note.setNote((rs.getString("note")));
        note.setNoteType(rs.getString("note_type"));
        note.setEncounterDate(rs.getString("encounter_date"));
        note.setPatientMrNumber(patientDataMap.get(rs.getString("patient_mr_number")).getMrNumber());
        note.setPatientId(patientDataMap.get(rs.getString("patient_mr_number")).getUuid());
        note.setEncounterType(rs.getString("encounter_type"));
        note.setRecalled(rs.getBoolean("is_recalled"));
        note.setPatientGender(patientDataMap.get(rs.getString("patient_mr_number")).getGender());
        note.setPatientBirthDate(patientDataMap.get(rs.getString("patient_mr_number")).getBirthDate());
        note.setPatientFullName(patientDataMap.get(rs.getString("patient_mr_number")).getFullName());
        note.setPatientMobile(patientDataMap.get(rs.getString("patient_mr_number")).getManagingOrganizationId());
        note.setPractitionerName(rs.getString("practitioner_name"));
        note.setHisVisitId(rs.getString("uuid"));
        try{
        note.setPractitionerId(doctorMap.get(rs.getString("practitioner_id")));
        }catch(Exception e){
    
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





private EncounterNote createEncounterNote(ResultSet rs, PatientData patientData, Map<String, String> doctorMap) throws SQLException {
    EncounterNote note = new EncounterNote();
    note.setUuid(UUID.randomUUID().toString());
    note.setEncounterId(UUID.randomUUID().toString());
    note.setCreatedAt(cleanString(rs.getString("created_at")));
    note.setUpdatedAt(cleanString(rs.getString("updated_at")));
    note.setNote((rs.getString("note")));
    note.setNoteType(rs.getString("note_type"));
    note.setEncounterDate(rs.getString("encounter_date").replaceAll("T", " "));
    note.setPatientMrNumber(patientData.getMrNumber());
    note.setEncounterType(rs.getString("encounter_type"));
    note.setRecalled(rs.getBoolean("is_recalled"));
   //s note.setPractitionerRoleType(rs.getString("practitioner_role_type"));
    note.setPractitionerName(rs.getString("practitioner_name"));
    try{
    note.setPractitionerId(doctorMap.get(rs.getString("practitioner_id")));
    }catch(Exception e){

    }
    note.setExternalId(rs.getString("uuid"));
    note.setEdited(false);
    note.setExternalSystem("his");
    return note;
}

    public int getPresentingIllness(int size, Map<String, PatientData> mps, Map<String, String> doc) {
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
                "    ProgressionComplaint <> '' LIMIT ?,1000";

        SqlRowSet set = hisJdbcTemplate.queryForRowSet(query, size);
        while (set.next()) {
            EncounterNote note = new EncounterNote();
            note.setUuid(UUID.randomUUID().toString());
            note.setEncounterId(note.getUuid());
            note.setCreatedAt(set.getString(7));
            note.setUpdatedAt(set.getString(8));
            note.setNote(set.getString(4));
            note.setNoteType(set.getString(9));
            note.setEncounterDate(set.getString(6).replaceAll("T", " "));
            note.setPatientMrNumber(mps.get(set.getString(3)).getMrNumber());
            note.setEncounterType(set.getString(10));
            note.setRecalled(set.getBoolean(12));
            note.setPractitionerRoleType(set.getString(13));
            note.setPractitionerName(set.getString("practitioner_name"));
            note.setPractitionerId(doc.get(set.getString("practitioner_id")));
            note.setExternalId(set.getString("uuid"));
            note.setEdited(set.getBoolean(11));
            note.setExternalSystem("his");
          
            notes.add(note);
        }
        logger.info("add presenting illness");
        encounterNoteRepository.saveAll(notes);

        return 1;

    }

    public int getCarePlan(int size, Map<String, PatientData> mps, Map<String, String> doc) {
        List<EncounterNote> notes = new ArrayList<>();
        List<Encounter> encounters = new ArrayList<>();
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
                
                "  cc.EntryBy = em.Employee_ID LIMIT ?, 1000";

        SqlRowSet set = hisJdbcTemplate.queryForRowSet(query, size);
        while (set.next()) {
            EncounterNote note = new EncounterNote();
            note.setUuid(UUID.randomUUID().toString());
            note.setEncounterId(note.getUuid());
            note.setCreatedAt(set.getString(7));
            note.setUpdatedAt(set.getString(8));
            note.setNote(set.getString(4));
            note.setNoteType(set.getString(9));
            note.setEncounterDate(set.getString(6).replaceAll("T", " "));
            note.setPatientMrNumber(mps.get(set.getString(3)).getMrNumber());
            note.setEncounterType(set.getString(10));
            note.setRecalled(set.getBoolean(12));
            note.setPractitionerRoleType(set.getString(13));
            note.setPractitionerName(set.getString("practitioner_name"));
            note.setPractitionerId(doc.get(set.getString("practitioner_id")));
            note.setExternalId(set.getString("uuid"));
            note.setEdited(set.getBoolean(11));
            note.setExternalSystem("his");
         
            
            notes.add(note);
        }
        encounterRepository.saveAll(encounters);

        return 1;

    }

    public int getProgressNote(int size, Map<String, PatientData> mps, Map<String, String> doc) {
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
                "ON `source`.`patient_mr_number` = `pm`.`Patient_ID` LIMIT ?, 1000";

        SqlRowSet set = hisJdbcTemplate.queryForRowSet(sqlQuery,size);
        while (set.next()) {
            EncounterNote note = new EncounterNote();
            note.setCreatedAt(set.getString(1));
            note.setUpdatedAt(set.getString(2));
            note.setNote(set.getString(3));
            note.setNoteType(set.getString(4));
            note.setEncounterDate(set.getString(5));
            note.setPatientMrNumber(mps.get(set.getString(6)).getMrNumber());
            note.setEncounterType(set.getString(7));
            note.setPractitionerRoleType(set.getString(9).replaceAll("\u0000", ""));
            note.setPractitionerName(set.getString(10).replaceAll("\u0000", ""));
            note.setPractitionerId(doc.get(set.getString(11)));
            note.setEdited(set.getBoolean(12));
            note.setExternalId(set.getString(14));
            note.setUuid(UUID.randomUUID().toString());
            note.setEncounterId(note.getUuid());
            note.setExternalSystem("his");

                  notes.add(note);
        }
        encounterNoteRepository.saveAll(notes);

        return 1;
    }

    public void chiefThreads() {

        int rows = 590989;
        Map<String, PatientData> mps = patientRepository.findAll().stream()
                .collect(Collectors.toMap(e -> e.getExternalId(), e -> e));
        Map<String, String> doc = doctorRepository.findHisPractitioners().stream()
                .collect(Collectors.toMap(e -> e.getExternalId(), e -> e.getSerenityUUid()));

        ExecutorService executorService = Executors.newFixedThreadPool(10);
        try {
            List<Future<Integer>> futures = executorService.invokeAll(submitTask2(rows, 20000, mps, doc));
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

    public void ilnessThreads() {

        int rows = 489791;
        Map<String, PatientData> mps = patientRepository.findAll().stream()
                .collect(Collectors.toMap(e -> e.getExternalId(), e -> e));
        Map<String, String> doc = doctorRepository.findHisPractitioners().stream()
                .collect(Collectors.toMap(e -> e.getExternalId(), e -> e.getSerenityUUid()));

        ExecutorService executorService = Executors.newFixedThreadPool(10);
        try {
            List<Future<Integer>> futures = executorService.invokeAll(submitIllnessTask(rows, 1000, mps, doc));
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

    public void careThreads() {

        int rows = 324564;
        Map<String, PatientData> mps = patientRepository.findAll().stream()
                .collect(Collectors.toMap(e -> e.getExternalId(), e -> e));
        Map<String, String> doc = doctorRepository.findHisPractitioners().stream()
                .collect(Collectors.toMap(e -> e.getExternalId(), e -> e.getSerenityUUid()));

        ExecutorService executorService = Executors.newFixedThreadPool(10);
        try {
            List<Future<Integer>> futures = executorService.invokeAll(submitCareTask(rows, 1000, mps, doc));
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

    public void progressThreads() {

        int rows = 389461;
        Map<String, PatientData> mps = patientRepository.findAll().stream()
                .collect(Collectors.toMap(e -> e.getExternalId(), e -> e));
        Map<String, String> doc = doctorRepository.findHisPractitioners().stream()
                .collect(Collectors.toMap(e -> e.getExternalId(), e -> e.getSerenityUUid()));

        ExecutorService executorService = Executors.newFixedThreadPool(10);
        try {
            List<Future<Integer>> futures = executorService.invokeAll(submitProgressTask(rows, 1000, mps, doc));
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

    public Set<Callable<Integer>> submitTask2(int visits, int batchSize, Map<String, PatientData> mps,
            Map<String, String> doc) {

        Set<Callable<Integer>> callables = new HashSet<>();
        int totalSize = visits;
        int batches = (totalSize + batchSize - 1) / batchSize; // Ceiling division

        for (int i = 0; i < batches; i++) {
            final int batchNumber = i; // For use in lambda

            callables.add(() -> {
                int startIndex = batchNumber * batchSize;

                logger.debug("Processing batch {}/{}, indices [{}]",
                        batchNumber + 1, batches, startIndex);

                return getChiefNote(startIndex, mps, doc);
            });
        }

        return callables;
    }



    public Set<Callable<Integer>> submitIllnessTask(int visits, int batchSize, Map<String, PatientData> mps,
    Map<String, String> doc) {

Set<Callable<Integer>> callables = new HashSet<>();
int totalSize = visits;
int batches = (totalSize + batchSize - 1) / batchSize; // Ceiling division

for (int i = 0; i < batches; i++) {
    final int batchNumber = i; // For use in lambda

    callables.add(() -> {
        int startIndex = batchNumber * batchSize;

        logger.debug("Processing batch {}/{}, indices [{}]",
                batchNumber + 1, batches, startIndex);

        return getPresentingIllness(startIndex, mps, doc);
    });
}

return callables;
}


public Set<Callable<Integer>> submitCareTask(int visits, int batchSize, Map<String, PatientData> mps,
Map<String, String> doc) {

Set<Callable<Integer>> callables = new HashSet<>();
int totalSize = visits;
int batches = (totalSize + batchSize - 1) / batchSize; // Ceiling division

for (int i = 0; i < batches; i++) {
final int batchNumber = i; // For use in lambda

callables.add(() -> {
    int startIndex = batchNumber * batchSize;

    logger.debug("Processing batch {}/{}, indices [{}]",
            batchNumber + 1, batches, startIndex);

    return getCarePlan(startIndex, mps, doc);
});
}

return callables;
}


public Set<Callable<Integer>> submitProgressTask(int visits, int batchSize, Map<String, PatientData> mps,
Map<String, String> doc) {

Set<Callable<Integer>> callables = new HashSet<>();
int totalSize = visits;
int batches = (totalSize + batchSize - 1) / batchSize; // Ceiling division

for (int i = 0; i < batches; i++) {
final int batchNumber = i; // For use in lambda

callables.add(() -> {
    int startIndex = batchNumber * batchSize;

    logger.debug("Processing batch {}/{}, indices [{}]",
            batchNumber + 1, batches, startIndex);

    return getProgressNote(startIndex, mps, doc);
});
}

return callables;
}



public void saveNotes(List<EncounterNote> notes){

String sql ="INSERT INTO public.encounter_notes\n" + //
        "(created_at, updated_at, pk, encounter_id, patient_id, visit_id, \"uuid\", note, is_formatted, note_type, is_edited, is_recalled, practitioner_id, encounter_date, practitioner_name, practitioner_role_type, encounter_type, patient_mr_number, edit_history)\n" + //
        "VALUES(?, ?, nextval('encounter_notes_pk_seq'::regclass), uuid(?), uuid(?), ?, ?, '', false, '', false, false, ?, '', '', '', '', '', '');";

serenityJdbcTemplate.batchUpdate(sql,new  BatchPreparedStatementSetter() {

    @Override
    public void setValues(PreparedStatement ps, int i) throws SQLException {
        // TODO Auto-generated method stub
        EncounterNote note = new EncounterNote();
        ps.setString(1,note.getCreatedAt());
        ps.setString(2,note.getCreatedAt());
        ps.setString(3, note.getEncounterId());
        ps.setString(4,note.getPatientId());
        ps.setString(5, note.getVisitId());
        ps.setString(6, note.getUuid());
        ps.setString(7,note.getNote());
        ps.setBoolean(8,false);
        ps.setString(9,note.getNoteType());
        ps.setBoolean(10,false);
        ps.setBoolean(11, false);
        ps.setString(12, note.getPractitionerId());
        ps.setString(13, note.getEncounterDate());
    

    }

    @Override
    public int getBatchSize() {
        // TODO Auto-generated method stub
      return notes.size();
    }
    
});

}
public String cleanString(String input) {
    if (input == null) return null;
    return input.replace("\0", "")
                .replace("\u0000", "");
}

}

