package com.serenity.integration.service;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
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
import com.serenity.integration.models.PatientData;
import com.serenity.integration.models.Practitioner;
import com.serenity.integration.models.Visits;
import com.serenity.integration.repository.DoctorRepository;
import com.serenity.integration.repository.PatientRepository;
import com.serenity.integration.repository.VisitRepository;

@Service
public class VisitService {
    @Autowired
    @Qualifier("serenityJdbcTemplate")
    JdbcTemplate serenityJdbcTemplate;

    @Autowired
    VisitRepository visitRepository;

    @Autowired
    @Qualifier(value = "hisJdbcTemplate")
    JdbcTemplate hisJdbcTemplate;


    @Autowired
    @Qualifier(value = "legJdbcTemplate")
    JdbcTemplate legJdbcTemplate;

    @Autowired
    PatientRepository patientRepository;

    @Autowired
    DoctorRepository doctorRepository;
  

    Logger logger = LoggerFactory.getLogger(getClass());
    public int loadVisits(int size, Map<String,PatientData> mps,Map<String,String> doc ) {
        List<Visits> visits = new ArrayList<>();
     
        String sql = """
                SELECT 
    pmh.Transaction_ID as uuid,
    pmh.DateOfVisit created_at,
    pmh.Updatedate updated_at,
    CASE
        WHEN pmh.Type = 'IPD' AND pmh.Admission_Type = 'Emergency' THEN 'emergency'
        WHEN pmh.Type = 'IPD' AND pmh.Admission_Type <> 'Emergency' THEN 'inpatient-encounter'
        ELSE 'ambulatory'
    END as encounter_class,
    'finished' as status,
    '' as display,
    CASE
        WHEN pmh.Type = 'IPD' AND pmh.Admission_Type = 'Emergency' THEN 'stat'
        WHEN pmh.Type = 'IPD' AND pmh.Admission_Type <> 'Emergency' THEN 'ASAP'
        ELSE 'routine'
    END as priority,
    CAST(CONCAT(CAST(app.Date as date), ' ', CAST(app.Time as time)) as datetime) planned_start,
    CAST(CONCAT(CAST(app.Date as date), ' ', CAST(app.EndTime as time)) as datetime) planned_end,
    CAST(CONCAT(CAST(pmh.DateOfVisit as date), ' ', CAST(pmh.Time as time)) as datetime) started_at,
    NULL as ended_at,
    pmh.Transaction_ID external_id,
    'his' as external_system,
    app.App_ID as appointment_id,
    'Nyaho Medical Centre' as location_id,
    'Nyaho Medical Centre' as location_name,
    NULL as service_type_id,
    NULL as service_type_name,
    '161380e9-22d3-4627-a97f-0f918ce3e4a9' as service_provider_id,
    pmh.Patient_ID patient_mr_number,
    pmh.Patient_ID patient_id,
    CONCAT(pm.PfirstName, ' ', pm.PLastName) patient_full_name,
    pm.Mobile patient_mobile,
    pm.DOB patient_birth_date,
    pm.Gender patient_gender,
    'departed' as patient_status,
    pmh.Doctor_ID created_by_id,
    CONCAT(dm.Title, ' ', dm.Name) created_by_name,
    pmh.Transaction_ID user_friendly_id,
    CONCAT(dm.Title, ' ', dm.Name) assigned_to_name,
    pmh.Doctor_ID assigned_to_id,
    'Nyaho Medical Centre' as service_provider_name
FROM patient_medical_history pmh
INNER JOIN patient_master pm ON pm.Patient_ID = pmh.Patient_ID
INNER JOIN doctor_master dm ON pmh.Doctor_ID = dm.Doctor_ID
INNER JOIN f_ledgertransaction lt ON lt.Transaction_ID = pmh.Transaction_ID
INNER JOIN appointment app ON app.ledgertnxNo = lt.LedgerTransactionNo
LIMIT ?,10000
                """;
        SqlRowSet set = hisJdbcTemplate.queryForRowSet(sql,size);
        while (set.next()) {
            Visits visit = new Visits();
            visit.setUuid(UUID.randomUUID());
            visit.setCreatedAt(set.getString(2));
            visit.setEncounterClass(set.getString("encounter_class"));
            visit.setStatus(set.getString("status"));
            visit.setPriority(set.getString("priority"));
            visit.setStartedAt(set.getString("started_at"));
            visit.setEndedAt(set.getString("ended_at"));
            visit.setExternalSystem("his");
            visit.setExternalId(set.getString(1));
            visit.setServiceProviderId("161380e9-22d3-4627-a97f-0f918ce3e4a9");
            visit.setServiceProviderName("Nyaho Medical Centre");
            visit.setHisNumber(set.getString("patient_id"));
            visit.setGender(set.getString("patient_gender"));
            visit.setPatientMobile(set.getString("patient_mobile"));
            visit.setPatientDob(set.getString("patient_birth_date"));
            visit.setAssignedToName(set.getString("assigned_to_name"));
            visit.setAssignedToId(set.getString("assigned_to_id"));
            visit.setPatientName(set.getString("patient_full_name"));
            visit.setPatientStatus(set.getString("patient_status"));
            visit.setPatientId(mps.get(set.getString("patient_mr_number")).getUuid());
            visit.setPatientMrNumber(mps.get(set.getString("patient_mr_number")).getMrNumber());
            try{
            visit.setPractitionerId(doc.get(set.getString("assigned_to_id")));
            }catch(Exception e){
                logger.error("could not find practitioner ");
            }
            visits.add(visit);
        }

        visitRepository.saveAll(visits);
return visits.size();
    }




    public int ipdVisits(int size, Map<String,PatientData> mps,Map<String,String> doc ) {
        List<Visits> visits = new ArrayList<>();
     
        String sql = """
                
SELECT 
    ip.Transaction_ID uuid,
    CONCAT(pmh.EntryDate, ' 00:00:00') created_at,
    CASE
        WHEN pmh.Updatedate IS NULL THEN CONCAT(pmh.EntryDate, ' 00:00:00')
        ELSE pmh.Updatedate
    END as updated_at,
    'inpatient-encounter' encounter_class,
    CASE 
        WHEN ich.status = 'OUT' THEN 'finished'
        WHEN ich.status = 'Cancel' THEN 'cancelled'
        ELSE 'in-progress'
    END as status,
    '' display,
    'routine' priority,
    NULL planned_start,
    NULL planned_end,
    CAST(CONCAT(ich.DateOfAdmit, ' ', ich.TimeOfAdmit) as datetime) started_at,
    CASE
        WHEN ich.DateOfDischarge != '0001-01-01' 
            AND ich.TimeOfDischarge != '00:00:00' 
            AND ich.Status = 'OUT' 
        THEN CAST(CONCAT(ich.DateOfDischarge, ' ', ich.TimeOfDischarge) as datetime)
        ELSE NULL
    END ended_at,
    ip.Transaction_ID external_id,
    'his' external_system,
    NULL appointment_id,
    'Nyaho Medical Centre' location_id,
    'Nyaho Medical Centre' location_name,
    NULL service_type_id,
    NULL service_type_name,
    '161380e9-22d3-4627-a97f-0f918ce3e4a9' service_provider_id,
    pm.Patient_ID patient_mr_number,
    pmh.Patient_ID patient_id,
    pm.PName patient_full_name,
    pm.Mobile patient_mobile,
    pm.DOB patient_birth_date,
    pm.Gender patient_gender,
    CASE 
        WHEN ich.status = 'OUT' THEN 'departed'
        WHEN ich.status = 'Cancel' THEN 'departed'
        ELSE 'in-progress'
    END as patient_status,
    pmh.UserID created_by_id,
    CONCAT(e_mas.Title, ' ', e_mas.Name) created_by_name,
    NULL user_friendly_id,
    CONCAT(dm.Title, ' ', dm.Name) assigned_to_name,
    dm.Doctor_ID assigned_to_id,
    'Nyaho Medical Centre' service_provider_name
FROM patient_ipd_profile ip
INNER JOIN patient_medical_history pmh ON ip.Transaction_ID = pmh.Transaction_ID
INNER JOIN ipd_case_history ich ON ip.Transaction_ID = ich.Transaction_ID
INNER JOIN room_master rm ON rm.Room_Id = ip.Room_ID
INNER JOIN ipd_case_type_master ctm ON ctm.IPDCaseType_ID = rm.IPDCaseTypeID
INNER JOIN doctor_master dm ON dm.Doctor_ID = ich.Consultant_ID
INNER JOIN employee_master e_mas ON e_mas.Employee_ID = pmh.UserID
INNER JOIN employee_master discharge_user ON discharge_user.Employee_ID = ich.DischargedBy
INNER JOIN patient_master pm ON pm.Patient_ID = ip.PatientID
INNER JOIN f_ipdadjustment fi ON fi.Transaction_ID = ip.Transaction_ID
INNER JOIN f_panel_master pnl ON pnl.Panel_ID = ip.PanelID
GROUP BY 
    ip.Transaction_ID,
    pmh.EntryDate,
    pmh.Updatedate,
    ich.Status,
    ich.DateOfAdmit,
    ich.TimeOfAdmit,
    ich.DateOfDischarge,
    ich.TimeOfDischarge,
    pm.Patient_ID,
    pmh.Patient_ID,
    pm.PName,
    pm.Mobile,
    pm.DOB,
    pm.Gender,
    ich.Employee_ID,
    dm.Title,
    dm.Name,
    dm.Doctor_ID
HAVING MIN(ip.StartDate) LIMIT ? ,1000
                """;
        SqlRowSet set = hisJdbcTemplate.queryForRowSet(sql,size);
        while (set.next()) {
            Visits visit = new Visits();
            visit.setUuid(UUID.randomUUID());
            visit.setCreatedAt(set.getString(2));
            visit.setEncounterClass(set.getString("encounter_class"));
            visit.setStatus(set.getString("status"));
            visit.setPriority(set.getString("priority"));
            visit.setStartedAt(set.getString("started_at"));
            visit.setEndedAt(set.getString("ended_at"));
            visit.setExternalSystem("his");
            visit.setExternalId(set.getString(1));
            visit.setServiceProviderId("161380e9-22d3-4627-a97f-0f918ce3e4a9");
            visit.setServiceProviderName("Nyaho Medical Centre");
            visit.setHisNumber(set.getString("patient_id"));
            visit.setGender(set.getString("patient_gender"));
            visit.setPatientMobile(set.getString("patient_mobile"));
            visit.setPatientDob(set.getString("patient_birth_date"));
            visit.setAssignedToName(set.getString("assigned_to_name"));
            visit.setAssignedToId(set.getString("assigned_to_id"));
            visit.setPatientName(set.getString("patient_full_name"));
            visit.setPatientStatus(set.getString("patient_status"));
            visit.setPatientId(mps.get(set.getString("patient_mr_number")).getUuid());
            visit.setPatientMrNumber(mps.get(set.getString("patient_mr_number")).getMrNumber());
            try{
            visit.setPractitionerId(doc.get(set.getString("assigned_to_id")));
            }catch(Exception e){
                logger.error("could not find practitioner ");
            }
            visits.add(visit);
        }

        visitRepository.saveAll(visits);
return visits.size();
    }

 
    public void insertIntoSerenity(List<Visits> visitss) {
        List<Visits> visits = new ArrayList<>();

        System.err.println("Settting variables ");
        visitss.stream().forEach(e -> {
            if (patientRepository.findByExternalId(e.getHisNumber()).isPresent()) {

                visits.add(e);
            }
        });

        String sql = "INSERT INTO public.visits " + //
                "(created_at,  id,  \"uuid\", encounter_class, status," +
                "priority,  started_at, ended_at, external_id, external_system," +
                "service_provider_id, service_provider_name, patient_mr_number, patient_id, patient_full_name," +
                "patient_mobile, patient_birth_date, patient_gender, patient_status, assigned_to_name, assigned_to_id,display)\n"
                + //
                "VALUES(to_timestamp(?, 'YYYY-MM-DD HH24:MI:SS'),nextval('visits_id_seq'::regclass),uuid(?),?  ,?," +
                "?,to_timestamp(?, 'YYYY-MM-DD HH24:MI:SS'),to_timestamp(?, 'YYYY-MM-DD HH24:MI:SS'),?,?," +
                "uuid(?),?,?,(select uuid from patients p where external_id =?),?," +
                "?,TO_DATE(?, 'YYYY/MM/DD'),?,?,?,uuid(?),?)";

        System.err.println("Settting Insert values ");

        serenityJdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {

            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                // TODO Auto-generated method stub
                ps.setString(1, visits.get(i).getCreatedAt() + " 08:03:02.226");
                ps.setString(2, visits.get(i).getUuid().toString());
                ps.setString(3, visits.get(i).getEncounterClass());
                ps.setString(4, visits.get(i).getStatus());

                ps.setString(5, visits.get(i).getPriority());
                ps.setString(6, visits.get(i).getStartedAt().replaceAll("T", " "));
                ps.setString(7, visits.get(i).getEndedAt().replaceAll("T", " "));
                ps.setString(8, visits.get(i).getExternalId());

                ps.setString(9, "his");

                ps.setString(10, visits.get(i).getServiceProviderId());
                ps.setString(11, visits.get(i).getServiceProviderName());
                try {
                    ps.setString(12, visits.get(i).getPatientMrNumber());
                } catch (Exception e) {
                    System.err.println("cannot find patient");

                }
                ps.setString(13, visits.get(i).getHisNumber());

                ps.setString(14, visits.get(i).getPatientName());
                ps.setString(15, visits.get(i).getPatientMobile());
                ps.setString(16, visits.get(i).getPatientDob());

                ps.setString(17, visits.get(i).getGender());
                ps.setString(18, visits.get(i).getPatientStatus());
                ps.setString(19, visits.get(i).getAssignedToName());
                try {
                    ps.setString(20, visits.get(i).getPractitionerId());
                } catch (Exception e) {
                    System.err.println("cannot find doctor");

                }
                ps.setString(21, visits.get(i).getHisNumber());

            }

            @Override
            public int getBatchSize() {
                // TODO Auto-generated method stub
                return visits.size();
            }

        });

    }

    

    public List<Visits>  getLegacyVisit(){
        List<Visits> visits = new ArrayList<>();
        String sql = "SELECT * FROM visit v join patient p  on p.id = v.patient_id";
        SqlRowSet set = legJdbcTemplate.queryForRowSet(sql);
        Map<String,PatientData> mps = patientRepository.findAll().stream().collect(Collectors.toMap(e -> e.getExternalId(), e -> e));
        Map<String,Doctors> doc = doctorRepository.findAll().stream().collect(Collectors.toMap(e -> e.getExternalId(), e -> e));

        while(set.next()){
            System.err.println(set.getString(1) +"=========");
          //  Optional<PatientData> data = patientRepository.findByExternalId(set.getString("mr_number"));
            Visits visit = new Visits();
            visit.setUuid(UUID.fromString(set.getString("uuid")));
            visit.setCreatedAt(set.getString("created_at"));
            visit.setStatus(set.getString("status"));
            visit.setStartedAt(set.getString("arrived_at"));
            visit.setEndedAt(set.getString("ended_at"));
            visit.setHisNumber(set.getString("mr_number"));
            visit.setExternalSystem("opd");
            visit.setExternalId(set.getString(5));
            visit.setAssignedToId(set.getString("assigned_to_id"));
            try{
                visit.setAssignedToName(doc.get(set.getString("assigned_to_id")).getFullName());
            }catch (Exception e){
                visit.setAssignedToName("");

            }
            visit.setPractitionerId(doc.get(set.getString("assigned_to_id")).getSerenityUUid());
            visit.setLocationId(set.getString("primary_location_id"));
            visit.setPatientMobile(set.getString("mobile"));
            visit.setPatientName(set.getString("first_name")+" "+set.getString("last_name"));
            visit.setPatientDob(set.getString("birth_date"));
            visit.setGender(set.getString("gender"));
            visit.setEncounterClass(set.getString("visit_class"));
            visit.setPatientId(mps.get(set.getString("mr_number")).getUuid());
            try{
            visit.setPatientMrNumber(mps.get(set.getString("mr_number")).getMrNumber());
            }catch(Exception e){
                logger.info("patient not found");

            }
            visit.setDisplay("opd-"+visit.getHisNumber());
            visit.setServiceProviderId("161380e9-22d3-4627-a97f-0f918ce3e4a9");
            visit.setServiceProviderName("Nyaho Medical Center");
            visit.setLocationId("23f59485-8518-4f4e-9146-d061dfe58175\"");
            visit.setLocationName("Airport Primary Care");
            visits.add(visit);
        

    }

return visits;
}



public void getHisThreads(){

int rows =640871;
    Map<String,PatientData> mps = patientRepository.findAll().stream().collect(Collectors.toMap(e -> e.getExternalId(), e -> e));
        Map<String,String> doc = doctorRepository.findHisPractitioners().stream().collect(Collectors.toMap(e -> e.getExternalId(), e -> e.getSerenityUUid()));

    ExecutorService executorService =  Executors.newFixedThreadPool(10);
        try {
            List<Future<Integer>> futures = executorService.invokeAll(submitTask2(rows,10000,mps,doc));
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
    
    

        public void getIPDVISITSThreads(){
            int rows =28888;
                Map<String,PatientData> mps = patientRepository.findAll().stream().collect(Collectors.toMap(e -> e.getExternalId(), e -> e));
                    Map<String,String> doc = doctorRepository.findHisPractitioners().stream().collect(Collectors.toMap(e -> e.getExternalId(), e -> e.getSerenityUUid()));
            
                ExecutorService executorService =  Executors.newFixedThreadPool(10);
                    try {
                        List<Future<Integer>> futures = executorService.invokeAll(submitIPDTask(rows,1000,mps,doc));
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
                
                
                    public Set<Callable<Integer>> submitIPDTask(int visits, int batchSize, Map<String,PatientData> mps,Map<String,String> doc) {
    
                        Set<Callable<Integer>> callables = new HashSet<>();
                        int totalSize = visits;
                        int batches = (totalSize + batchSize - 1) / batchSize;  // Ceiling division
                    
                        for (int i = 0; i < batches; i++) {
                            final int batchNumber = i;  // For use in lambda
                            
                            callables.add(() -> {
                                int startIndex = batchNumber * batchSize;
                                
                                logger.debug("Processing batch {}/{}, indices [{}]", 
                                         batchNumber + 1, batches, startIndex);
                                try{
                                return ipdVisits(startIndex, mps,doc);
                                }catch(Exception e ){
                                    e.printStackTrace();
                                    return 1;
                                }
                            });
                        }
                        
                        return callables;
                    }
                    

public void getlegacyThreads(){
List<Visits>visits = getLegacyVisit();
//int rows = legJdbcTemplate.queryForObject(sql, Integer.class);

ExecutorService executorService =  Executors.newFixedThreadPool(10);
    try {
        List<Future<Integer>> futures = executorService.invokeAll(submitTask(visits,100));
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



    

    public Set<Callable<Integer>> submitTask(List<Visits> visits, int batchSize) {
        if (visits == null || visits.isEmpty() || batchSize <= 0) {
            throw new IllegalArgumentException("Invalid input parameters");
        }
    
        Set<Callable<Integer>> callables = new HashSet<>();
        int totalSize = visits.size();
        int batches = (totalSize + batchSize - 1) / batchSize;  // Ceiling division
    
        for (int i = 0; i < batches; i++) {
            final int batchNumber = i;  // For use in lambda
            
            callables.add(() -> {
                int startIndex = batchNumber * batchSize;
                int endIndex = Math.min(startIndex + batchSize, totalSize);
                
                logger.debug("Processing batch {}/{}, indices [{}, {}]", 
                         batchNumber + 1, batches, startIndex, endIndex);
                
                return saveVisits(visits.subList(startIndex, endIndex));
            });
        }
        
        return callables;
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
                try{
                return loadVisits(startIndex, mps,doc);
                }catch(Exception e ){
                    e.printStackTrace();
                    return 1;
                }
            });
        }
        
        return callables;
    }

public int saveVisits(List<Visits> visits){
    visitRepository.saveAll(visits);
return 1;

}

public void removeDuplication(){
    String sql ="""
            delete from visits a using visits b
  where a.id > b.id
    and a.externalid = b.externalid;
            """;
  
  
  }

}