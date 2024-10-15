package com.serenity.integration.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.serenity.integration.models.EncounterNote;

@Repository
public interface EncounterNoteRepository extends  JpaRepository<EncounterNote, String>{



}
