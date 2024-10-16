package com.serenity.integration.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.serenity.integration.models.Encounter;

public interface EncounterRepository extends JpaRepository<Encounter,Long>{

}
