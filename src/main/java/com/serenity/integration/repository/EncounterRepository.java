package com.serenity.integration.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.serenity.integration.models.Encounter;
@Repository
public interface EncounterRepository extends JpaRepository<Encounter,Long>{

}
