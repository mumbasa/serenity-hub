package com.serenity.integration.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.repository.CrudRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Service;

import com.serenity.integration.models.Doctors;
import com.serenity.integration.models.Encounter;
import com.serenity.integration.models.EncounterNote;
import com.serenity.integration.models.MedicalRequest;
import com.serenity.integration.models.PatientData;
import com.serenity.integration.models.Visits;
import com.serenity.integration.repository.DoctorRepository;
import com.serenity.integration.repository.EncounterNoteRepository;
import com.serenity.integration.repository.EncounterRepository;
import com.serenity.integration.repository.MedicalRequestRepository;
import com.serenity.integration.repository.PatientRepository;
import com.serenity.integration.repository.VisitRepository;

@Service
public class MedicalRequestService {
    @Autowired
    @Qualifier(value = "hisJdbcTemplate")
    JdbcTemplate hisJdbcTemplate;

    @Autowired
    EncounterNoteRepository encounterNoteRepository;
    static Logger logger = LoggerFactory.getLogger("Medical Request Service");

    @Autowired
    VisitRepository visitRepository;

    @Autowired
    PatientRepository patientRepository;

    @Autowired
    DoctorRepository doctorRepository;

    @Autowired
    EncounterRepository encounterRepository;

    @Autowired
    MedicalRequestRepository medicalRequestRepository;

    public  List<MedicalRequest> medicalRequestOPD2() {
        Map<String, PatientData> mps = patientRepository.findAll().stream()
        .collect(Collectors.toMap(e -> e.getExternalId(), e -> e));
Map<String, String> doc = doctorRepository.findHisPractitioners().stream()
        .collect(Collectors.toMap(e -> e.getExternalId(), e -> e.getSerenityUUid()));

    
        int totalSize = 1179689;
        int batches = (totalSize + 10000 - 1) / 10000; // Ceiling division

        for (int i = 0; i < batches; i++) {
            int startIndex = i * 10000;
                int endIndex = Math.min(startIndex + 10000, totalSize);

        String query = """
                                        select

                	pm.PatientMedicine_ID uuid,

                	pm.EntryDate created_at,

                	pm.EntryDate updated_at,

                	pm.EntryDate authored_on,

                	IFNULL(im.TypeName, pm.MedicineName) name,

                	"outpatient" category,

                	pm.Medicine_ID code,

                	null date,

                	pm.Remarks notes,

                	null intended_dispenser,

                	"routine" priority,

                	"completed" status,

                	concat(pm.dose, " - ", pm.NoTimesDay, " - ", pm.NoOfDays) dosage_display,

                	null dosage_form,

                	null dosage_route,

                	null dosage_site,

                	null dosage_frequency,

                	null dosage_frequency_unit,
                	null dose,

                	null dose_unit,

                	null dosage_strength,

                	null dosage_period,

                	null course_of_therapy,

                	null quantity_to_dispense,

                	null number_of_refills,

                	null dosage_period_unit,

                	"Nyaho Medical Centre" service_provider_id,

                	null encounter_id,

                	pm.Transaction_ID visit_id,

                	pm.Patient_ID patient_id,

                	pm.Patient_ID mr_number,

                	patient_master.PName patient_full_name,

                	CONCAT(dm.Title, ' ', dm.Name) practitioner_name,

                	pm.DoctorID practitioner_id

                from

                	patient_medicine pm

                join doctor_master dm on dm.Doctor_ID = pm.DoctorID

                join patient_master on patient_master.Patient_ID = pm.Patient_ID

                left join f_itemmaster im on

                	pm.Medicine_ID = im.ItemID

                where

                	pm.IsChange = 0

                	and pm.isReject = 0

                    LIMIT ?,10000
             
                                    """;

        List<MedicalRequest> requests = new ArrayList<>();
        System.err.println(" statring the rowset");
        SqlRowSet set = hisJdbcTemplate.queryForRowSet(query,startIndex);
        while (set.next()) {
            String patientMr = set.getString("patient_id");
            String date = set.getString("created_at");
            String doctor = set.getString("practitioner_id");
            String externalId=set.getString("visit_id");
           // List<Encounter> ecounter = encounterRepository.findByExternalIdAndAssignedToId(externalId, doc.get(doctor));
            
                MedicalRequest request = new MedicalRequest();
                request.setUuid(UUID.randomUUID().toString());
                request.setCreatedAt(set.getString("created_at"));
                request.setAuthoredOn(set.getString("authored_on"));
                request.setName(set.getString("name"));
                request.setCategory(set.getString("category"));
                request.setCode(set.getString("code"));
                request.setNotes(cleanString(set.getString("notes")));
                request.setPriority(set.getString("priority"));
                request.setStatus(set.getString("status"));
                request.setDosageDisplay(set.getString("dosage_display"));
                request.setServiceProviderId("161380e9-22d3-4627-a97f-0f918ce3e4a9");
                request.setServiceProviderName("Nyaho Medical Centre");
             //   request.setVisitId(ecounter.get(0).getVisitId());
                try {
                    request.setPatientId(mps.get(set.getString("patient_id")).getUuid());
                } catch (Exception e) {
                    logger.info("patient not found");
                }
                try {

                    request.setPractitionerId(doc.get(set.getString("practitioner_id")));
                    request.setPractitionerName(set.getString("practitioner_name"));
                } catch (Exception e) {
                    logger.info("doctor not found");
                }

              //  request.setEncounterId(ecounter.get(0).getUuid());
                requests.add(request);
        

        }

        ExecutorService executorService = Executors.newFixedThreadPool(10);
        try {
            List<Future<Integer>> futures = executorService.invokeAll(submitTask2( 1000,requests));
            for (Future<Integer> future : futures) {
                System.out.println("future.get = " + future.get());
            }
        } catch (InterruptedException | ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }
        
       return new ArrayList<>();
    }




    public  List<MedicalRequest> medicalRequestOPD(Map<String, PatientData> mps ,Map<String, String> doc,int batchSize) {
       
        String query = """
                                        select

                	pm.PatientMedicine_ID uuid,

                	pm.EntryDate created_at,

                	pm.EntryDate updated_at,

                	pm.EntryDate authored_on,

                	IFNULL(im.TypeName, pm.MedicineName) name,

                	"outpatient" category,

                	pm.Medicine_ID code,

                	null date,

                	pm.Remarks notes,

                	null intended_dispenser,

                	"routine" priority,

                	"completed" status,

                	concat(pm.dose, " - ", pm.NoTimesDay, " - ", pm.NoOfDays) dosage_display,

                	null dosage_form,

                	null dosage_route,

                	null dosage_site,

                	null dosage_frequency,

                	null dosage_frequency_unit,
                	null dose,

                	null dose_unit,

                	null dosage_strength,

                	null dosage_period,

                	null course_of_therapy,

                	null quantity_to_dispense,

                	null number_of_refills,

                	null dosage_period_unit,

                	"Nyaho Medical Centre" service_provider_id,

                	null encounter_id,

                	pm.Transaction_ID visit_id,

                	pm.Patient_ID patient_id,

                	pm.Patient_ID mr_number,

                	patient_master.PName patient_full_name,

                	CONCAT(dm.Title, ' ', dm.Name) practitioner_name,

                	pm.DoctorID practitioner_id

                from

                	patient_medicine pm

                join doctor_master dm on dm.Doctor_ID = pm.DoctorID

                join patient_master on patient_master.Patient_ID = pm.Patient_ID

                left join f_itemmaster im on

                	pm.Medicine_ID = im.ItemID

                where

                	pm.IsChange = 0

                	and pm.isReject = 0

                 LIMIT ?, 1000
                                    """;

        List<MedicalRequest> requests = new ArrayList<>();

        SqlRowSet set = hisJdbcTemplate.queryForRowSet(query,batchSize);
        while (set.next()) {
          
           
                MedicalRequest request = new MedicalRequest();
                request.setUuid(UUID.randomUUID().toString());
                request.setCreatedAt(set.getString("created_at"));
                request.setAuthoredOn(set.getString("authored_on"));
                request.setName(set.getString("name"));
                request.setCategory(set.getString("category"));
                request.setExternalSystem("his");
                request.setExternalId(set.getString("visit_id"));
                request.setCode(set.getString("code"));
                request.setNotes(set.getString("notes"));
                request.setPriority(set.getString("priority"));
                request.setStatus(set.getString("status"));
                request.setDosageDisplay(set.getString("dosage_display"));
                request.setServiceProviderId("161380e9-22d3-4627-a97f-0f918ce3e4a9");
                request.setServiceProviderName("Nyaho Medical Centre");
                try {
                    request.setPatientId(mps.get(set.getString("patient_id")).getUuid());
                } catch (Exception e) {
                    logger.info("patient not found");
                }
                try {

                    request.setPractitionerId(doc.get(set.getString("practitioner_id")));
                    request.setPractitionerName(set.getString("practitioner_name"));
                } catch (Exception e) {
                    logger.info("doctor not found");
                }
            //    List<Encounter> encouter = encounterRepository.findByExternalIdAndAssignedToId(request.getExternalId(), request.getPractitionerId());
              //  request.setEncounterId(encouter.get(0).getUuid());
               // request.setVisitId(encouter.get(0).getVisitId());
               // request.setVisitId(UUID.randomUUID().toString());
              ///  Encounter encounter = new Encounter(request, mps.get(set.getString("patient_id")), "his");
             //   Visits visit = new Visits(encounter);
             //   visits.add(visit);
             //   encounters.add(encounter);
                requests.add(request);
               

        

        }
       return requests;
    }

    public List<MedicalRequest> medicalRequestIPD(Map<String, PatientData> mps, Map<String, String> doc,int batchSize) {
      
        String query = """
                        Select om.EntryID uuid,

                  om.EntryDate created_at,

                  case

                    when om.UpdateDateTime is null then om.EntryDate

                    else concat(om.UpdateDateTime, ' 00:00:00')

                  end updated_at,

                  om.EntryDate authored_on,

                  om.MedicineName name,

                  "inpatient" category,

                  om.MedicineID code,

                  null date,

                  om.Remark notes,

                  null intended_dispenser,

                  "routine" priority,

                  case

                    when IFNULL(id.ReceiveQty, om.ReqQty) = 0 then "draft"

                    when (

                      IFNULL(id.ReceiveQty, om.ReqQty) - ifnull(

                        (

                          select SUM(Qty)

                          from cpoe_medication_record mr

                          where itemID = om.MedicineID

                            and TransactionID = pmh.Transaction_ID

                            and IndentNo = om.IndentNo

                        ),

                        0

                      )

                    ) = 0 then "completed"

                    when DATE(om.Duration) < DATE(NOW()) then "ended"

                    when ifnull(

                      (

                        select STATUS

                        from cpoe_medication_record mr

                        where itemID = om.MedicineID

                          and TransactionID = pmh.Transaction_ID

                          and IndentNo = om.IndentNo

                        order by id desc

                        limit 1

                      ), 0

                    ) = 0 then "active"

                    when (

                      select STATUS

                      from cpoe_medication_record mr

                      where itemID = om.MedicineID

                        and TransactionID = pmh.Transaction_ID

                        and IndentNo = om.IndentNo

                      order by id desc

                      limit 1

                    ) = 2 then "stopped"

                    else "unknown"

                  end status,

                  concat(om.Dose, " - ", om.Timing, " - ", om.Duration) dosage_display,

                  null dosage_form,

                  null dosage_route,

                  null dosage_site,

                  null dosage_frequency,

                  null dosage_frequency_unit,

                  null dose,

                  null dose_unit,

                  null dosage_strength,

                  null dosage_period,

                  null course_of_therapy,

                  null quantity_to_dispense,

                  null number_of_refills,

                  null dosage_period_unit,

                  null encounter_id,

                  om.TransactionID visit_id,

                  pmh.Patient_ID patient_id,

                  CONCAT(dm.Title, ' ', dm.Name) practitioner_name,

                  pmh.Doctor_ID practitioner_id

                from orderset_medication om

                  inner join patient_medical_history pmh on pmh.Transaction_ID = om.TransactionID

                  inner join doctor_master dm on dm.Doctor_ID = pmh.Doctor_ID

                  inner join patient_master on patient_master.Patient_ID = pmh.Patient_ID

                  left outer join f_indent_detail_patient id on om.IndentNo = id.IndentNo

                  and om.MedicineID = id.ItemId

                  left outer join f_salesdetails sd on sd.IndentNo = id.IndentNo

                  and sd.ItemID = id.ItemId

                  and sd.TrasactionTypeID = '3'

                order by om.EntryDate desc
                LIMIT ?,1000
                    """;

       // List<Encounter> encounters = new ArrayList<>();
        List<MedicalRequest> requests = new ArrayList<>();
       // List<Visits> visits = new ArrayList<>();

        SqlRowSet set = hisJdbcTemplate.queryForRowSet(query,batchSize);
        while (set.next()) {

           /*  String patientMr = set.getString("patient_id");
            String date = set.getString("created_at");
            String doctor = set.getString("practitioner_id");
            Optional<Encounter> ecounter = encounterRepository.findEcounterByPatientDateDoctor(patientMr, date, doctor);
            if (ecounter.isPresent()) {
                MedicalRequest request = new MedicalRequest();
                request.setUuid(UUID.randomUUID().toString());
                request.setCreatedAt(set.getString("created_at"));
                request.setAuthoredOn(set.getString("authored_on"));
                request.setName(set.getString("name"));
                request.setCategory(set.getString("category"));
                request.setCode(set.getString("code"));
                request.setNotes(cleanString(set.getString("notes")));
                request.setPriority(set.getString("priority"));
                request.setStatus(set.getString("status"));
                request.setDosageDisplay(set.getString("dosage_display"));
                request.setServiceProviderId("161380e9-22d3-4627-a97f-0f918ce3e4a9");
                request.setServiceProviderName("Nyaho Medical Centre");
                try {
                    request.setPatientId(mps.get(set.getString("patient_id")).getUuid());
                } catch (Exception e) {
                    logger.info("patient not found");
                }
                try {

                    request.setPractitionerId(doc.get(set.getString("practitioner_id")));
                    request.setPractitionerName(set.getString("practitioner_name"));
                } catch (Exception e) {
                    logger.info("doctor not found");
                }

                request.setEncounterId(ecounter.get().getUuid());
                requests.add(request);
            } else { */

                MedicalRequest request = new MedicalRequest();
                request.setUuid(UUID.randomUUID().toString());
                request.setCreatedAt(set.getString("created_at"));
                request.setAuthoredOn(set.getString("authored_on"));
                request.setName(set.getString("name"));
                request.setCategory(set.getString("category"));
                request.setCode(set.getString("code"));
                request.setNotes(set.getString("notes"));
                request.setPriority(set.getString("priority"));
                request.setStatus(set.getString("status"));
                request.setDosageDisplay(set.getString("dosage_display"));
                request.setServiceProviderId("161380e9-22d3-4627-a97f-0f918ce3e4a9");
                request.setServiceProviderName("Nyaho Medical Centre");
                try {
                    request.setPatientId(mps.get(set.getString("patient_id")).getUuid());
                } catch (Exception e) {
                    logger.info("patient not found");
                }
                try {

                    request.setPractitionerId(doc.get(set.getString("practitioner_id")));
                    request.setPractitionerName(set.getString("practitioner_name"));
                } catch (Exception e) {
                    logger.info("doctor not found");
                }

                request.setEncounterId(UUID.randomUUID().toString());
                //Encounter encounter = new Encounter(request, mps.get(set.getString("patient_id")), "his");
                //Visits visit = new Visits(encounter);
                //visits.add(visit);
                //encounters.add(encounter);
                request.setVisitId(UUID.randomUUID().toString());
                requests.add(request);

            }

        
       // saveInBatches(requests, medicalRequestRepository, 100);
     //   saveInBatches(encounters, encounterRepository, 2000);
       // saveInBatches(visits, visitRepository, 2000);

       logger.info("Results are "+ requests.size());
       return requests;
    }

    

    public void IPDThread() {

    
        List<MedicalRequest> notes = IPDDataThread();

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
		logger.info("Starting importing Medical Requests");

    }

    




    public void OPDThread() {

    
        List<MedicalRequest> notes = OPDDataThread();

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
		logger.info("Starting importing Medical Requests");

    }




    public List<MedicalRequest> IPDDataThread() {
        Map<String, PatientData> mps = patientRepository.findAll().stream()
        .collect(Collectors.toMap(e -> e.getExternalId(), e -> e));
Map<String, String> doc = doctorRepository.findHisPractitioners().stream()
        .collect(Collectors.toMap(e -> e.getExternalId(), e -> e.getSerenityUUid()));
        List<MedicalRequest> data = new ArrayList<>();
        String sql ="""
         select

                count(*)

                from orderset_medication om

                  inner join patient_medical_history pmh on pmh.Transaction_ID = om.TransactionID

                  inner join doctor_master dm on dm.Doctor_ID = pmh.Doctor_ID

                  inner join patient_master on patient_master.Patient_ID = pmh.Patient_ID

                  left outer join f_indent_detail_patient id on om.IndentNo = id.IndentNo

                  and om.MedicineID = id.ItemId

                  left outer join f_salesdetails sd on sd.IndentNo = id.IndentNo

                  and sd.ItemID = id.ItemId

                  and sd.TrasactionTypeID = '3'

                order by om.EntryDate desc  
        
                    """;
       
        @SuppressWarnings("null")
        int rows = hisJdbcTemplate.queryForObject(sql, Integer.class);            
        logger.info("data rows found "+rows);
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        try {
            List<Future<List<MedicalRequest>>> futures = executorService.invokeAll(getMedicalIPDRequestsData( 1000,rows,mps,doc));
            for (Future<List<MedicalRequest>> future : futures) {
                data.addAll(future.get());

            }

            
        } catch (InterruptedException | ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        executorService.shutdown();
        System.err.println("patiend count is "+data.size());
        return data;
    }

  


    public List<MedicalRequest> OPDDataThread() {
        Map<String, PatientData> mps = patientRepository.findAll().stream()
        .collect(Collectors.toMap(e -> e.getExternalId(), e -> e));
Map<String, String> doc = doctorRepository.findHisPractitioners().stream()
        .collect(Collectors.toMap(e -> e.getExternalId(), e -> e.getSerenityUUid()));
        List<MedicalRequest> data = new ArrayList<>();
        String sql ="""
                
     select

                count(*)

                from

                	patient_medicine pm

                join doctor_master dm on dm.Doctor_ID = pm.DoctorID

                join patient_master on patient_master.Patient_ID = pm.Patient_ID

                left join f_itemmaster im on

                	pm.Medicine_ID = im.ItemID

                where

                	pm.IsChange = 0

                	and pm.isReject = 0
                   
                    """;
       
        @SuppressWarnings("null")
        int rows = hisJdbcTemplate.queryForObject(sql, Integer.class);            
        logger.info("data srows found "+rows);
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        try {
            List<Future<List<MedicalRequest>>> futures = executorService.invokeAll(getMedicalRequestsData( 1000,rows,mps,doc));
            for (Future<List<MedicalRequest>> future : futures) {
                data.addAll(future.get());

            }

            
        } catch (InterruptedException | ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        executorService.shutdown();
        System.err.println("patiend count is "+data.size());
        return data;
    }

  
    private static double calculateProgress(int processed, int total) {
        return Math.round((double) processed / total * 100 * 100.0) / 100.0;
    }

    public String cleanString(String input) {
        if (input == null)
            return null;
        return input.replace("\0", "")
                .replace("\u0000", "");
    }

    public Set<Callable<Integer>> submitTask2(int batchSize, List<MedicalRequest> notes) {
    
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
                medicalRequestRepository.saveAll(notes.subList(startIndex, endIndex));
                }
                catch (Exception e) {
                    // TODO: handle exception
                    e.printStackTrace();
                    logger.info("error adding note");
                    for(MedicalRequest note : notes){
                        try{
                        medicalRequestRepository.save(note);
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



    public Set<Callable<List<MedicalRequest>>> getMedicalRequestsData(int batchSize, int rows,  Map<String, PatientData> mps,Map<String, String> doc ) {
     
        Set<Callable<List<MedicalRequest>>> callables = new HashSet<>();
        int totalSize = rows;
        int batches = (totalSize + batchSize - 1) / batchSize; // Ceiling division

        for (int i = 0; i < batches; i++) {
            final int batchNumber = i; // For use in lambda

            callables.add(() -> {
                int startIndex = batchNumber * batchSize;
            
                logger.debug("Processing batch {}/{}, indices [{}]",
                        batchNumber + 1, batches, startIndex);
                return  medicalRequestOPD(mps,doc,startIndex);
                }

               
            );
        }

        return callables;
    }



    public Set<Callable<List<MedicalRequest>>> getMedicalIPDRequestsData(int batchSize, int rows,  Map<String, PatientData> mps,Map<String, String> doc ) {
     
        Set<Callable<List<MedicalRequest>>> callables = new HashSet<>();
        int totalSize = rows;
        int batches = (totalSize + batchSize - 1) / batchSize; // Ceiling division

        for (int i = 0; i < batches; i++) {
            final int batchNumber = i; // For use in lambda

            callables.add(() -> {
                int startIndex = batchNumber * batchSize;
            
                logger.debug("Processing batch {}/{}, indices [{}]",
                        batchNumber + 1, batches, startIndex);
                return  medicalRequestIPD(mps,doc,startIndex);
                }

               
            );
        }

        return callables;
    }

}