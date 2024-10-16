package com.serenity.integration.configuration;

import org.springframework.data.jpa.domain.Specification;

import com.serenity.integration.models.PatientData;

public class PatientSpecification {

    public static Specification<PatientData> mobileLike(String mobile) {
        return (root, query, builder) -> builder.like(root.get("mobile"), "%" + mobile + "%");
    }

    public static Specification<PatientData> mrNumberLike(String mrNumber) {
        return (root, query, builder)-> builder.like(root.get("mrNumber"), "%" + mrNumber + "%");
    }


    public static Specification<PatientData> gender(String gender) {
        return (root, query, builder)-> builder.equal(root.get("gender"), gender);
    }

    public static Specification<PatientData> filterDateOfBirth(String dob) {
        return (root, query, builder)-> builder.equal(root.get("birthDate"), dob);
    }
}

