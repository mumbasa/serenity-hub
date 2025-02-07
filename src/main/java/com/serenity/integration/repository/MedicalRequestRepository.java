package com.serenity.integration.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.serenity.integration.models.EncounterNote;
import com.serenity.integration.models.MedicalRequest;

@Repository
public interface MedicalRequestRepository extends JpaRepository<MedicalRequest,Long>{
    @Query(value = "select * from medicalrequest where externalsystem='his' and practitionerid is not null and visitid is not null and patientid  is not null and encounterid is not  null order by id  OFFSET ?1 LIMIT 1000",nativeQuery = true)
    List<MedicalRequest> findOffset(int offset);

    @Query(value = "select * from medicalrequest where  practitionerid is not null and visitid is not null and patientid  is not null and encounterid is not  null  ORDER BY id OFFSET ?1 LIMIT 1000",nativeQuery = true)
    List<MedicalRequest> findByExternalSystem(int offset);
    
    @Query(value = "select count(*) from medicalrequest where externalsystem=?1  and practitionerid is not null and visitid is not null and patientid  is not null and encounterid is not  null",nativeQuery = true)
    int findByCountSystem(String system);


}
