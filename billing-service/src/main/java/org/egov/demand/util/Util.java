package org.egov.demand.util;

import static org.egov.demand.util.Constants.ADVANCE_BUSINESSSERVICE_JSONPATH_CODE;
import static org.egov.demand.util.Constants.INVALID_TENANT_ID_MDMS_KEY;
import static org.egov.demand.util.Constants.INVALID_TENANT_ID_MDMS_MSG;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.egov.common.contract.request.RequestInfo;
import org.egov.demand.config.ApplicationProperties;
import org.egov.demand.model.AuditDetails;
import org.egov.demand.repository.ServiceRequestRepository;
import org.egov.mdms.model.MasterDetail;
import org.egov.mdms.model.MdmsCriteria;
import org.egov.mdms.model.MdmsCriteriaReq;
import org.egov.mdms.model.ModuleDetail;
import org.egov.tracer.model.CustomException;
import org.postgresql.util.PGobject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

@Component
@Slf4j
public class Util {

	@Autowired
	private ApplicationProperties appProps;
	
	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private ServiceRequestRepository serviceRequestRepository;

	/**
	 * prepares mdms request
	 * 
	 * @param tenantId
	 * @param moduleName
	 * @param names
	 * @param filter
	 * @param requestInfo
	 * @return
	 */
	public MdmsCriteriaReq prepareMdMsRequest(String tenantId, String moduleName, List<String> names, String filter,
			RequestInfo requestInfo) {

		List<MasterDetail> masterDetails = new ArrayList<>();
		names.forEach(name -> {
				masterDetails.add(MasterDetail.builder().name(name).build());
		});

		ModuleDetail moduleDetail = ModuleDetail.builder().moduleName(moduleName).masterDetails(masterDetails).build();
		List<ModuleDetail> moduleDetails = new ArrayList<>();
		moduleDetails.add(moduleDetail);
		MdmsCriteria mdmsCriteria = MdmsCriteria.builder().tenantId(tenantId).moduleDetails(moduleDetails).build();
		return MdmsCriteriaReq.builder().requestInfo(requestInfo).mdmsCriteria(mdmsCriteria).build();
	}

	/**
	 * Fetches all the values of particular attribute as documentContext
	 *
	 * @param tenantId    tenantId of properties in PropertyRequest
	 * @param names       List of String containing the names of all master-data
	 *                    whose code has to be extracted
	 * @param requestInfo RequestInfo of the received PropertyRequest
	 * @return Map of MasterData name to the list of code in the MasterData
	 *
	 */
	public DocumentContext getAttributeValues(MdmsCriteriaReq mdmsReq) {
		StringBuilder uri = new StringBuilder(appProps.getMdmsHost()).append(appProps.getMdmsEndpoint());

		try {
			return JsonPath.parse(serviceRequestRepository.fetchResult(uri.toString(), mdmsReq));
		} catch (Exception e) {
			log.error("Error while fetvhing MDMS data", e);
			throw new CustomException(INVALID_TENANT_ID_MDMS_KEY, INVALID_TENANT_ID_MDMS_MSG);
		}
	}

	/**
	 * Generates the Audit details object for the requested user and current time
	 * 
	 * @param requestInfo
	 * @return
	 */
	public AuditDetails getAuditDetail(RequestInfo requestInfo) {

		String userId = requestInfo.getUserInfo().getUuid();
		Long currEpochDate = System.currentTimeMillis();

		return AuditDetails.builder().createdBy(userId).createdTime(currEpochDate).lastModifiedBy(userId)
				.lastModifiedTime(currEpochDate).build();
	}

	public String getStringVal(Set<String> set) {
		StringBuilder builder = new StringBuilder();
		int i = 0;
		for (String val : set) {
			builder.append(val);
			i++;
			if (i != set.size())
				builder.append(",");
		}
		return builder.toString();
	}
	
	/**
	 * converts the object to a pgObject for persistence
	 * 
	 * @param additionalDetails
	 * @return
	 */
	public PGobject getPGObject(Object additionalDetails) {

		String value = null;
		try {
			value = mapper.writeValueAsString(additionalDetails);
		} catch (JsonProcessingException e) {
			throw new CustomException(Constants.EG_BS_JSON_EXCEPTION_KEY, Constants.EG_BS_JSON_EXCEPTION_MSG);
		}

		PGobject json = new PGobject();
		json.setType(Constants.DB_TYPE_JSONB);
		try {
			json.setValue(value);
		} catch (SQLException e) {
			throw new CustomException(Constants.EG_BS_JSON_EXCEPTION_KEY, Constants.EG_BS_JSON_EXCEPTION_MSG);
		}
		return json;
	}
	
    public JsonNode getJsonValue(PGobject pGobject){
        try {
            if(Objects.isNull(pGobject) || Objects.isNull(pGobject.getValue()))
                return null;
            else
                return mapper.readTree( pGobject.getValue());
        } catch (Exception e) {
        	throw new CustomException(Constants.EG_BS_JSON_EXCEPTION_KEY, Constants.EG_BS_JSON_EXCEPTION_MSG);
        }
    }


    public String getApportionURL(){
		StringBuilder builder = new StringBuilder(appProps.getApportionHost());
		builder.append(appProps.getApportionEndpoint());
		return builder.toString();
	}

	/**
	 * Fetches the isAdvanceAllowed flag for the given businessService
	 * @param businessService
	 * @param mdmsData
	 * @return
	 */
	public Boolean getIsAdvanceAllowed(String businessService, DocumentContext mdmsData){
		String jsonpath = ADVANCE_BUSINESSSERVICE_JSONPATH_CODE;
		jsonpath = jsonpath.replace("{}",businessService);

		List<Boolean> isAdvanceAllowed = mdmsData.read(jsonpath);

		if(CollectionUtils.isEmpty(isAdvanceAllowed))
			throw new CustomException("BUSINESSSERVICE_ERROR","Failed to fetch isAdvanceAllowed for businessService: "+businessService);

		return isAdvanceAllowed.get(0);
	}
	
}