package com.serenity.integration.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.serenity.integration.models.PatientData;
@Repository
public interface PatientRepository extends JpaRepository<PatientData,Long>{
    @Query(value = "select * from patient_information pi2 where externalid   in ( 'NMC/EX/15/000003', 'NMC/EX/15/000004', 'NMC/EX/15/000005') ; ",nativeQuery = true)
    public List<PatientData> findTop5();

    public List<PatientData> findByExternalIdNotIn (List<String> mrnumbers);
    
    @Query(value = "select * from patient_information pi2 ",nativeQuery = true)
    public List<PatientData> findySystem ();

    public Optional<PatientData> findByExternalId(String hisNumber);
}
