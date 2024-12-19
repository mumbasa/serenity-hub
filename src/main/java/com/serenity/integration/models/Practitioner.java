package com.serenity.integration.models;

import java.time.LocalDate;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Practitioner {
   private String countryCode;
    private String gender;
    private String title;
    private String firstName;
    private String lastName;
    private String dateOfBirth;
    private String email;
    private String mobile;
    private String postalAddress;
    private String homeAddress;
    private String hisId;
    private String empId;
    private String serenityId;
    private PractitionerRole practitionerRole;
    private List<String> practitionerSpecialty;
    private String teamMemberType;

    // Nested class for PractitionerRole
    public static class PractitionerRole {
        private String id;
        private String name;
        private Permissions permissions;
        private String organization;
        private boolean isCore;

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public Permissions getPermissions() { return permissions; }
        public void setPermissions(Permissions permissions) { this.permissions = permissions; }
        
        public String getOrganization() { return organization; }
        public void setOrganization(String organization) { this.organization = organization; }
        
        public boolean isCore() { return isCore; }
        public void setCore(boolean core) { isCore = core; }
    }

    // Nested class for Permissions
      public static class Permissions {
        private List<String> resources;
        private List<String> workspaces;

        // Getters and Setters
        public List<String> getResources() { return resources; }
        public void setResources(List<String> resources) { this.resources = resources; }
        
        public List<String> getWorkspaces() { return workspaces; }
        public void setWorkspaces(List<String> workspaces) { this.workspaces = workspaces; }
    }

    // Getters and Setters for Practitioner class
  
} 