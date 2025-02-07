package com.serenity.integration.service;

import java.sql.PreparedStatement;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.repository.CrudRepository;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
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
    @Qualifier(value = "legJdbcTemplate")
    JdbcTemplate legJdbcTemplate;


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

    @Autowired
    @Qualifier("serenityJdbcTemplate")
    JdbcTemplate serenityJdbcTemplate;

    @Autowired
    @Qualifier("vectorJdbcTemplate")
    JdbcTemplate vectorJdbcTemplate;


    public List<MedicalRequest> medicalRequestOPD2() {
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
            SqlRowSet set = hisJdbcTemplate.queryForRowSet(query, startIndex);
            while (set.next()) {
                String patientMr = set.getString("patient_id");
                String date = set.getString("created_at");
                String doctor = set.getString("practitioner_id");
                String externalId = set.getString("visit_id");
                // List<Encounter> ecounter =
                // encounterRepository.findByExternalIdAndAssignedToId(externalId,
                // doc.get(doctor));

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
                request.setExternalId(set.getString("visit_id"));
                request.setExternalSystem("his");
                // request.setVisitId(ecounter.get(0).getVisitId());
                try {
                    request.setPatientId(mps.get(set.getString("patient_id")).getUuid());
                    request.setMrNumber(mps.get(set.getString("patient_id")).getMrNumber());
                    request.setPatientName(mps.get(set.getString("patient_id")).getFullName());

                } catch (Exception e) {
                    logger.info("patient not found");
                }
                try {

                    request.setPractitionerId(doc.get(set.getString("practitioner_id")));
                    request.setPractitionerName(set.getString("practitioner_name"));
                } catch (Exception e) {
                    logger.info("doctor not found");
                }

                // request.setEncounterId(ecounter.get(0).getUuid());
                requests.add(request);

            }

            ExecutorService executorService = Executors.newFixedThreadPool(10);
            try {
                List<Future<Integer>> futures = executorService.invokeAll(submitTask2(1000, requests));
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

    public void medicalRequestIPD() {

        Map<String, PatientData> mps = patientRepository.findAll().stream()
                .collect(Collectors.toMap(e -> e.getExternalId(), e -> e));
        Map<String, String> doc = doctorRepository.findHisPractitioners().stream()
                .collect(Collectors.toMap(e -> e.getExternalId(), e -> e.getSerenityUUid()));
                String sql = """
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
            int totalSize = hisJdbcTemplate.queryForObject(sql, Integer.class);
        int batches = (totalSize + 10000 - 1) / 10000; // Ceiling division

        for (int i = 0; i < batches; i++) {
            int startIndex = i * 10000;
            int endIndex = Math.min(startIndex + 10000, totalSize);

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
                    LIMIT ?,10000
                        """;

            List<MedicalRequest> requests = new ArrayList<>();
            System.err.println(" statring the rowset");
            SqlRowSet set = hisJdbcTemplate.queryForRowSet(query, startIndex);
            while (set.next()) {

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
                    request.setMrNumber(mps.get(set.getString("patient_id")).getMrNumber());
                    request.setPatientName(mps.get(set.getString("patient_id")).getFullName());

                } catch (Exception e) {
                    logger.info("patient not found");
                }
                try {

                    request.setPractitionerId(doc.get(set.getString("practitioner_id")));
                    request.setPractitionerName(set.getString("practitioner_name"));
                } catch (Exception e) {
                    logger.info("doctor not found");
                }

                request.setVisitId(set.getString("visit_id"));
                requests.add(request);

            }

            ExecutorService executorService = Executors.newFixedThreadPool(10);
            try {
                List<Future<Integer>> futures = executorService.invokeAll(submitTask2(1000, requests));
                for (Future<Integer> future : futures) {
                    System.out.println("future.get = " + future.get());
                }
            } catch (InterruptedException | ExecutionException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
        cleanDAta();
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
                try {
                    medicalRequestRepository.saveAll(notes.subList(startIndex, endIndex));
                } catch (Exception e) {
                    // TODO: handle exception
                    e.printStackTrace();
                    logger.info("error adding note");
                    for (MedicalRequest note : notes) {
                        try {
                            medicalRequestRepository.save(note);
                        } catch (Exception es) {
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

    
    public void saveMedicalRequestThread() {

        long count = medicalRequestRepository.count();

        ExecutorService executorService = Executors.newFixedThreadPool(10);
        try {
            List<Future<Integer>> futures = executorService.invokeAll(submitTask2(1000, count));
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
                List<MedicalRequest> notes = medicalRequestRepository.findByExternalSystem(startIndex);

                try {
                    saveMedicalRequest(notes);
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

    public void saveMedicalRequest(List<MedicalRequest> requests) {

        String sql = """
                        INSERT INTO public.medication_requests
                (created_at, pk, service_provider_id, "uuid", "name",
                category, code, notes, priority, status,
                encounter_id,patient_id, patient_mr_number, patient_full_name,
                practitioner_name, practitioner_id,  visit_id)
                 VALUES(to_timestamp(?, 'YYYY-MM-DD HH24:MI:SS'), ?, uuid(?), uuid(?),  ?,
                 ?, ?, ?,?, ?,
                 uuid(?), uuid(?), ?, ?, ?,
                  uuid(?), uuid(?))
                        """;

        serenityJdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {

            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                // TODO Auto-generated method stub
                MedicalRequest request = requests.get(i);
                ps.setString(1, request.getCreatedAt());
                ps.setLong(2, request.getId());
                ps.setString(3, "161380e9-22d3-4627-a97f-0f918ce3e4a9");
                ps.setString(4, request.getUuid());
                ps.setString(5, request.getName()==null?"":request.getName());
                ps.setString(6, request.getCategory());
                ps.setString(7, request.getCode());
                ps.setString(8, request.getNotes());
                ps.setString(9, request.getPriority());
                ps.setString(10, request.getStatus());

                ps.setString(11, request.getEncounterId());
                ps.setString(12, request.getPatientId());
                ps.setString(13, request.getMrNumber());
                ps.setString(14, request.getPatientName());
                ps.setString(15, request.getPractitionerName());

                ps.setString(16, request.getPractitionerId());
                ps.setString(17, request.getVisitId());

            }

            @Override
            public int getBatchSize() {
                // TODO Auto-generated method stub
                return requests.size();
            }

        });

    }


    public void saveMedicalRequestNoThread() {
        String sqls ="select count(*) from medicalrequest where  practitionerid is not null and visitid is not null and patientid  is not null and encounterid is not  null";
        int totalSize = vectorJdbcTemplate.queryForObject(sqls, Integer.class);
        logger.info("Total dump size "+totalSize);
        int batchSize = 1000;
        int batches = (totalSize + batchSize - 1) / batchSize; // Ceiling division

        for (int i = 0; i < batches; i++) {
            logger.info("Starting medicalResult dump");
            int startIndex = i * batchSize;
            List<MedicalRequest> requests = medicalRequestRepository.findByExternalSystem(startIndex);

        String sql = """
                        INSERT INTO public.medication_requests
                (created_at, pk, service_provider_id, "uuid", "name",
                category, code, notes, priority, status,
                encounter_id,patient_id, patient_mr_number, patient_full_name,
                practitioner_name, practitioner_id,  visit_id)
                 VALUES(to_timestamp(?, 'YYYY-MM-DD HH24:MI:SS'), nextval('medication_requests_pk_seq'::regclass), uuid(?), uuid(?),  ?,
                 ?, ?, ?,?, ?,
                 uuid(?), uuid(?), ?, ?, ?,
                  uuid(?), uuid(?))
                        """;

        serenityJdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {

            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {

                MedicalRequest request = requests.get(i);
                ps.setString(1, request.getCreatedAt());
              //  ps.setLong(2, request.getId());
                ps.setString(2, "161380e9-22d3-4627-a97f-0f918ce3e4a9");
                ps.setString(3, request.getUuid());
                ps.setString(4, request.getName()==null?"":request.getName());
                ps.setString(5, request.getCategory());
                ps.setString(6, request.getCode());
                ps.setString(7, request.getNotes());
                ps.setString(8, request.getPriority());
                ps.setString(9, request.getStatus());

                ps.setString(10, request.getEncounterId());
                ps.setString(11, request.getPatientId());
                ps.setString(12, request.getMrNumber());
                ps.setString(13, request.getPatientName());
                ps.setString(14, request.getPractitionerName());

                ps.setString(15, request.getPractitionerId());
                ps.setString(16, request.getVisitId());

            }

            @Override
            public int getBatchSize() {
                // TODO Auto-generated method stub
                return requests.size();
            }

        });

    }

}


    public void cleanDAta(){
String sql ="""
        update medicalrequest 
set encounterid = v."uuid" 
from encounter  v
where medicalrequest.externalid =v.external_id  

        """;

        vectorJdbcTemplate.update(sql);
        sql ="""
                
                """;
    }


  
public void getLegacyRequest2() {
    logger.info("Starting importing Medical Requests");
    Map<String, PatientData> mps = patientRepository.findAll().stream()
    .collect(Collectors.toMap(e -> e.getExternalId(), e -> e));
    // Step 1: Get the total number of rows
    String sqlCount = "SELECT count(*) FROM medication_request m JOIN patient p ON m.patient_id = p.id";
    int rows = legJdbcTemplate.queryForObject(sqlCount, Integer.class);
    int batchSize = 1000;
    long batches = ((rows + batchSize - 1) / batchSize); // Ceiling division
    
    for (int i = 0; i < batches; i++) {
        int startIndex = i * batchSize;

movetoHub(startIndex, batchSize,mps);
    }
    cleanLegacyRequest();
            // Step 6: Clean up resources
}
    public Set<Callable<Integer>> submitLegacyNotes(int batchSize, long rows,Map<String, PatientData> mps) {
        Set<Callable<Integer>> callables = new HashSet<>();
        long totalSize = rows; // Use long to avoid potential overflow
        long batches = ((totalSize + batchSize - 1) / batchSize); // Ceiling division
    
        for (int i = 0; i < batches; i++) {
            final int batchNumber = i; // For use in lambda
    
            callables.add(() -> {
                int startIndex = batchNumber * batchSize;
                logger.debug("Processing batch {}/{}, indices [{}]",
                        batchNumber + 1, batches, startIndex);
    
                try {
                    movetoHub(startIndex, batchSize,mps);
                    // Return the number of rows processed or a status code
                    return batchSize;
                } catch (Exception e) {
                    logger.error("Error processing batch {}/{}: {}", batchNumber + 1, batches, e.getMessage(), e);
                    // Return an error code or rethrow the exception
                    throw new RuntimeException("Failed to process batch " + (batchNumber + 1), e);
                }
            });
        }
    
        return callables;
    }

    public int movetoHub(int offset,int limit,Map<String, PatientData> mps){
        List<MedicalRequest> medicalRequests = new ArrayList<>();
        String sql ="SELECT * FROM medication_request m join patient p on m.patient_id=p.id order by patient_id OFFSET ? LIMIT ?";
        SqlRowSet set = legJdbcTemplate.queryForRowSet(sql,offset,limit);
        while (set.next()) {
            PatientData patient = mps.get(set.getString("mr_number"));
            MedicalRequest request = new MedicalRequest();
            request.setCode(set.getString("code"));
            request.setCategory(set.getString("category"));
            request.setAuthoredOn(set.getString("authored_on"));
            request.setCreatedAt(set.getString("created_at"));
            request.setUuid(set.getString("id"));
            request.setEncounterId(set.getString("encounter_id"));
            request.setName(set.getString("name"));
            request.setExternalId(set.getString("id"));
            request.setExternalSystem("opd");
            request.setPatientId(patient.getUuid());
            request.setMrNumber(patient.getMrNumber());
            
            request.setPatientName(patient.getFullName());
            request.setVisitId(set.getString("visit_id"));
            request.setPriority(set.getString("priority"));
            request.setStatus(set.getString("status"));
            medicalRequests.add(request);
            
        }
        medicalRequestRepository.saveAll(medicalRequests);
        return 1;

    }


public void cleanLegacyRequest(){
String sql ="""
  update medicalrequest k
set practitionerid=e.assigned_to_id,practitionername=assigned_to_name ,visitid=e.visit_id 
from  encounter e
where e.external_id =k.encounterid  and k.externalsystem ='opd'
        """;

vectorJdbcTemplate.update(sql);
}
}