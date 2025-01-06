package com.serenity.integration.service;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;
import org.springframework.web.client.RestTemplate;

import com.google.gson.Gson;
import com.serenity.integration.models.AvailableTime;
import com.serenity.integration.models.Category;
import com.serenity.integration.models.CustomerGroup;
import com.serenity.integration.models.CustomerGroupResponse;
import com.serenity.integration.models.Healthcare;
import com.serenity.integration.models.HealthcareService;
import com.serenity.integration.models.HealthcareServiceResponse;
import com.serenity.integration.models.PriceTier;
import com.serenity.integration.models.Report;
import com.serenity.integration.models.ServiceData;
import com.serenity.integration.setup.Location;
import com.serenity.integration.repository.ReportRepo;
import com.serenity.integration.repository.ServiceDataRepo;
import com.serenity.integration.models.ServicePriceResponse;
import com.serenity.integration.models.ServicePricing;
import com.serenity.integration.models.ServiceType;
import com.serenity.integration.models.Specialty;
import com.serenity.integration.models.User;
import com.serenity.integration.models.V1Response;

@Service
public class SetupService {
    @Value("${access.token}")
    String token;

    @Autowired
    ServiceDataRepo serviceDataRepo;

    @Autowired
    ReportRepo reportRepo;
    Logger LOGGER = LoggerFactory.getLogger("ME");

    public static void main(String[] args) {
        Integer[] data ={0,1,2,3,4,5,6,7,8,9,10,11,12,13,15,16,17,18,19};
        List<Integer> das = Arrays.asList(data);
        int as = (das.size()/3);
        System.err.println(as);
        int rounds=0;
        for(int a=0;a<=as;a++){
            if(rounds<as){
                System.err.println(true);
            System.err.println(das.subList(a*3, (a*3)+3));
            }else{

                System.err.println(false);

                System.err.println(das.subList(rounds*3, das.size()));

            }
            rounds++;

        }


    }

    public ResponseEntity<CustomerGroupResponse> migrate(String organisationId) {
        ResponseEntity<CustomerGroupResponse> response = null;
        String[] paymentTypes = {
                "Local cash",
                "Foreign cash",
                "Local credit",
                "Foreign credit",
                "Local insurance",
                "Foreign insurance",
                "Foreign Corporate",
                "Local Corporate"
        };

        List<String> keys = getCustomerGroups(organisationId).stream().map(CustomerGroup::getName).toList();
        HashSet<String> names = new HashSet(keys);
        for (String type : paymentTypes) {

            if (!names.contains(type)) {
                LOGGER.info("fount new " + type);
                CustomerGroup group = new CustomerGroup();
                group.setActive(true);
                group.setCreatedById("8aaf05f8-741e-4e66-86df-a595f981d963");
                group.setCreatedByName("Rejoice Hormeku");
                group.setManagingOrganization("161380e9-22d3-4627-a97f-0f918ce3e4a9");
                group.setName(type);
                group.setUuid(UUID.randomUUID().toString());
                String url = "https://stag.api.cloud.serenity.health/v2/billing/customer-groups";

                HttpHeaders headers = new HttpHeaders();
                headers.set("Content-Type", "application/json");
                headers.add("x-api-key", "efomrddi");
                HttpEntity<CustomerGroup> httpEntity = new HttpEntity<>(group, headers);
                RestTemplate restTemplate = new RestTemplate();
                response = restTemplate.exchange(url, HttpMethod.POST, httpEntity,
                        CustomerGroupResponse.class);
                LOGGER.info(response.getBody().toString());
            }
        }
        return response;
    }

    public List<CustomerGroup> getCustomerGroups(String organisationId) {

        // LOGGER.info("Searching for "+stock.getFullName());
        String url = "https://stag.api.cloud.serenity.health/v2/billing/customer-groups?managing_organization="
                + organisationId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.add("x-api-key", "efomrddi");
        HttpEntity<CustomerGroupResponse> httpEntity = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<CustomerGroupResponse> response = restTemplate.exchange(url, HttpMethod.GET, httpEntity,
                CustomerGroupResponse.class);
        LOGGER.info(response.getBody().getData().toString());
        return response.getBody().getData();
    }

    public List<ServicePricing> getServicePrice(String orgId) {
        List<ServicePricing> prices =  new ArrayList<>();
        // LOGGER.info("Searching for "+stock.getFullName());
        String url = "https://stag.api.cloud.serenity.health/v2/billing/service-prices?managing_organization=" + orgId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("   Content-Type", "application/json");
        headers.add("x-api-key", "efomrddi");
        HttpEntity<String> httpEntity = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<ServicePriceResponse> response = restTemplate.exchange(url, HttpMethod.GET, httpEntity,
                ServicePriceResponse.class);

        int total = response.getBody().getTotal();
        int rounds = 100/50;
        for(int i=1;i<=(rounds+1);i++){
            System.err.println("goint for round "+i);
            response = restTemplate.exchange(url+"&page="+i, HttpMethod.GET, httpEntity,
            ServicePriceResponse.class);
            System.err.println(response.getBody().getData());
            prices.addAll(response.getBody().getData());

        }
     
        return prices;
    }


    public ServicePricing getServicePrices(int id) {
        // LOGGER.info("Searching for "+stock.getFullName());
        String url = "https://stag.api.cloud.serenity.health/v2/billing/service-prices/" + id;

        HttpHeaders headers = new HttpHeaders();
        headers.set("   Content-Type", "application/json");
        headers.add("x-api-key", "efomrddi");
        HttpEntity<String> httpEntity = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<ServicePricing> response = restTemplate.exchange(url, HttpMethod.GET, httpEntity,
        ServicePricing.class);
System.err.println(response.getBody());
        
        return response.getBody();
    }






    
    public List<Healthcare> getHealthService(String orgId) {
        // LOGGER.info("Searching for "+stock.getFullName());
        String url = "https://staging.nyaho.serenity.health/v1/providers/" + orgId
                + "/administration/healthcareservices";
        System.err.println(url);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.add("Authorization", "Bearer " + getToken().getAccess());
        headers.add("PROVIDER-PORTAL-ID", "j&4P8F<6+dF7/HASJ^hI92/6a&jdJOj*O\"[pHsh}t{o\"&7]\"}1~wg&SI%--,h{/");
        HttpEntity<String> httpEntity = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<HealthcareServiceResponse> response = restTemplate.exchange(url, HttpMethod.GET, httpEntity,
                HealthcareServiceResponse.class);
        // System.err.println(response.getBody().getData());
        return (response.getBody().getData());
    }

    public V1Response getToken() {
        User user = new User();
        user.setEmail("chris@clearspacelabs.com");
        user.setPassword("charlehaschanged");
        Gson g = new Gson();
        String data = g.toJson(user);
        System.err.println(data);
        String url = "https://staging.nyaho.serenity.health/v1/providers/auth/login";
        System.err.println(url);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/json");
        headers.add("Authorization", "Bearer " + token);
        headers.add("PROVIDER-PORTAL-ID", "j&4P8F<6+dF7/HASJ^hI92/6a&jdJOj*O\"[pHsh}t{o\"&7]\"}1~wg&SI%--,h{/");
        HttpEntity<String> httpEntity = new HttpEntity<>(data, headers);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<V1Response> response = restTemplate.exchange(url, HttpMethod.POST, httpEntity,
                V1Response.class);
        // System.err.println(response.getBody().getData());
        return (response.getBody());
    }

    public void addHealthService(String orgId, String c) {
        LOGGER.info("Searching for ");
        String url = "https://staging.nyaho.serenity.health/v1/providers/" + orgId
                + "/administration/healthcareservices";
        System.err.println(url);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.add("Authorization", "Bearer " + token);
        headers.add("PROVIDER-PORTAL-ID", "j&4P8F<6+dF7/HASJ^hI92/6a&jdJOj*O\"[pHsh}t{o\"&7]\"}1~wg&SI%--,h{/");
        HttpEntity<String> httpEntity = new HttpEntity<>(c, headers);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<HealthcareServiceResponse> response = restTemplate.exchange(url, HttpMethod.POST, httpEntity,
                HealthcareServiceResponse.class);
        System.err.println(response.getBody());
        // return (response.getBody().getData());
    }

   
    public void setPricing(String orgId, String v1Provider) {
          int count=1;
        List<CustomerGroup> groups = getCustomerGroups(orgId);
        Map<String, CustomerGroup> groupMap = groups.stream().collect(Collectors.toMap(e -> e.getName(), e -> e));
        Map<String, ServicePricing> pricings = getServicePrice(orgId).stream()
                .collect(Collectors.toMap(e -> e.getName(), e -> e));

              System.err.println(pricings.keySet().size());
        Map<String, Healthcare> healthMap = getHealthService(v1Provider).stream()
                .collect(Collectors.toMap(e -> e.getHealthcareServiceName(), e -> e));

        String[] firstElements = { "ServiceName", "HealthcareService", "PriceType", "Currency", "ServiceCharge",
                "basePrice", "groupName" };

        try (Reader in = new FileReader(ResourceUtils.getFile("classpath:pricing.csv"))) {
            CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                    .setHeader(firstElements)
                    .setDelimiter("\t")
                    .build();

            Iterable<CSVRecord> records = csvFormat.parse(in);
            for (CSVRecord record : records) {
              

                    ServicePricing servicePricing = new ServicePricing(record);
                    servicePricing.setManagingOrganization(orgId);
                    if (groupMap.containsKey(servicePricing.getCustomerGroupName())) {
                        servicePricing
                                .setCustomerGroupId(groupMap.get(servicePricing.getCustomerGroupName()).getUuid());

                    }
                    if (healthMap.containsKey(servicePricing.getHealthcareServiceName())) {
                        servicePricing
                                .setHealthcareServiceId(healthMap.get(servicePricing.getHealthcareServiceName()).getId());

                    }

                    if (servicePricing.getHealthcareServiceId() != null) {
                      
                        try{
                            if (!pricings.containsKey(record.get(0))) {
                                System.err.println("saving prices "+(count++));

                      //  savePricing(servicePricing);
                            }
                            else {
                               ServicePricing p = pricings.get(record.get(0));
                               p.setAmount(record.get("basePrice"));
                                System.err.println("already exit prices");

                              //  LOGGER.info("already exist");
                               updatePricing(servicePricing);
                            }
                        }catch(Exception e){
                            System.err.println(e.getMessage());
                            Gson k = new Gson();
                           // System.err.println("error" +k.toJson(servicePricing));
                        }
                    }

                
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();

        }

    }

    public void healthServiceSetup(String org, String file) {
        String[] firstElements = { "ServiceName", "ServiceClass", "BasePrice", "ServiceRequestCategory",
                "DiagnosticOrderCode", "DiagnosticServiceSection", "RevenueTagDisplay", "ServiceSpecialty",
                "ServiceType" };

        HashSet<String> dasta = new HashSet<>(
                getHealthService(org).stream().map(Healthcare::getHealthcareServiceName).collect(Collectors.toSet()));

        List<ServiceData> services = new ArrayList<>();
        try (Reader in = new FileReader(ResourceUtils.getFile("classpath:" + file))) {
            CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                    .setHeader(firstElements)
                    .setDelimiter("\t")
                    .build();

            Iterable<CSVRecord> records = csvFormat.parse(in);
            for (CSVRecord record : records) {
                if (!dasta.contains(record.get("ServiceName"))) {

                    if (!record.get(1).strip().equalsIgnoreCase("Diagnostic")
                            & !record.get(1).strip().equalsIgnoreCase("Hospitalization")) {
                        HealthcareService service = new HealthcareService();
                        service.setHealthcareServiceName(record.get("ServiceName"));
                        Category category = new Category();
                        category.setCode(record.get(1).strip());
                        List<Category> cats = new ArrayList<>();
                        cats.add(category);
                        service.setHealthcareServiceCategories(cats);

                        Specialty specialty = new Specialty();
                        specialty.setCode(record.get("ServiceSpecialty"));
                        List<Specialty> specs = new ArrayList<>();
                        specs.add(specialty);
                        service.setHealthcareServiceSpecialties(specs);

                        ServiceType serviceType = new ServiceType();
                        serviceType.setCode(record.get("ServiceType"));
                        List<ServiceType> serviceTypes = new ArrayList<>();
                        serviceTypes.add(serviceType);
                        service.setHealthcareServiceTypes(serviceTypes);
                        service.setProvider("161380e9-22d3-4627-a97f-0f918ce3e4a9");

                        service.setSlotDuration("30");
                        service.setVirtual_service(false);
                        service.setComment("Healthcare service for General Consultation at Nyaho Medical Center");
                        service.setHealthcareServiceAppointmentRequired(false);
                        service.setHealthcareServiceServiceProvisionCode("cost");
                        service.setExtraDetails(record.get("BasePrice"));
                        service.setRevenueDisplayTag(record.get("RevenueDisplayTag"));

                        PriceTier free = new PriceTier();
                        free.setCharge("0");
                        free.setDisplay("Free");
                        free.setCurrency("GHS");
                        free.setDescription("Free service for " + service.getHealthcareServiceName());
                        free.setPriority("routine");

                        PriceTier priceTier = new PriceTier();
                        priceTier.setCharge(record.get("BasePrice"));
                        priceTier.setCurrency("GHS");
                        priceTier.setDisplay("Express");
                        priceTier.setPriority("urgent");
                        priceTier.setDisplay("Express");
                        priceTier.setDescription("Express service for " + record.get("ServiceName"));

                        PriceTier standard = new PriceTier();
                        standard.setCharge("50");
                        standard.setDisplay("Standard");
                        standard.setCurrency("GHS");
                        standard.setDescription("Standard service for " + record.get("ServiceName"));
                        standard.setPriority("routine");

                        List<PriceTier> priceTiers = new ArrayList<>();
                        priceTiers.add(priceTier);
                        priceTiers.add(free);
                        priceTiers.add(standard);
                        service.setPriceTiers(priceTiers);
                        ServiceData d = new ServiceData();
                        Gson gson = new Gson();
                        String json = gson.toJson(service);
                        // System.err.println(json);
                        d.setServiceName(service.getHealthcareServiceName());
                        d.setData(json);

                        services.add(d);

                    } else if (record.get(1).strip().equalsIgnoreCase("Hospitalization")) {
                        LOGGER.info(record.get(1));

                        HealthcareService service = new HealthcareService();
                        service.setHealthcareServiceName(record.get("ServiceName"));
                        Category category = new Category();
                        category.setCode(record.get(1).strip());
                        List<Category> cats = new ArrayList<>();
                        cats.add(category);
                        service.setHealthcareServiceCategories(cats);

                        Specialty specialty = new Specialty();
                        specialty.setCode(record.get("ServiceSpecialty"));
                        List<Specialty> specs = new ArrayList<>();
                        specs.add(specialty);
                        service.setHealthcareServiceSpecialties(specs);

                        ServiceType serviceType = new ServiceType();
                        serviceType.setCode(record.get("ServiceType"));
                        serviceType.setDisplay(record.get("ServiceType"));
                        ;
                        List<ServiceType> serviceTypes = new ArrayList<>();
                        serviceTypes.add(serviceType);
                        service.setHealthcareServiceTypes(serviceTypes);
                        service.setProvider("161380e9-22d3-4627-a97f-0f918ce3e4a9");

                        service.setSlotDuration("30");
                        service.setVirtual_service(false);
                        service.setHealthcareServiceAppointmentRequired(true);
                        service.setComment("Healthcare service for General Consultation at Nyaho Medical Center");
                        service.setExtraDetails(record.get("BasePrice"));
                        service.setMaximumCapacity(10);
                        service.setRevenueDisplayTag(record.get("RevenueDisplayTag"));
                        service.setSubscriptionFrequency("Weekly");
                        service.setWardType("ICU");
                        service.setHealthcareServiceServiceProvisionCode("cost");

                        PriceTier free = new PriceTier();
                        free.setCharge("0");
                        free.setDisplay("Free");
                        free.setCurrency("GHS");
                        free.setDescription("Free service for " + service.getHealthcareServiceName());
                        free.setPriority("routine");

                        PriceTier priceTier = new PriceTier();
                        priceTier.setCharge(record.get("BasePrice"));
                        priceTier.setCurrency("GHS");
                        priceTier.setDisplay("Express");
                        priceTier.setPriority("urgent");
                        priceTier.setDisplay("Express");
                        priceTier.setDescription("Express service for " + record.get("ServiceName"));

                        PriceTier standard = new PriceTier();
                        standard.setCharge("50");
                        standard.setDisplay("Standard");
                        standard.setCurrency("GHS");
                        standard.setDescription("Standard service for " + record.get("ServiceName"));
                        standard.setPriority("routine");

                        List<PriceTier> priceTiers = new ArrayList<>();
                        priceTiers.add(priceTier);
                        priceTiers.add(free);
                        priceTiers.add(standard);
                        service.setPriceTiers(priceTiers);
                        ServiceData d = new ServiceData();
                        Gson gson = new Gson();
                        String json = gson.toJson(service);
                        System.err.println(json);
                        d.setServiceName(service.getHealthcareServiceName());
                        d.setData(json);

                        services.add(d);

                    }

                    else {

                        HealthcareService service = new HealthcareService();
                        service.setActive(true);
                        service.setHealthcareServiceAppointmentRequired(true);
                        service.setVirtual_service(true);

                        service.setOrderCode(record.get("DiagnosticOrderCode").equals("") ? null : record.get(4));

                        service.setHealthcareServiceName(record.get("ServiceName"));

                        service.setServiceRequestCategory(
                                record.get("ServiceRequestCategory").equals("") ? null : record.get(3));

                        service.setDiagnosticServiceSection(
                                record.get("DiagnosticServiceSection").equals("") ? null : record.get(5));

                        service.setCharacteristic(null);
                        service.setComment("This is the service description");
                        service.setCommunication("EN");
                        service.setExtraDetails(null);
                        service.setTelecom("0267600606");
                        service.setProgram(null);
                        String[] methods = { "fax", "mail", "phone" };
                        service.setReferralMethod(Arrays.asList(methods));
                        List<AvailableTime> avTime = new ArrayList<>();
                        AvailableTime time = new AvailableTime();
                        time.setAllDay(true);
                        time.setAvailableStartTime("00:00:00Z");
                        time.setAvailableEndTime("23:59:59Z");
                        String[] days = { "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday",
                                "Sunday" };
                        time.setDaysOfWeek(Arrays.asList(days));
                        avTime.add(time);
                        service.setHealthcareServiceAvailableTimes(avTime);
                        service.setSlotDuration("30");
                        service.setHealthcareServiceServiceProvisionCode("Free");

                        if (!record.get(3).isEmpty()) {
                            Category category = new Category();
                            category.setCode(record.get("ServiceClass"));
                            List<com.serenity.integration.models.Category> cats = new ArrayList<>();
                            cats.add(category);
                            service.setHealthcareServiceCategories(cats);
                        }
                        Specialty specialty = new Specialty();
                        specialty.setCode(record.get("ServiceSpecialty"));
                        List<Specialty> specs = new ArrayList<>();
                        specs.add(specialty);
                        service.setHealthcareServiceSpecialties(specs);

                        ServiceType serviceType = new ServiceType();
                        serviceType.setCode(record.get("ServiceType"));
                        List<ServiceType> serviceTypes = new ArrayList<>();
                        serviceTypes.add(serviceType);
                        service.setHealthcareServiceTypes(serviceTypes);
                        service.setProvider("161380e9-22d3-4627-a97f-0f918ce3e4a9");
                        PriceTier priceTier = new PriceTier();
                        priceTier.setCharge(record.get("BasePrice"));
                        priceTier.setCurrency("GHS");
                        priceTier.setPriority("urgent");
                        priceTier.setDisplay("Express");
                        priceTier.setDescription("Express service for " + record.get("ServiceName"));
                        List<PriceTier> priceTiers = new ArrayList<>();
                        priceTiers.add(priceTier);
                        service.setPriceTiers(priceTiers);
                        service.setRevenueDisplayTag(record.get(file));
                        Location location = new Location();
                        location.setId("u101-location-None");
                        List<Location> locations = new ArrayList<>();
                        locations.add(location);
                        service.setHealthcareServiceLocations(locations);
                        ServiceData d = new ServiceData();
                        Gson gson = new Gson();
                        String json = gson.toJson(service);
                        System.err.println(json);
                        d.setServiceName(service.getHealthcareServiceName());
                        d.setData(json);
                        services.add(d);

                    }
                }

            }
        } catch (IOException e) {
            e.printStackTrace();

        }

        System.err.println(services.size());
        List<Report> daps = new ArrayList<>();

        serviceDataRepo.saveAllAndFlush(services);
        List<ServiceData> datas = serviceDataRepo.findAll();
        for (ServiceData a : datas) {
            try {
                addHealthService(org, a.getData());
            } catch (Exception e) {
                Report rep = new Report();
                rep.setServiceName(a.getServiceName());
                rep.setError(e.getMessage());
                daps.add(rep);
                e.printStackTrace();

            }
        }
        reportRepo.saveAllAndFlush(daps);
    }

    public  void savePricing(ServicePricing price) {

        String url = "https://stag.api.cloud.serenity.health/v2/billing/service-prices";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.add("x-api-key", "efomrddi");
        HttpEntity<ServicePricing> httpEntity = new HttpEntity<>(price, headers);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<ServicePriceResponse> response = restTemplate.exchange(url, HttpMethod.POST, httpEntity,
                ServicePriceResponse.class);
        LOGGER.info(response.getBody().toString());

    }


    private void updatePricing(ServicePricing price) {

        String url = "https://stag.api.cloud.serenity.health/v2/billing/service-prices/"+price.getHealthcareServiceId();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.add("x-api-key", "efomrddi");
        HttpEntity<ServicePricing> httpEntity = new HttpEntity<>(price, headers);
        RestTemplate restTemplate = new RestTemplate();
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();


        restTemplate.setRequestFactory(requestFactory);
        ResponseEntity<ServicePriceResponse> response = restTemplate.exchange(url, HttpMethod.PATCH, httpEntity,
                ServicePriceResponse.class);
        LOGGER.info(response.getBody().toString());

    }
}
