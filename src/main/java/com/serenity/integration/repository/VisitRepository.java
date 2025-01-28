package com.serenity.integration.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.serenity.integration.models.Visits;

@Repository
public interface VisitRepository extends JpaRepository<Visits,Long>{
    @Query(value = "SELECT * FROM visits OFFSET ?1  LIMIT ?2",nativeQuery = true)
    List<Visits> getfirst100k(int offset,int limit);
  
    @Query(value = "SELECT * FROM visits limit 1",nativeQuery = true)
    List<Visits> getfirst1();

    public int countByEncounterClass(String encounterClass);
    public int countByExternalSystem(String externalSystem);
    public List<Visits>  findByEncounterClass(String encounterClass);
    public Optional<Visits>  findByExternalId(String encounterClass);



    @Query(value = "select * from visits where externalsystem='opd' order by id  OFFSET ?1  LIMIT 1000",nativeQuery = true)
    List<Visits> getfirst100k(int offset);

    @Query(value = "select * from visits where visits.encounterclass='ambulatory' order by id  OFFSET ?1  LIMIT 100",nativeQuery = true)
    List<Visits> getfirstAmbul10k(int offset);

 
    @Query(value = "select * from visits where createdat =?1 and hisnumber =?2 and assignedtoid =?3",nativeQuery = true)
    Visits getVistByDateDoctorPatient(String date,String patient,String doctor);
}
