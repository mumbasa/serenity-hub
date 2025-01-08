package com.serenity.integration.service;

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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Service;

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
            note.setDataSource("his");
            notes.add(note);
        }

        encounterNoteRepository.saveAllAndFlush(notes);
        return notes;

    }

    public int getChiefNote(int size, Map<String, PatientData> mps, Map<String, String> doc) {

        List<EncounterNote> notes = new ArrayList<>();
        List<Encounter> encounters = new ArrayList<>();

        String sqlQuery = "SELECT " +
                "  Transaction_ID AS \"uuid\", " +
                "  Transaction_ID AS \"encounter_id\", " +
                "  PatientID AS \"patient_mr_number\", " +
                "  MainComplaint AS \"note\", " +
                "  EntryBy AS \"practitioner_id\", " +
                "  DATE_FORMAT(EntryDate, '%Y-%m-%dT%TZ') AS \"encounter_date\", " +
                "  NULL AS \"created_at\", " +
                "  NULL AS \"updated_at\", " +
                "  'chief-complaint' AS \"note-type\", " +
                "  'outpatient-consultation' AS \"encounter_type\", " +
                "  FALSE AS is_edited, " +
                "  FALSE AS is_recalled, " +
                "  'unknown' AS practitioner_role_type, " +
                "  CONCAT(practitioners.title, ' ', practitioners.Name) AS \"practitioner_name\", " +
                "  NULL AS \"edit_history\" " +
                "FROM " +
                "  cpoe_hpexam " +
                "  LEFT JOIN employee_master AS practitioners ON cpoe_hpexam.EntryBy = practitioners.Employee_ID LIMIT ?, 1000";

        SqlRowSet set = hisJdbcTemplate.queryForRowSet(sqlQuery, size);
        while (set.next()) {
            EncounterNote note = new EncounterNote();
            note.setUuid(UUID.randomUUID().toString());
            note.setEncounterId(UUID.randomUUID().toString());
            note.setEncounterId(set.getString(2));
            note.setCreatedAt(set.getString(7));
            note.setUpdatedAt(set.getString(8));
            note.setNote(set.getString(4));
            note.setNoteType(set.getString(9));
            note.setEncounterDate(set.getString(6));
            note.setPatientMrNumber(mps.get(set.getString(3)).getMrNumber());
            note.setEncounterType(set.getString(10));
            note.setRecalled(set.getBoolean(12));
            note.setPractitionerRoleType(set.getString(13));
            note.setPractitionerName(set.getString(14));
            note.setPractitionerId(set.getString(5));

            note.setEdited(set.getBoolean(11));
            note.setDataSource("his");
            Visits visits = visitRepository.getVistByDateDoctorPatient(note.getEncounterDate(),
                    doc.get(set.getString(5)), set.getString(3));
            try {
                Encounter encounter = new Encounter(note, visits, mps.get(set.getString(3)));
                encounters.add(encounter);
            } catch (Exception e) {
                Encounter encounter = new Encounter(note, mps.get(set.getString(3)));
                encounters.add(encounter);

            }
            notes.add(note);
        }
encounterRepository.saveAll(encounters);
encounterNoteRepository.saveAll(notes);

return 1;
    }

    public void getPresentingIllness() {

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
                "    ProgressionComplaint <> '';";

        SqlRowSet set = hisJdbcTemplate.queryForRowSet(query);
        while (set.next()) {
            EncounterNote note = new EncounterNote();
            note.setUuid(set.getString(1));
            note.setEncounterId(set.getString(2));
            note.setCreatedAt(set.getString(7));
            note.setUpdatedAt(set.getString(8));
            note.setNote(set.getString(4));
            note.setNoteType(set.getString(9));
            note.setEncounterDate(set.getString(6));
            note.setPatientMrNumber(set.getString(3));
            note.setEncounterType(set.getString(10));
            note.setRecalled(set.getBoolean(12));
            note.setPractitionerRoleType(set.getString(13));
            note.setPractitionerName(set.getString(14));
            note.setPractitionerId(set.getString(5));
            note.setEdited(set.getBoolean(11));
            note.setDataSource("his");
            notes.add(note);
        }
        int rounds = (notes.size() / 1000);
        int cycle = 0;
        for (int i = 0; i < rounds; i++) {
            logger.info("adding illness care  " + i);
            try {
                if (cycle < rounds) {
                    encounterNoteRepository.saveAllAndFlush(notes.subList(i * 1000, (i * 1000) + 1000));
                } else {
                    encounterNoteRepository.saveAllAndFlush(notes.subList(cycle * 1000, notes.size()));

                }

                cycle++;
            } catch (Exception e) {

            }

        }

    }

    public void getCarePlan() {

        List<EncounterNote> notes = new ArrayList<>();
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
                "  cc.EntryBy = em.Employee_ID;";

        SqlRowSet set = hisJdbcTemplate.queryForRowSet(query);
        while (set.next()) {
            EncounterNote note = new EncounterNote();
            note.setUuid(set.getString(1));
            note.setEncounterId(set.getString(2));
            note.setCreatedAt(set.getString(7));
            note.setUpdatedAt(set.getString(8));
            note.setNote(set.getString(4));
            note.setNoteType(set.getString(9));
            note.setEncounterDate(set.getString(6));
            note.setPatientMrNumber(set.getString(3));
            note.setEncounterType(set.getString(10));
            // note.setRecalled(set.getBoolean(12));
            note.setPractitionerRoleType(set.getString(13));
            note.setPractitionerName(set.getString(14));
            note.setPractitionerId(set.getString(5));
            note.setEdited(set.getBoolean(11));
            note.setDataSource("his");
            notes.add(note);
        }
        int rounds = (notes.size() / 1000);
        int cycle = 0;
        for (int i = 0; i <= rounds; i++) {
            logger.info("adding round care plan " + i);
            try {
                if (cycle < rounds) {
                    encounterNoteRepository.saveAllAndFlush(notes.subList(i * 1000, (i * 1000) + 1000));
                } else {
                    encounterNoteRepository.saveAllAndFlush(notes.subList(cycle * 1000, notes.size()));

                }

                cycle++;
            } catch (Exception e) {

            }

        }

    }

    public void getProgressNote() {
        Map<String, String> patient = new HashMap<String, String>();
        Map<String, String> practitioners = new HashMap<String, String>();
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
                "ON `source`.`patient_mr_number` = `pm`.`Patient_ID`;";

        // "WHERE source.patient_mr_number ='"+queryDetails +"'";
        SqlRowSet set = hisJdbcTemplate.queryForRowSet(sqlQuery);
        while (set.next()) {
            EncounterNote note = new EncounterNote();
            note.setCreatedAt(set.getString(1));
            note.setUpdatedAt(set.getString(2));
            note.setNote(set.getString(3));
            note.setNoteType(set.getString(4));
            note.setEncounterDate(set.getString(5));
            note.setPatientMrNumber(set.getString(6));
            note.setEncounterType(set.getString(7));

            note.setPractitionerRoleType(set.getString(9).replaceAll("\u0000", ""));
            note.setPractitionerName(set.getString(10).replaceAll("\u0000", ""));
            note.setPractitionerId(set.getString(11));
            note.setEdited(set.getBoolean(12));
            note.setUuid(set.getString(14));
            note.setDataSource("his");
            notes.add(note);
        }
        int rounds = (notes.size() / 1000);
        int cycle = 0;
        for (int i = 0; i <= rounds; i++) {
            logger.info("adding round progress " + i);
            try {
                if (cycle < rounds) {
                    encounterNoteRepository.saveAllAndFlush(notes.subList(i * 1000, (i * 1000) + 1000));
                } else {
                    encounterNoteRepository.saveAllAndFlush(notes.subList(cycle * 1000, notes.size()));

                }

                cycle++;
            } catch (Exception e) {

            }

        }

    }




public void chiefThreads(){

int rows =5909;//89;
    Map<String,PatientData> mps = patientRepository.findAll().stream().collect(Collectors.toMap(e -> e.getExternalId(), e -> e));
        Map<String,String> doc = doctorRepository.findHisPractitioners().stream().collect(Collectors.toMap(e -> e.getExternalId(), e -> e.getSerenityUUid()));

    ExecutorService executorService =  Executors.newFixedThreadPool(10);
        try {
            List<Future<Integer>> futures = executorService.invokeAll(submitTask2(rows,1000,mps,doc));
            for(Future<Integer> future : futures){
                System.out.println("future.get = " + future.get());
            }
        } catch (InterruptedException | ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        executorService.shutdown();
        System.err.println("patiend count is ");
        
        
            
        }
    
    


public Set<Callable<Integer>> submitTask2(int visits, int batchSize, Map<String,PatientData> mps,Map<String,String> doc) {
    
        Set<Callable<Integer>> callables = new HashSet<>();
        int totalSize = visits;
        int batches = (totalSize + batchSize - 1) / batchSize;  // Ceiling division
    
        for (int i = 0; i < batches; i++) {
            final int batchNumber = i;  // For use in lambda
            
            callables.add(() -> {
                int startIndex = batchNumber * batchSize;
                
                logger.debug("Processing batch {}/{}, indices [{}]", 
                         batchNumber + 1, batches, startIndex);
                
                return getChiefNote(startIndex, mps,doc);
            });
        }
        
        return callables;
    }

}