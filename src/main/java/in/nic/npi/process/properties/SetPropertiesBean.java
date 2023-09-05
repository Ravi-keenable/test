package in.nic.npi.process.properties;

import java.util.HashMap;
import java.util.Map;
import org.apache.camel.Exchange;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;

import in.nic.npi.constants.ConfigConstants;
import in.nic.npi.constants.ConfigJsonpathConstant;

/*This class sets the values from input configuration in camel exchange properties , so it can be used later */
public class SetPropertiesBean {

    public void setProperties(Exchange exchange) {

        Map<String, Object> properties = exchange.getProperties();
        Map<String, Object> customProperties = new HashMap<>();
        String configuration = exchange.getIn().getBody(String.class);
        Object originalbody = configuration;

        Object apiRequestMethod = JsonPath.parse(configuration).read(ConfigJsonpathConstant.Api_Request_Method_Jsonpath);
        Object ressplitpath = JsonPath.parse(configuration).read(ConfigJsonpathConstant.Response_Split_Path_Jsonpath);

        boolean isIterative = Boolean.parseBoolean(JsonPath.parse(configuration).read(ConfigJsonpathConstant.Is_Iterative_Jsonpath));

        Object pathParams = processPathParams(configuration);
        Object queryParams = processQueryParams(configuration);
        Object authParams = processAuthParams(configuration);
        boolean isDependent = Boolean.parseBoolean(JsonPath.parse(configuration).read(ConfigJsonpathConstant.Is_Dependent_Jsonpath));
        Object payload = JsonPath.parse(configuration).read(ConfigJsonpathConstant.Payload_Jsonpath);

        Object uniqueRecSplitPath = JsonPath.parse(configuration).read(ConfigJsonpathConstant.Unique_Record_Key_JsonPath);

        if(uniqueRecSplitPath != null){
            customProperties.put(ConfigConstants.Unique_Record_Key_Path, uniqueRecSplitPath);
        }
        if (payload != null) {
            customProperties.put(ConfigConstants.Payload, payload);
        }
      
        if (authParams != null) {
            customProperties.put(ConfigConstants.Auth_Param, authParams);
        }
        if (queryParams != null) {
            customProperties.put(ConfigConstants.Query_Param, queryParams);
        }
        if (pathParams != null) {
            customProperties.put(ConfigConstants.Path_Param, pathParams);
        }

        if (isDependent == true) {
            Object mastersplitpath = JsonPath.parse(configuration).read(ConfigJsonpathConstant.Master_Split_Path_Jsonpath);
            Object dependentMasterApi = JsonPath.parse(configuration).read(ConfigJsonpathConstant.Dependent_Master_Api_Jsonpath);
            Object dependentValueMap = JsonPath.parse(configuration)
                    .read(ConfigJsonpathConstant.Dependent_Master_Values_Jsonpath);
            customProperties.put(ConfigConstants.Dependent_Master_Api, dependentMasterApi);
            customProperties.put(ConfigConstants.Dependent_Master_Values,dependentValueMap);
            customProperties.put(ConfigConstants.Master_Split_Path,mastersplitpath);



        }
        if (isIterative == true) {
            Object iterativeKeys = JsonPath.parse(configuration).read(ConfigJsonpathConstant.Iterative_Keys_Jsonpath);
            Object iterativeType = JsonPath.parse(configuration).read(ConfigJsonpathConstant.Iterative_Type_Jsonpath);
            Object iteratesByHeader = JsonPath.parse(configuration).read(ConfigJsonpathConstant.Iterates_By_Header_Jsonpath);

            customProperties.put(ConfigConstants.Iterative_Keys, iterativeKeys);
            customProperties.put(ConfigConstants.Iterative_Type, iterativeType);
            customProperties.put(ConfigConstants.Iterates_By_Header, iteratesByHeader);

        }
        customProperties.put(ConfigConstants.Response_Split_Path,ressplitpath);
        customProperties.put(ConfigConstants.Is_Dependent,isDependent);
        customProperties.put(ConfigConstants.Is_Iterative, isIterative);
        customProperties.put(ConfigConstants.Original_Body, originalbody);
        customProperties.put(ConfigConstants.Api_Request_Method, apiRequestMethod);
        
        properties.putAll(customProperties);

    }

    private Map<String, String> processPathParams(String configuration) {
        try {
            Map<String, String> pathparams = JsonPath.parse(configuration).read(ConfigJsonpathConstant.Path_Param_Jsonpath);
            return pathparams;
        } catch (PathNotFoundException e) {
            return null;
        }
    }

    private Map<String, String> processQueryParams(String configuration) {
        try {
            Map<String, String> queryparams = JsonPath.parse(configuration).read(ConfigJsonpathConstant.Query_Param_Jsonpath);
            return queryparams;
        } catch (PathNotFoundException e) {
            return null;
        }
    }

    private Map<String, String> processAuthParams(String configuration) {
        try {
            Map<String, String> authparams = JsonPath.parse(configuration).read(ConfigJsonpathConstant.Auth_Param_Jsonpath);
            return authparams;
        } catch (PathNotFoundException e) {
            return null;
        }
    }
}
