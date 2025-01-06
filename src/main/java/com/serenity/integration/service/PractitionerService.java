package com.serenity.integration.service;

import java.io.FileReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;
import org.springframework.web.client.RestTemplate;

import com.serenity.integration.models.Doctors;
import com.serenity.integration.models.Practitioner;
import com.serenity.integration.models.PractitionerResponse;
import com.serenity.integration.repository.DoctorRepository;

@Service
public class PractitionerService {

    @Autowired
    @Qualifier(value = "hisJdbcTemplate")
    JdbcTemplate hisJdbcTemplate;

    @Autowired
    DoctorRepository doctorRepository;

    @Autowired
    SetupService setupService;

    @Autowired
    @Qualifier("serenityJdbcTemplate")
    JdbcTemplate serenityJdbcTemplate;

    @Autowired
    @Qualifier(value = "legJdbcTemplate")
    JdbcTemplate legJdbcTemplate;

    public void saveHisPractioner() {
        List<Doctors> doctors = new ArrayList<>();
      Set<UUID> uuids = new HashSet<>();

        List<String> docsId= doctorRepository.findAll().stream().map(Doctors::getNationalMobileNumber).toList();
        String query = "SELECT * from employee_master";
        SqlRowSet set = hisJdbcTemplate.queryForRowSet(query);
        while (set.next()) {
            if(docsId.contains(set.getString("mobile"))){
            Doctors d = new Doctors();
            d.setExternalId(set.getString("Employee_ID"));
            d.setTitle(set.getString("title"));
            d.setMobile(PatientService.generateMobile(set.getString("mobile")));
            d.setHomeAddress(set.getString("house_no") + " " + (set.getString("locality")) + " " + (set.getString("city")));
            d.setDateOfBirth(set.getString("dob"));
            d.setEmail(set.getString("mobile")+"@nyahomedical.com");
            d.setExternalSystem("his");
            d.setFirstName(set.getString("name"));
            d.setPostalAddress(set.getString("street_name"));
            d.setNationalMobileNumber((set.getString("mobile")));
            d.setManagingOrganisation("161380e9-22d3-4627-a97f-0f918ce3e4a9");
            d.setManagingOrganisationId("Nyaho Medical Center");
            d.setSerenityUUid(PatientService.checkAndGenereateUUID(uuids, UUID.randomUUID()).toString());
            doctors.add(d);
            }else{
                Optional<Doctors> doctor = doctorRepository.NationalMobileNumber(set.getString("mobile"));
                if(doctor.isPresent()){
                    Doctors doc2 = doctor.get();
                    doc2.setCountryCode("+233");
                    doc2.setExternalId(set.getString("Employee_ID"));
                    doc2.setExternalSystem("his");
                    doc2.setDateOfBirth(set.getString("dob"));
                    doc2.setNationalMobileNumber((set.getString("mobile")));
                    doc2.setTitle(set.getString("title"));

                    doctors.add(doc2);
                }

            }
        }

        doctorRepository.saveAll(doctors);
    }

    public void getPractitioner() {
        List<Doctors> doctors = doctorRepository.findAll().stream().filter(e -> e.getSerenityId() == null).toList();
        for (Doctors doctor : doctors) {
            if (!doctor.getMobile().isBlank() | doctor.getMobile().length() > 6 | doctor.getMobile() != null) {
                doctor.setEmail(doctor.getMobile() + "@nyahomedical.com");
                doctor.setMobile(doctor.getMobile().replaceFirst("0", "+233"));
            } else {
                doctor.setEmail(doctor.getFirstName() + "@nyahomedical.com");
                Random rand = new Random();
                int ge = rand.nextInt(999999999);
                String numGen = String.format("%09d", ge);
                doctor.setMobile("+233" + numGen);
            }

        }

        Map<String, Doctors> map = insertCSVData();
        int count = 1;
        for (Doctors doctor : doctors) {
            if (map.containsKey(doctor.getMobile())) {
                doctor.setSerenityId(map.get(doctor.getMobile()).getSerenityId());
                doctor.setSerenityUUid(map.get(doctor.getMobile()).getSerenityUUid());
                doctor.setEmail(map.get(doctor.getMobile()).getEmail());
                doctor.setDateOfBirth(map.get(doctor.getMobile()).getDateOfBirth());
                doctorRepository.save(doctor);
                System.err.println("Found ");
                count++;
            }

        }

        System.err.println("Found entries :" + count);

    }

    public String getPractitioner1() throws UnsupportedEncodingException {
        // LOGGER.info("Searching for "+stock.getFullName());
        String url = "https://staging.nyaho.serenity.health/v1/providers/161380e9-22d3-4627-a97f-0f918ce3e4a9"
                + "/practitioners?search=" + URLEncoder.encode("bry_lar@yaho.com", "UTF-8");
        System.err.println(url);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/json");
        headers.add("Authorization", "Bearer " + setupService.getToken().getAccess());
        headers.add("PROVIDER-PORTAL-ID", "j&4P8F<6+dF7/HASJ^hI92/6a&jdJOj*O\"[pHsh}t{o\"&7]\"}1~wg&SI%--,h{/");
        HttpEntity<String> httpEntity = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, httpEntity,
                String.class);
        // System.err.println(response.getBody().getData());
        return (response.getBody());
    }

    public PractitionerResponse addPractioner(String orgId, String practitioner, Doctors doc) {
        // LOGGER.info("Searching for "+stock.getFullName());
        String url = "https://staging.nyaho.serenity.health/v1/providers/" + orgId
                + "/practitioners";
        System.err.println(url);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.add("Authorization", "Bearer " + setupService.getToken().getAccess());
        headers.add("PROVIDER-PORTAL-ID", "j&4P8F<6+dF7/HASJ^hI92/6a&jdJOj*O\"[pHsh}t{o\"&7]\"}1~wg&SI%--,h{/");
        HttpEntity<String> httpEntity = new HttpEntity<>(practitioner, headers);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<PractitionerResponse> response = restTemplate.exchange(url, HttpMethod.POST, httpEntity,
                PractitionerResponse.class);

        if (response.getBody().isSuccess()) {
            System.err.println("Serenity id =" + response.getBody().getData().getUuid());
            doc.setSerenityId(response.getBody().getData().getId());
            doc.setSerenityUUid(response.getBody().getData().getUuid());
            doctorRepository.save(doc);
        }
        return (response.getBody());
    }

    public void migratePractitioner() {
        List<Doctors> doctors = doctorRepository.findAll().stream().filter(e -> e.getSerenityId() == null).toList();
        for (Doctors doctor : doctors) {
            if (!doctor.getMobile().isBlank() | doctor.getMobile().length() > 6 | doctor.getMobile() != null) {
                doctor.setEmail(doctor.getMobile() + "@nyahomedical.com");
                doctor.setMobile(doctor.getMobile().replaceFirst("0", "+233"));
            } else {
                doctor.setEmail(doctor.getFirstName() + "@nyahomedical.com");
                Random rand = new Random();
                int ge = rand.nextInt(999999999);
                String numGen = String.format("%09d", ge);
                doctor.setMobile("+233" + numGen);
            }

        }
        // System.err.println("Doctors are " + doctors.stream().filter(e ->
        // !e.getEmail().isBlank()).toList().size());

        for (Doctors doctor : doctors) {
            try {
                addPractioner("161380e9-22d3-4627-a97f-0f918ce3e4a9",
                        dummyPay(doctor.getFirstName(), doctor.getMobile(), doctor.getEmail(), doctor.getTitle(),
                                doctor.getDateOfBirth()),
                        doctor);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

    }

    public String dummyPay(String name, String telephone, String email, String title, String dob) {
        String[] splitNames = name.split(" ");
        String lastname;
        String firstname;
        System.err.println(splitNames.length);

        if (splitNames.length == 1) {
            lastname = name;
            firstname = name;
            // throw new ArrayIndexOutOfBoundsException();
        } else {
            firstname = splitNames[0];
            lastname = splitNames[splitNames.length - 1];
        }

        String a = "{" +
                "    \"country_code\": \"+233\",\n" + //
                "    \"gender\": \"MALE\",\n" + //
                "    \"title\": \"" + title.replaceAll("\\.", "") + "\",\n" + //
                "    \"first_name\": \"" + firstname + "\",\n" + //
                "    \"last_name\": \"" + lastname + "\",\n" + //
                "    \"date_of_birth\": \"" + dob + "\",\n" + //
                "    \"email\": \"" + email + "\",\n" + //
                "    \"mobile\": \"" + telephone + "\",\n" + //
                "    \"postal_address\": \"somewhere\",\n" + //
                "    \"home_address\": \"somewhere\",\n" + //
                "    \"practitioner_role\": {\n" + //
                "        \"id\": \"ADMIN\",\n" + //
                "        \"name\": \"Admin\",\n" + //
                "        \"permissions\": {\n" + //
                "            \"resources\": [\n" + //
                "                \"*.*\",\n" + //
                "                \"patient.*\",\n" + //
                "                \"Appointments.*\",\n" + //
                "                \"Diagnostic.requests.*\",\n" + //
                "                \"Diagnostic.samples.*\",\n" + //
                "                \"Diagnostic.reports.*\",\n" + //
                "                \"Medication.orders.*\",\n" + //
                "                \"Medication.dispense.*\",\n" + //
                "                \"Encounters.*\",\n" + //
                "                \"Vitals.*\",\n" + //
                "                \"Bills.*\",\n" + //
                "                \"Bills.acceptCash.write\",\n" + //
                "                \"Bills.acceptCash.read\",\n" + //
                "                \"Bills.approveCredit.read\",\n" + //
                "                \"Procedures.*\",\n" + //
                "                \"Medication.immunizations.*\",\n" + //
                "                \"Practioners.*\",\n" + //
                "                \"Bills.acceptUserAccount.write\",\n" + //
                "                \"Bills.acceptCorporate.write\",\n" + //
                "                \"Bills.acceptInsurance.write\",\n" + //
                "                \"Diagnostic.deviceResults.read\",\n" + //
                "                \"Encounters.pastEncounters.write\",\n" + //
                "                \"Inventory.read\",\n" + //
                "                \"Inventory.write\",\n" + //
                "                \"Bills.requestCancelation.write\",\n" + //
                "                \"Bills.approveCancelation.write\",\n" + //
                "                \"Bills.finishCancelation.write\",\n" + //
                "                \"Diagnostic.devices.*\",\n" + //
                "                \"Services.read\",\n" + //
                "                \"Services.write\",\n" + //
                "                \"Beds.read\",\n" + //
                "                \"Beds.write\",\n" + //
                "                \"Rooms.read\",\n" + //
                "                \"Rooms.write\",\n" + //
                "                \"Admissions.read\",\n" + //
                "                \"Admissions.write\",\n" + //
                "                \"Discharge.write\",\n" + //
                "                \"DischargeAuthorization.write\",\n" + //
                "                \"Bills.acceptPatientAccount.write\",\n" + //
                "                \"PatientAccount.debtLimit.write\",\n" + //
                "                \"PatientAccount.deposit.write\",\n" + //
                "                \"PatientAccount.withdraw.write\",\n" + //
                "                \"DoctorProgressNotes.write\"\n" + //
                "            ],\n" + //
                "            \"workspaces\": [\n" + //
                "                \"BILL.*\",\n" + //
                "                \"RECEPT.*\",\n" + //
                "                \"OPD.*\",\n" + //
                "                \"IPD.*\",\n" + //
                "                \"PHARM.*\",\n" + //
                "                \"VIRT.*\",\n" + //
                "                \"DIAG.*\",\n" + //
                "                \"HOME.*\",\n" + //
                "                \"EMERG.*\",\n" + //
                "                \"MOM.*\",\n" + //
                "                \"ADMIN.*\"\n" + //
                "            ]\n" + //
                "        },\n" + //
                "        \"organization\": \"161380e9-22d3-4627-a97f-0f918ce3e4a9\",\n" + //
                "        \"is_core\": true\n" + //
                "    },\n" + //
                "    \"practitioner_specialty\": [\n" + //
                "        \"Non_clinical_staff\"\n" + //
                "    ],\n" + //
                "    \"team_member_type\": \"clinical_staff\"\n" + //
                "}";
        return a;
    }

    public String payloadCreate(Doctors doctor) {
        String load = "{" +
                "    \"country_code\": \"+233\",\n" + //
                "    \"gender\": \"MALE\",\n" + //
                "    \"title\": \"" + doctor.getTitle().replaceAll("\\.", "") + "\",\n" + //
                "    \"first_name\": \"" + doctor.getFirstName() + "\",\n" + //
                "    \"last_name\": \"NA\",\n" + //
                "    \"date_of_birth\": \"" + doctor.getDateOfBirth() + "\",\n" + //
                "    \"email\": \"" + doctor.getEmail() + "\",\n" + //
                "    \"mobile\": \"" + doctor.getMobile().replaceFirst("0", "+233").substring(0, 13) + "\",\n" + //
                "    \"postal_address\": \"somewhere\",\n" + //
                "    \"home_address\": \"" + doctor.getHomeAddress() + "Accra\",\n" + //
                "    \"practitioner_role\": {\n" + //
                "        \"id\": \"ADMIN\",\n" + //
                "        \"name\": \"Admin\",\n" + //
                "        \"permissions\": {\n" + //
                "            \"resources\": [\n" + //
                "                \"*.*\",\n" + //
                "                \"patient.*\",\n" + //
                "                \"Appointments.*\",\n" + //
                "                \"Diagnostic.requests.*\",\n" + //
                "                \"Diagnostic.samples.*\",\n" + //
                "                \"Diagnostic.reports.*\",\n" + //
                "                \"Medication.orders.*\",\n" + //
                "                \"Medication.dispense.*\",\n" + //
                "                \"Encounters.*\",\n" + //
                "                \"Vitals.*\",\n" + //
                "                \"Bills.*\",\n" + //
                "                \"Bills.acceptCash.write\",\n" + //
                "                \"Bills.acceptCash.read\",\n" + //
                "                \"Bills.approveCredit.read\",\n" + //
                "                \"Procedures.*\",\n" + //
                "                \"Medication.immunizations.*\",\n" + //
                "                \"Practioners.*\",\n" + //
                "                \"Bills.acceptUserAccount.write\",\n" + //
                "                \"Bills.acceptCorporate.write\",\n" + //
                "                \"Bills.acceptInsurance.write\",\n" + //
                "                \"Diagnostic.deviceResults.read\",\n" + //
                "                \"Encounters.pastEncounters.write\",\n" + //
                "                \"Inventory.read\",\n" + //
                "                \"Inventory.write\",\n" + //
                "                \"Bills.requestCancelation.write\",\n" + //
                "                \"Bills.approveCancelation.write\",\n" + //
                "                \"Bills.finishCancelation.write\",\n" + //
                "                \"Diagnostic.devices.*\",\n" + //
                "                \"Services.read\",\n" + //
                "                \"Services.write\",\n" + //
                "                \"Beds.read\",\n" + //
                "                \"Beds.write\",\n" + //
                "                \"Rooms.read\",\n" + //
                "                \"Rooms.write\",\n" + //
                "                \"Admissions.read\",\n" + //
                "                \"Admissions.write\",\n" + //
                "                \"Discharge.write\",\n" + //
                "                \"DischargeAuthorization.write\",\n" + //
                "                \"Bills.acceptPatientAccount.write\",\n" + //
                "                \"PatientAccount.debtLimit.write\",\n" + //
                "                \"PatientAccount.deposit.write\",\n" + //
                "                \"PatientAccount.withdraw.write\",\n" + //
                "                \"DoctorProgressNotes.write\"\n" + //
                "            ],\n" + //
                "            \"workspaces\": [\n" + //
                "                \"BILL.*\",\n" + //
                "                \"RECEPT.*\",\n" + //
                "                \"OPD.*\",\n" + //
                "                \"IPD.*\",\n" + //
                "                \"PHARM.*\",\n" + //
                "                \"VIRT.*\",\n" + //
                "                \"DIAG.*\",\n" + //
                "                \"HOME.*\",\n" + //
                "                \"EMERG.*\",\n" + //
                "                \"MOM.*\",\n" + //
                "                \"ADMIN.*\"\n" + //
                "            ]\n" + //
                "        },\n" + //
                "        \"organization\": \"161380e9-22d3-4627-a97f-0f918ce3e4a9\",\n" + //
                "        \"is_core\": true\n" + //
                "    },\n" + //
                "    \"practitioner_specialty\": [\n" + //
                "        \"Non_clinical_staff\"\n" + //
                "    ],\n" + //
                "    \"team_member_type\": \"clinical_staff\"\n" + //
                "}";
        return load;

    }

    public Map<String, Doctors> insertCSVData() {
        List<Doctors> practitioners = new ArrayList<>();
        Map<String, Doctors> map = new HashMap<>();
        String[] firstElements = {
                "UUID",
                "Created At",
                "Modified At",
                "ID",
                "Address",
                "Birth Date",
                "Gender",
                "Is Active",
                "Is Deleted",
                "Period Start",
                "Period End",
                "Other Names",
                "Photo",
                "Postal Code",
                "Title",
                "Practitioner Type",
                "Signature",
                "Specialty",
                "Managing Organization ID",
                "Practitioner ID",
                "Role ID",
                "User ID",
                "First Name",
                "Last Name",
                "Email",
                "Mobile",
                "Full Name"
        };

        try (Reader in = new FileReader(ResourceUtils.getFile("classpath:pract.csv"))) {
            CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                    .setHeader(firstElements)
                    .setDelimiter("\t")
                    .build();

            Iterable<CSVRecord> records = csvFormat.parse(in);
            for (CSVRecord record : records) {
                Doctors d = new Doctors();
                d.setDateOfBirth(record.get("Birth Date"));
                d.setFirstName(record.get("First Name"));
                d.setLastName(record.get("Last Name"));
                d.setSerenityUUid(record.get("UUID"));
                d.setSerenityId(record.get("ID"));
                d.setMobile(record.get("Mobile"));
                d.setEmail(record.get("Email"));
                d.setGender(record.get("Gender"));
                practitioners.add(d);
            }

            map = practitioners.stream().collect(Collectors.toMap(Doctors::getMobile, address -> address,
                    (ad1, ad2) -> ad1));

        } catch (Exception e) {

        }
        System.err.println(map.values());
        return map;
    }

    public void addSerenityPractitioner() {
        List<Doctors> doctors = new ArrayList<>();
        String sql = "SELECT * FROM practitioners";
        SqlRowSet set = serenityJdbcTemplate.queryForRowSet(sql);
        while (set.next()) {
            Doctors doc = new Doctors();
            doc.setCreatedAt(set.getString("created_at"));
            doc.setSerenityUUid(set.getString("uuid"));
            doc.setEmail(set.getString("email"));
            doc.setMobile(set.getString("mobile"));
            doc.setFirstName(set.getString("first_name"));
            doc.setLastName(set.getString("last_name"));
            doc.setGender(set.getString("gender"));
            doc.setFullName(set.getString("full_name"));
            doc.setManagingOrganisation(set.getString("managing_organization_name"));
            doc.setManagingOrganisationId(set.getString("managing_organization_id"));
            doc.setNationalMobileNumber(set.getString("national_mobile_number"));
            doctors.add(doc);

        }

        doctorRepository.saveAll(doctors);

    }


    public void savePracttioner(){
      
       addSerenityPractitioner();
        //saveHisPractioner();
       

    }
}
