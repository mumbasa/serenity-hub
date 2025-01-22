package com.serenity.integration.service;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.serenity.integration.models.PatientData;
import com.serenity.integration.repository.PatientRepository;

@Service
public class PatientMigrationService {

    @Autowired
    @Qualifier("serenityJdbcTemplate")
    JdbcTemplate serenityJdbcTemplate;

    @Autowired
    @Qualifier("legJdbcTemplate")
    JdbcTemplate legJdbcTemplate;

    @Autowired
    PatientRepository patientRepository;

    @Autowired
    PatientService patientService;

    public void getPatients() {
        String sql = "SELECT external_id from public.patients";
        List<String> set = serenityJdbcTemplate.queryForList(sql, String.class);
        List<PatientData> patientData = patientRepository.findTop5();
        task(patientData);

        System.err.println("patiend count is " + patientData.size() + set.size());

    }

    public void getPatientsThreads() {
        String sql = "SELECT external_id from public.patients";
        Set<String> set = new HashSet<>(serenityJdbcTemplate.queryForList(sql, String.class));
        List<PatientData> patientData = patientRepository.findySystem().stream().filter(e -> !set.contains(e.getExternalId())).toList();
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        try {
            List<Future<Integer>> futures = executorService.invokeAll(sumitTask(patientData,  100));
            for (Future<Integer> future : futures) {
                System.out.println("future.get = " + future.get());
            }
        } catch (InterruptedException | ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        executorService.shutdown();
        System.err.println("patiend count is " + patientData.size() + set.size());

    }

    public Set<Callable<Integer>> sumitTask(List<PatientData> data, int size) {
        Set<Callable<Integer>> callables = new HashSet<Callable<Integer>>();
        long totalSize = data.size();
        long batches = (totalSize + size - 1) / size; // Ceiling division


        for (int i = 0; i <batches; i++) {

            final int batchNumber = i; // For use in lambda
                System.err.println("Round submission " + i);
               
                callables.add(new Callable<Integer>() {
                    int startIndex = batchNumber * size;
                    int endIndex =(int) Math.min(startIndex + size, totalSize);
                    List<PatientData> ds = data.subList(startIndex,endIndex);
                    @Override
                    public Integer call() throws Exception {
                        // TODO Auto-generated method stub
                        return task(ds);

                    }

                });
            };
            
return callables;
        }

        
    

    public void sumitTasker(int size) {
        String sql = "SELECT external_id from public.patients";
        List<String> ids = serenityJdbcTemplate.queryForList(sql, String.class);
        System.err.println("ids are " + ids.size());
        List<PatientData> patientData = patientRepository.findTop5();// stream().filter(e ->
                                                                     // !ids.contains(e.getExternalId())).collect(Collectors.toList());
        System.err.println(patientData.size() + "----------------- size");
        System.err.println(patientData.stream().map(PatientData::getExternalId).toList());
        int rounds = patientData.size() / size;

        for (int i = 0; i <= rounds; i++) {
            if (i < rounds) {
                System.err.println("Round submission " + i);
                List<PatientData> ds = patientData.subList(i * size, (i * size) + size);
                try {
                    task2(ds);
                } catch (Exception e) {
                    e.printStackTrace();
                    for (PatientData d : ds) {
                        List<PatientData> f = new ArrayList<>();
                        f.add(d);
                        task2(f);

                    }

                }
            } else {
                System.err.println("Finishing Round submission " + i);
                List<PatientData> ds = patientData.subList((i * size), patientData.size());

                try {
                    task2(ds);
                } catch (Exception e) {

                    for (PatientData d : ds) {
                        List<PatientData> f = new ArrayList<>();
                        f.add(d);
                        task2(f);

                    }

                    e.printStackTrace();
                }

            }
            ;
        }

    }

    public void sumitTask2(int size) {
        String sql = "SELECT external_id from public.patients";
        List<String> ids = serenityJdbcTemplate.queryForList(sql, String.class);
        System.err.println("ids are " + ids.size());
        List<PatientData> patientData = patientRepository.findAll();

        int rounds = patientData.size() / size;

        for (int i = 0; i <= rounds; i++) {
            if (i < rounds) {
                System.err.println("Round submission " + i);
                List<PatientData> ds = patientData.subList(i * size, (i * size) + size);
                try {
                    task(ds);
                } catch (Exception e) {

                    e.printStackTrace();
                }
            } else {
                System.err.println("Finishing Round submission " + i);

                List<PatientData> ds = patientData.subList((i * size), patientData.size());
                task(ds);

            }
            ;
        }

    }

    public int task(List<PatientData> data) {
       
        String sql = "INSERT INTO public.patients(created_at, id,  \"uuid\", first_name, last_name, full_name, other_names, mobile, email, birth_date, gender, nationality, mr_number,  blood_type,  managing_organization_id, managing_organization_name,marital_status, name_prefix, occupation,  national_mobile_number, passport_number,  external_id, external_system) VALUES (CAST(? AS TIMESTAMP WITH TIME ZONE),nextval('patients_id_seq'::regclass),uuid(?),?,?,?,?,?,?,CAST(? AS DATE),?,?,?,?,CAST(? AS UUID),?,?,?,?,?,?,?,?)";
        serenityJdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {

            @SuppressWarnings("null")
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                PatientData k = data.get(i);
                try{
                ps.setString(1, k.getCreatedAt().split("T")[0] + " 00:00:00.000 +0000");
                }catch (Exception e){
                    ps.setString(1,LocalDate.now() + " 00:00:00.000 +0000");

                }
                ps.setString(2, k.getUuid());
                ps.setString(3, k.getFirstName());
                ps.setString(4, k.getLastName());
                ps.setString(5, k.getFullName() == null ? k.getFirstName() + " " + k.getLastName() : k.getLastName());
                ps.setString(6, k.getOtherNames() == null ? "" : k.getOtherNames());
                ps.setString(7, k.getMobile());
                ps.setString(8, k.getEmail());
                ps.setString(9, k.getBirthDate());
                ps.setString(10, k.getGender());
                ps.setString(11, k.getNationality());
                ps.setString(12, k.getMrNumber());
                ps.setString(13, k.getBloodType());
                ps.setString(14, k.getManagingOrganizationId());
                ps.setString(15, "Nyaho Medical Center");
                ps.setString(16, k.getMaritalStatus());
                ps.setString(17, k.getTitle());
                ps.setString(18, k.getOccupation());
                ps.setString(19, k.getNationalMobileNumber());
                ps.setString(20, k.getPassportNumber());
                ps.setString(21, k.getExternalId());
                ps.setString(22, k.getExternalSystem());

            }

            @Override
            public int getBatchSize() {
                // TODO Auto-generated method stub
                return data.size();
            }

        });
        return data.size();
    }

    public int task2(List<PatientData> data) {
        String sql = "INSERT INTO public.patients(created_at, id,  \"uuid\", first_name, last_name, full_name, other_names, mobile, email, birth_date, gender, nationality, mr_number,  blood_type,  managing_organization_id, managing_organization_name,marital_status, name_prefix, occupation,  national_mobile_number, passport_number,  external_id, external_system) VALUES (CAST(? AS TIMESTAMP WITH TIME ZONE),nextval('patients_id_seq'::regclass),uuid(?),?,?,?,?,?,?,CAST(? AS DATE),?,?,?,?,CAST(? AS UUID),?,?,?,?,?,?,?,?)";
        serenityJdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {

            @SuppressWarnings("null")
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                PatientData k = data.get(i);
                try {
                    ps.setString(1, k.getCreatedAt().split("T")[0] + " 00:00:00.000 +0000");
                } catch (Exception e) {
                    ps.setString(1, LocalDate.now().toString() + " 00:00:00.000 +0000");

                }
                ps.setString(2, k.getUuid());
                ps.setString(3, k.getFirstName());
                ps.setString(4, k.getLastName());
                ps.setString(5, k.getFullName());
                ps.setString(6, k.getOtherNames() == null ? "" : k.getOtherNames());
                ps.setString(7, k.getMobile());
                ps.setString(8, k.getEmail());
                ps.setString(9, k.getBirthDate());
                ps.setString(10, k.getGender());
                ps.setString(11, k.getNationality());
                ps.setString(12, k.getMrNumber());
                ps.setString(13, k.getBloodType());
                ps.setString(14, k.getManagingOrganizationId());
                ps.setString(15, "Nyaho Medical Center");
                ps.setString(16, k.getMaritalStatus());
                ps.setString(17, k.getTitle());
                ps.setString(18, k.getOccupation());
                ps.setString(19, k.getNationalMobileNumber());
                ps.setString(20, k.getPassportNumber());
                ps.setString(21, k.getExternalId());
                ps.setString(22, k.getExternalSystem());

            }

            @Override
            public int getBatchSize() {
                // TODO Auto-generated method stub
                return data.size();
            }

        });
        return data.size();

    }

    public void patientETL() {
        patientService.loadPatients();
        getPatientsThreads();

    }
}
