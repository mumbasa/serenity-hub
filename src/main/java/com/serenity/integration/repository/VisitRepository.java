package com.serenity.integration.repository;

import java.util.List;
import java.util.Map;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.serenity.integration.models.Visits;

@Repository
public interface VisitRepository extends JpaRepository<Visits,Long>{
    @Query(value = "SELECT * FROM visits OFFSET ?1  LIMIT ?2",nativeQuery = true)
    List<Visits> getfirst100k(int offset,int limit);
  
    @Query(value = "select * from visits where createdat =?1 and hisnumber =?3 and assignedtoid =?3",nativeQuery = true)
    Visits getVistByDateDoctorPatient(String date,String doctor,String patient);
}
