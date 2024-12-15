package com.serenity.integration.service;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Service;

import com.serenity.integration.models.PatientData;
import com.serenity.integration.repository.PatientRepository;

@Service
public class PatientMigrationService {

    @Autowired
    @Qualifier("serenityJdbcTemplate")
    JdbcTemplate serenityJdbcTemplate;

    @Autowired
    PatientRepository patientRepository;



    public void getPatients(){
String sql = "SELECT external_id from public.patients";
List<String> set = serenityJdbcTemplate.queryForList(sql,String.class);
String result = "("+set.stream().collect(Collectors.joining("','", "'", "'"))+")";
List<PatientData> patientData = patientRepository.findAll();



System.err.println("patiend count is "+patientData.size() +set.size());


    
}


public void task(List<PatientData> data,List<String> id){
 List<PatientData>  datas= data.stream().filter(e-> !id.contains(e.getExternalId())).collect(Collectors.toList());
   String sql= "INSERT INTO patients() VALUES (?,?,?,?)";
    serenityJdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {

        @Override
        public void setValues(PreparedStatement ps, int i) throws SQLException {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'setValues'");
        }

        @Override
        public int getBatchSize() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'getBatchSize'");
        }
        
    });

}
}
