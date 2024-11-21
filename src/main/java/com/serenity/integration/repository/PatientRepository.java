package com.serenity.integration.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.serenity.integration.models.PatientData;
@Repository
public interface PatientRepository extends JpaRepository<PatientData,Long>{
    @Query(value = "SELECT * FROM patient_information WHERE nationality ='GHANA' LIMIT 100",nativeQuery = true)
    public List<PatientData> findTop5();

}
