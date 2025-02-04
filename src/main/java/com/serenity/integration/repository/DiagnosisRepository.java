package com.serenity.integration.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.serenity.integration.models.Diagnosis;
import com.serenity.integration.models.Encounter;

public interface DiagnosisRepository extends JpaRepository<Diagnosis,Long>{

       @Query(value = "SELECT * FROM diagnosis where substring(visitid , 1 , 2) != 'LL' order by id OFFSET ?  LIMIT 1000 ",nativeQuery = true)
    List<Diagnosis> getfirst100k(int offset);
    @Query(value = "SELECT * FROM diagnosis where system ='opd' order by id OFFSET ?  LIMIT 1000 ",nativeQuery = true)
    List<Diagnosis> findBySystemLimit(int offset);


}
