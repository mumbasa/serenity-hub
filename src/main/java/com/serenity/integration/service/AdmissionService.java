package com.serenity.integration.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Service;

@Service
public class AdmissionService {
@Autowired
JdbcTemplate hisJdbcTemplate;

    public void getAdmission(){

    String queryString = """
        select
            replace(ip.Transaction_ID, 'ISHHI', '') uuid,
            concat(pmh.EntryDate, ' 00:00:00') created_at,
            -- set the time part of created_at timestamp to 0 because there's no admission time in, only a Date
            case
                when pmh.Updatedate is null then concat(pmh.EntryDate, ' 00:00:00')
                else pmh.Updatedate
            end as updated_at,  
            pmh.Patient_ID patient_id,
            null healthcare_service_name, -- @TODO
            concat(dm.Title, ' ', dm.Name) admitted_by_name,
            e_mas.Name created_by_name,
            pmh.Source admit_source, -- @TODO
            pmh.Source admit_source_display, -- @TODO
            concat(ich.DateOfAdmit, ' ', ich.TimeOfAdmit) admitted_at,
            null origin, -- @TODO
            null price_tier_name, -- @TODO
            case 
                when ich.Status = 'OUT' then false
                else true
            end as is_active,
            null is_readmission,
            rm.Name room_name,
            rm.Bed_No bed_name,
            null reason, -- @TODO
            null transferred_by_name,
            null transferred_at,
            "Nyaho Medical Centre" location_id, -- @TODO
            "Nyaho Medical Centre" location_name, -- @TODO
            case 
                when ich.status = 'OUT' then "active"
                when ich.status = 'Cancel' then "cancelled"
                else "discharged"
            end as status,
            ich.Consultant_ID admitted_by,
            ich.Employee_ID created_by,
            null healthcare_service_id, -- @TODO
            "Nyaho Medical Centre" provider_id, -- @TODO
            ip.Room_ID room_id,
            null bed_id,
            replace(ip.Transaction_ID, 'ISHHI', '') visit_id, -- @TODO
            null discharge_disposition,
            ich.DischargedBy discharged_by_id,
            concat(ich.DateOfDischarge, ' ', ich.TimeOfDischarge) discharged_at,
            discharge_user.Name discharged_by_name,
            null transferred_by_id,
            false is_deleted,
            0 total_billed_cycles,
            null last_billing_datetime,
            null notes,
            pm.PName patient_full_name,
            pm.Mobile patient_mobile,
            pm.Gender patient_gender,
            pm.DOB patient_birth_date,
            pm.Patient_ID patient_mr_number,
            null discharged_authorized_at,
            null discharged_authorized_by_name,
            null discharged_authorized_by_id,
            null destination_display
        from
            patient_ipd_profile ip
        inner join patient_medical_history PMH on
            IP.Transaction_ID = PMH.Transaction_ID
        inner join ipd_case_history ich on
            ip.Transaction_ID = ich.Transaction_ID
        inner join room_master rm on
            rm.Room_Id = ip.Room_ID
        inner join ipd_case_type_master ctm on
            ctm.IPDCaseType_ID = rm.IPDCaseTypeID
        inner join doctor_master dm on
            dm.Doctor_ID = ich.Consultant_ID
        inner join employee_master e_mas on
            e_mas.Employee_ID = pmh.UserID
        inner join employee_master discharge_user on
            discharge_user.Employee_ID = ich.DischargedBy
        inner join patient_master pm on
            pm.Patient_ID = ip.PatientID
        inner join f_ipdadjustment fi on
            fi.Transaction_ID = ip.Transaction_ID
        inner join f_panel_master pnl on
            pnl.Panel_ID = ip.PanelID
        group by
            ip.Transaction_ID
        having
            min(ip.StartDate)
        order by
            ich.DateOfAdmit desc LIMIT 10
        """;

SqlRowSet set = hisJdbcTemplate.queryForRowSet(queryString);
while (set.next()){
System.err.println(set.getString(1));
System.err.println(set.getString(2));

}


}

}
