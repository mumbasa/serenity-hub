package com.serenity.integration.service;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.serenity.integration.models.Encounter;
import com.serenity.integration.models.Visits;
import com.serenity.integration.repository.VisitRepository;

@Service
public class VisitMigration {

    @Autowired
    @Qualifier("serenityJdbcTemplate")
    JdbcTemplate serenityJdbcTemplate;

   /*  @Autowired
    @Qualifier("hubJdbcTemplate")
    JdbcTemplate hubJdbcTemplate; */

    @Autowired
    VisitRepository visitRepository;




    Logger logger = LoggerFactory.getLogger(this.getClass().getCanonicalName());



public void getPatientsThreads(){
String sql ="SELECT count(*) FROM visists where externalsystem='his'";
    int dataSize = 2160000;
    ExecutorService executorService =  Executors.newFixedThreadPool(10);
    try {
        List<Future<Integer>> futures = executorService.invokeAll(submitTask2( 1000,dataSize));
        for(Future<Integer> future : futures){
            System.out.println("future.get = " + future.get());
        }
    } catch (InterruptedException | ExecutionException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
    
    executorService.shutdown();
    System.err.println("patiend count is "+dataSize);
    
    
        
    }


public Set<Callable<Integer>> submitTask2(int batchSize, int rows) {
    
        Set<Callable<Integer>> callables = new HashSet<>();
        int totalSize = rows;
        int batches = (totalSize + batchSize - 1) / batchSize; // Ceiling division

        for (int i = 0; i < batches; i++) {
            final int batchNumber = i; // For use in lambda

            callables.add(() -> {
                int startIndex = batchNumber * batchSize;
               // int endIndex = Math.min(startIndex + batchSize, totalSize);
           logger.debug("Processing batch {}/{}, indices [{}]",
                        batchNumber + 1, batches, startIndex);
                
                List<Visits> vists = visitRepository.getfirst100k(startIndex);    
                return task(vists);
                
            }
               );
        }

        return callables;
    }

    

public Set<Callable<Integer>> sumitTask(List<Visits> data,int size){
Set<Callable<Integer>> callables = new HashSet<Callable<Integer>>();
int rounds = data.size()/size;

for (int i=0;i<=rounds;i++){
    if(i<rounds){
        System.err.println("Round submission "+i);
        List<Visits> ds =data.subList(i*size,(i*size)+size);
        callables.add(new Callable<Integer>() {

            @Override
            public Integer call() throws Exception {
                // TODO Auto-generated method stub
                return  task(ds);
                
            }

            
        });
    }else{
        System.err.println("Finishing Round submission "+i);

        List<Visits> ds =data.subList((i*size),data.size());
        callables.add(new Callable<Integer>() {

            @Override
            public Integer call() throws Exception {
                // TODO Auto-generated method stub
            return    task(ds);
            }

            
        });
    }


}

    return callables;
} 




    public void sumitTask2(int size){
        String sql = "SELECT external_id from public.patients";
        List<String> ids = serenityJdbcTemplate.queryForList(sql,String.class);
        System.err.println("ids are "+ids.size());
        List<Visits> patientData = visitRepository.findAll();
        
        int rounds = patientData.size()/size;
        
        for (int i=0;i<=rounds;i++){
            if(i<rounds){
                System.err.println("Round submission "+i);
                List<Visits> ds =patientData.subList(i*size,(i*size)+size);
                try{
                        task(ds);
                }catch(Exception e){
                    
                e.printStackTrace();
                }
            }else{
                System.err.println("Finishing Round submission "+i);
        
                List<Visits> ds =patientData.subList((i*size),patientData.size());
                task(ds);
    
                    
                };
            }
        
        
        }

        public int task(List<Visits> visits) {
    
            String sql = "INSERT INTO public.visits " + //
                    "(created_at,  id,  \"uuid\", encounter_class, status," +
                    "priority,  started_at, ended_at, external_id, external_system," +
                    "service_provider_id, service_provider_name, patient_mr_number, patient_id, patient_full_name," +
                    "patient_mobile, patient_birth_date, patient_gender, patient_status, assigned_to_name, assigned_to_id,display,location_id,location_name)\n"
                    + //
                    "VALUES(to_timestamp(?, 'YYYY-MM-DD HH24:MI:SS'),nextval('visits_id_seq'::regclass),uuid(?),?  ,?," +
                    "?,to_timestamp(?, 'YYYY-MM-DD HH24:MI:SS'),to_timestamp(?, 'YYYY-MM-DD HH24:MI:SS'),?,?," +
                    "uuid(?),?,?,(select uuid from patients p where external_id =?),?," +
                    "?,TO_DATE(?, 'YYYY/MM/DD'),?,?,?,uuid(?),?,uuid('23f59485-8518-4f4e-9146-d061dfe58175'),'Airport Primary Care')";
    
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
                    ps.setString(6, visits.get(i).getCreatedAt() + " 08:03:02.226");
                    ps.setString(7, visits.get(i).getCreatedAt() + " 08:03:02.226");
                    ps.setString(8, visits.get(i).getExternalId());
    
                    ps.setString(9, visits.get(i).getExternalSystem());
    
                    ps.setString(10, visits.get(i).getServiceProviderId());
                    ps.setString(11, visits.get(i).getServiceProviderName());
                  
                    ps.setString(12, visits.get(i).getPatientMrNumber());
                   
                    ps.setString(13, visits.get(i).getHisNumber());
    
                    ps.setString(14, visits.get(i).getPatientName());
                    ps.setString(15, visits.get(i).getPatientMobile());
                    ps.setString(16, visits.get(i).getPatientDob());
    
                    ps.setString(17, visits.get(i).getGender());
                    ps.setString(18, visits.get(i).getPatientStatus());
                    ps.setString(19, visits.get(i).getAssignedToName());
                    
                   ps.setString(20, visits.get(i).getPractitionerId());
                   
                    ps.setString(21, visits.get(i).getHisNumber());
                    
    
                }
    
                @Override
                public int getBatchSize() {
                    // TODO Auto-generated method stub
                    return visits.size();
                }
    
            });
    
return visits.size();

        }


}
