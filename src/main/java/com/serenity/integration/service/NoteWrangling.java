package com.serenity.integration.service;

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
public class NoteWrangling {
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
            note.setExternalSystem("his");
            notes.add(note);
        }

        encounterNoteRepository.saveAllAndFlush(notes);
        return notes;

    }

    public  List<EncounterNote> getChiefNote( Map<String, PatientData> mps, Map<String, String> doc) {
    Map<String,String> visits = new HashMap<>();

     String query = """
         SELECT 
            Transaction_ID AS uuid,
            Transaction_ID AS encounter_id,
            PatientID AS patient_mr_number,
            MainComplaint AS note,
            EntryBy AS practitioner_id,
            DATE_FORMAT(EntryDate, '%Y-%m-%dT%TZ') AS encounter_date,
            NULL AS created_at,
            NULL AS updated_at,
            'chief-complaint' AS note_type,
            'outpatient-consultation' AS encounter_type,
            FALSE AS is_edited,
            FALSE AS is_recalled,
            'unknown' AS practitioner_role_type,
            CONCAT(practitioners.title, ' ', practitioners.Name) AS practitioner_name
        FROM cpoe_hpexam 
        LEFT JOIN employee_master AS practitioners ON cpoe_hpexam.EntryBy = practitioners.Employee_ID 
        WHERE MainComplaint is not null
    """;

    List<EncounterNote> notes = new ArrayList<>();

    SqlRowSet set = hisJdbcTemplate.queryForRowSet(query);
        while (set.next()) {
            EncounterNote note = new EncounterNote();
            note.setUuid(UUID.randomUUID().toString());
            note.setEncounterId(UUID.randomUUID().toString());
            note.setNote(set.getString(4).replace("\0", ""));
            note.setNoteType(set.getString(9).replace("\0", ""));
            String date=set.getString(6);
            try{
            date=date.replace("\0", "");
            note.setEncounterDate(date==null? "0000-00-00":date.replaceAll("T", " "));
        }catch(Exception e){
            note.setEncounterDate("0000-00-00");
        }

            note.setPatientMrNumber(mps.get(set.getString(3)).getMrNumber());
            note.setPatientGender(mps.get(set.getString(3)).getGender());
            note.setPatientMobile(mps.get(set.getString(3)).getMobile());
            note.setPatientBirthDate(mps.get(set.getString(3)).getBirthDate());
            note.setEncounterType(set.getString(10).replace("\0", ""));
            note.setRecalled(set.getBoolean(12));
            note.setPractitionerRoleType("doctor");
            String practitionerName =set.getString("practitioner_name");
            if(practitionerName!=null){
            note.setPractitionerName(set.getString("practitioner_name").replace("\0", ""));
            note.setPractitionerId(doc.get(set.getString("practitioner_id")));
            }else{
                note.setPractitionerName("unknwon");
 
            }

          
            note.setPatientFullName(mps.get(set.getString(3)).getFullName());
            note.setPatientId(mps.get(set.getString(3)).getUuid());
            String key = note.getEncounterDate().split(" ")[0]+"="+set.getString(3);
            if(visits.containsKey(key)){
                note.setVisitId(visits.get(key));
            }else{
                String vid = UUID.randomUUID().toString();
                note.setVisitId(vid);
                visits.put(key, vid);
            }
            note.setExternalId(set.getString("uuid"));
            note.setEdited(set.getBoolean(11));
            note.setExternalSystem("his");
          
          
            notes.add(note);



        }
    

    
     return notes;
}

    public List<EncounterNote> getPresentingIllness(Map<String, PatientData> mps, Map<String, String> doc) {
        Map<String,String> visits = new HashMap<>();
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
                "    ProgressionComplaint <> '' OR ProgressionComplaint IS NOT NULL";

                List<EncounterNote> notes = new ArrayList<>();

                SqlRowSet set = hisJdbcTemplate.queryForRowSet(query);
                    while (set.next()) {
                        EncounterNote note = new EncounterNote();
                        note.setUuid(UUID.randomUUID().toString());
                        note.setEncounterId(UUID.randomUUID().toString());
                        note.setNote(set.getString(4).replace("\0", ""));
                        note.setNoteType(set.getString(9).replace("\0", ""));
                        String date=set.getString(6);
                        try{
                        date=date.replace("\0", "");
                        note.setEncounterDate(date==null? "0000-00-00":date.replaceAll("T", " "));
                    }catch(Exception e){
                        note.setEncounterDate("0000-00-00");
                    }
            
                        note.setPatientMrNumber(mps.get(set.getString(3)).getMrNumber());
                        note.setPatientGender(mps.get(set.getString(3)).getGender());
                        note.setPatientMobile(mps.get(set.getString(3)).getMobile());
                        note.setPatientBirthDate(mps.get(set.getString(3)).getBirthDate());
                        note.setEncounterType(set.getString(10).replace("\0", ""));
                        note.setRecalled(set.getBoolean(12));
                        note.setPractitionerRoleType("doctor");
                        String practitionerName =set.getString("practitioner_name");
                        if(practitionerName!=null){
                        note.setPractitionerName(set.getString("practitioner_name").replace("\0", ""));
                        note.setPractitionerId(doc.get(set.getString("practitioner_id")));
                        }else{
                            note.setPractitionerName("unknwon");
             
                        }
            
                      
                        note.setPatientFullName(mps.get(set.getString(3)).getFullName());
                        note.setPatientId(mps.get(set.getString(3)).getUuid());
                        String key = note.getEncounterDate().split(" ")[0]+"="+set.getString(3);
                        if(visits.containsKey(key)){
                            note.setVisitId(visits.get(key));
                        }else{
                            String vid = UUID.randomUUID().toString();
                            note.setVisitId(vid);
                            visits.put(key, vid);
                        }
                        note.setExternalId(set.getString("uuid"));
                        note.setEdited(set.getBoolean(11));
                        note.setExternalSystem("his");
                      
                      
                        notes.add(note);
            
                    }
        return notes;

    }

    public List<EncounterNote> getCarePlan(Map<String, PatientData> mps, Map<String, String> doc) {
        List<EncounterNote> notes = new ArrayList<>();
        Map<String,String> visits = new HashMap<>();
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
                        note.setEncounterId(UUID.randomUUID().toString());
                        note.setNote(set.getString(4).replace("\0", ""));
                        note.setNoteType(set.getString(9).replace("\0", ""));
                        String date=set.getString(6);
                        try{
                        date=date.replace("\0", "");
                        note.setEncounterDate(date==null? "0000-00-00":date.replaceAll("T", " "));
                    }catch(Exception e){
                        note.setEncounterDate("0000-00-00");
                    }
            
                        note.setPatientMrNumber(mps.get(set.getString(3)).getMrNumber());
                        note.setPatientGender(mps.get(set.getString(3)).getGender());
                        note.setPatientMobile(mps.get(set.getString(3)).getMobile());
                        note.setPatientBirthDate(mps.get(set.getString(3)).getBirthDate());
                        note.setPatientFullName(mps.get(set.getString(3)).getFullName());
                        note.setEncounterType(set.getString(10).replace("\0", ""));
                        note.setRecalled(set.getBoolean(12));
                        note.setPractitionerRoleType("doctor");
                        String practitionerName =set.getString("practitioner_name");
                        if(practitionerName!=null){
                        note.setPractitionerName(set.getString("practitioner_name").replace("\0", ""));
                        note.setPractitionerId(doc.get(set.getString("practitioner_id")));
                        }else{
                            note.setPractitionerName("unknwon");
             
                        }
            
                      
                        note.setPatientId(mps.get(set.getString(3)).getUuid());
                        String key = note.getEncounterDate().split(" ")[0]+"="+set.getString(3);
                        if(visits.containsKey(key)){
                            note.setVisitId(visits.get(key));
                        }else{
                            String vid = UUID.randomUUID().toString();
                            note.setVisitId(vid);
                            visits.put(key, vid);
                        }
                        note.setExternalId(set.getString("uuid"));
                        note.setEdited(set.getBoolean(11));
                        note.setExternalSystem("his");
                      
                      
                        notes.add(note);
            
                    }
        return notes;

    }

    public List<EncounterNote>  getProgressNote(Map<String, PatientData> mps, Map<String, String> doc) {
        List<EncounterNote> notes = new ArrayList<>();
        Map<String,String> visits = new HashMap<>();
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

        SqlRowSet set = hisJdbcTemplate.queryForRowSet(sqlQuery);
        while (set.next()) {
            EncounterNote note = new EncounterNote();
           note.setCreatedAt(cleanString(set.getString(1)));
            note.setUpdatedAt(cleanString(set.getString(2)));
            note.setNote(cleanString(set.getString(3)));
            note.setNoteType(cleanString(set.getString(4)));
            note.setEncounterDate(cleanString(set.getString(5)));
            try{
            note.setPatientMrNumber(mps.get(set.getString(6)).getMrNumber());
            note.setPatientGender(mps.get(set.getString(6)).getGender());
            note.setPatientMobile(mps.get(set.getString(6)).getMobile());
            note.setPatientBirthDate(mps.get(set.getString(6)).getBirthDate());
            note.setPatientFullName(mps.get(set.getString(6)).getFullName());
            note.setPatientId(mps.get(set.getString(6)).getUuid());

            }catch (Exception e){

                System.err.println("no patient");

            }
            note.setEncounterType(set.getString(7));
            note.setPractitionerRoleType("doctor");
            String pName =cleanString(set.getString(10));
            try{
            note.setPractitionerName(pName);
            note.setPractitionerId(doc.get(set.getString(11)));
              }catch (Exception e){

              }
            note.setEdited(set.getBoolean(12));
            note.setExternalId(set.getString(14));
            note.setUuid(UUID.randomUUID().toString());
            note.setEncounterId(UUID.randomUUID().toString());
            note.setExternalSystem("his");
            
            String key = note.getEncounterDate().split(" ")[0]+"="+set.getString(3);
            if(visits.containsKey(key)){
                note.setVisitId(visits.get(key));
            }else{
                String vid = UUID.randomUUID().toString();
                note.setVisitId(vid);
                visits.put(key, vid);
            }
          

            notes.add(note);


















            
        }

        return notes;
    }

    public void chiefThreads() {

    
        Map<String, PatientData> mps = patientRepository.findAll().stream()
                .collect(Collectors.toMap(e -> e.getExternalId(), e -> e));
        Map<String, String> doc = doctorRepository.findHisPractitioners().stream()
                .collect(Collectors.toMap(e -> e.getExternalId(), e -> e.getSerenityUUid()));
        List<EncounterNote> notes = getChiefNote(mps, doc);

        ExecutorService executorService = Executors.newFixedThreadPool(10);
        try {
            List<Future<Integer>> futures = executorService.invokeAll(submitTask2( 1000,notes));
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

  
    
   
    public void illThreads() {
    
        Map<String, PatientData> mps = patientRepository.findAll().stream()
                .collect(Collectors.toMap(e -> e.getExternalId(), e -> e));
        Map<String, String> doc = doctorRepository.findHisPractitioners().stream()
                .collect(Collectors.toMap(e -> e.getExternalId(), e -> e.getSerenityUUid()));
        List<EncounterNote> notes = getPresentingIllness(mps, doc);

        ExecutorService executorService = Executors.newFixedThreadPool(10);
        try {
            List<Future<Integer>> futures = executorService.invokeAll(submitTask2( 1000,notes));
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
    
        Map<String, PatientData> mps = patientRepository.findAll().stream()
                .collect(Collectors.toMap(e -> e.getExternalId(), e -> e));
        Map<String, String> doc = doctorRepository.findHisPractitioners().stream()
                .collect(Collectors.toMap(e -> e.getExternalId(), e -> e.getSerenityUUid()));
        List<EncounterNote> notes = getCarePlan(mps, doc);

        ExecutorService executorService = Executors.newFixedThreadPool(10);
        try {
            List<Future<Integer>> futures = executorService.invokeAll(submitTask2( 1000,notes));
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
    
        Map<String, PatientData> mps = patientRepository.findAll().stream()
                .collect(Collectors.toMap(e -> e.getExternalId(), e -> e));
        Map<String, String> doc = doctorRepository.findHisPractitioners().stream()
                .collect(Collectors.toMap(e -> e.getExternalId(), e -> e.getSerenityUUid()));
        List<EncounterNote> notes = getProgressNote(mps, doc);

        ExecutorService executorService = Executors.newFixedThreadPool(10);
        try {
            List<Future<Integer>> futures = executorService.invokeAll(submitTask2( 1000,notes));
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

   
    public void encounterThread(  List<EncounterNote> notes) {
    
        List<Encounter> encounters = new ArrayList<>();

       notes.stream().forEach(e -> encounters.add(new Encounter(e)));

        ExecutorService executorService = Executors.newFixedThreadPool(10);
        try {
            List<Future<Integer>> futures = executorService.invokeAll(submitEncounters( 100,encounters));
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



    public void visitsThread() {
    
        List<Visits> visit = new ArrayList<>();

       encounterNoteRepository.findAll().stream().forEach(e -> visit.add(new Visits(e)));
       List<Visits> visits = visit.stream().distinct().toList();
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        try {
            List<Future<Integer>> futures = executorService.invokeAll(submitVisits( 1000,visits));
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

    public Set<Callable<Integer>> submitTask2(int batchSize, List<EncounterNote> notes) {
    
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
                try{
                encounterNoteRepository.saveAll(notes.subList(startIndex, endIndex));
                }
                catch (Exception e) {
                    // TODO: handle exception
                    e.printStackTrace();
                    logger.info("error adding note");
                    for(EncounterNote note : notes){
                        try{
                        encounterNoteRepository.save(note);
                        }catch(Exception es){
                            System.err.println("failed add some");
                            es.printStackTrace();

                        }
                    }
                }

               
                return 1;
            });
        }

        return callables;
    }


    public Set<Callable<Integer>> submitEncounters(int batchSize, List<Encounter> encounters) {
    
        Set<Callable<Integer>> callables = new HashSet<>();
        int totalSize = encounters.size();
        int batches = (totalSize + batchSize - 1) / batchSize; // Ceiling division

        for (int i = 0; i < batches; i++) {
            final int batchNumber = i; // For use in lambda

            callables.add(() -> {
                int startIndex = batchNumber * batchSize;
                int endIndex = Math.min(startIndex + batchSize, totalSize);
                logger.debug("Processing batch {}/{}, indices [{}]",
                        batchNumber + 1, batches, startIndex);
                try{
                encounterRepository.saveAll(encounters.subList(startIndex, endIndex));
                }
                catch (Exception e) {
                    // TODO: handle exception
                    e.printStackTrace();
                    logger.info("error adding note");
                    for(Encounter note : encounters){
                        try{
                        encounterRepository.save(note);
                        }catch(Exception es){
                            System.err.println("failed add some");
                            es.printStackTrace();

                        }
                    }
                }

               
                return 1;
            });
        }

        return callables;
    }


public void setEncounterThreads(){
int data = 1883637;
int rounds = (int)Math.ceil(data/10000);
for(int i=0;i<rounds;i++){
logger.info("setting offset");
List<EncounterNote> notes = encounterNoteRepository.findOffsetData(i*10000, 10000);
List<Encounter> encounters = new ArrayList<>();
notes.stream().forEach(e -> encounters.add(new Encounter(e)));
encounterRepository.saveAll(encounters);

}






}


public void setVisitThreads(){
    int data = 1883637;
    int rounds = (int)Math.ceil(data/10000);
    for(int i=0;i<rounds;i++){
    logger.info("setting visit offset");
    List<EncounterNote> notes = encounterNoteRepository.findOffsetData(i*10000, 10000);
    Set<Visits> encounters = new HashSet<>();
    notes.stream().forEach(e -> encounters.add(new Visits(e)));
    visitRepository.saveAll(encounters);
    
    }



}

    public Set<Callable<Integer>> submitVisits(int batchSize, List<Visits> encounters) {
    
        Set<Callable<Integer>> callables = new HashSet<>();
        int totalSize = encounters.size();
        int batches = (totalSize + batchSize - 1) / batchSize; // Ceiling division

        for (int i = 0; i < batches; i++) {
            final int batchNumber = i; // For use in lambda

            callables.add(() -> {
                int startIndex = batchNumber * batchSize;
                int endIndex = Math.min(startIndex + batchSize, totalSize);
                logger.debug("Processing batch {}/{}, indices [{}]",
                        batchNumber + 1, batches, startIndex);
                try{
                visitRepository.saveAll(encounters.subList(startIndex, endIndex));
                }
                catch (Exception e) {
                    // TODO: handle exception
                    e.printStackTrace();
                    logger.info("error adding note");
                    for(Visits note : encounters){
                        try{
                        visitRepository.save(note);
                        }catch(Exception es){
                            System.err.println("failed add some");
                            es.printStackTrace();

                        }
                    }
                }

               
                return 1;
            });
        }

        return callables;
    }

    public String cleanString(String input) {
        if (input == null) return null;
        return input.replace("\0", "")
                    .replace("\u0000", "");
    }
    

}