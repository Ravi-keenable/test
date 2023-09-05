package in.nic.npi.process.configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.camel.Exchange;
import com.jayway.jsonpath.JsonPath;

@SuppressWarnings("unchecked")
public class ProcessInputConfig {

    // change Path headers body and Query headers to map exact value from master
    public void parseDependentValues(Exchange exchange) {
        Map<String, Object> pathParams = (Map<String, Object>) exchange.getProperty("PathParam");
        Map<String, Object> queryParams = (Map<String, Object>) exchange.getProperty("QueryParam");
        Map<String, Object> parsedDependencyValues = exchange.getProperty("ParsedDependencyValues", Map.class);

        Object payload;
        try {
            payload = exchange.getProperty("Payload");
            setDependentRecursive(payload, parsedDependencyValues);
        } catch (Exception e) {
            e.printStackTrace();
        }
        setDependentRecursive(pathParams, parsedDependencyValues);
        setDependentRecursive(queryParams, parsedDependencyValues);

        String query = setQueryParamHeader(queryParams);
        exchange.getIn().setHeader(Exchange.HTTP_QUERY, query);
    }

    private void setDependentRecursive(Object obj, Map<String, Object> parsedDependencyValues) {
        if (obj instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) obj;
            setDependentRecursiveMap(map, parsedDependencyValues);
        } else if (obj instanceof Map) {
            List<Object> listInstance = (List<Object>) obj;
            setDependentRecursiveList(listInstance, parsedDependencyValues);
        }
    }

    private void setDependentRecursiveMap(Map<String, Object> baseMap, Map<String, Object> parsedDependencyValues) {
        Set<Entry<String, Object>> entries = baseMap.entrySet();
        for (Entry<String, Object> entry : entries) {
            Object value = entry.getValue();
            if (value instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) entry.getValue();
                setDependentRecursiveMap(map, parsedDependencyValues);
            } else if (value instanceof List) {
                List<Object> list = (List<Object>) entry.getValue();
                setDependentRecursiveList(list, parsedDependencyValues);
            } else {
                String data;
                try {
                    data = value.toString();

                } catch (Exception e) {
                    data = "";
                }
                if (data.startsWith("#")) {
                    String dependencyKey = data.substring(1);
                    Object dependencyValue = parsedDependencyValues.get(dependencyKey);
                    baseMap.put(entry.getKey(), dependencyValue);
                }
            }
        }
    }

    private void setDependentRecursiveList(List<Object> list, Map<String, Object> dependencyValues) {
        for (int i = 0; i < list.size(); i++) {
            Object listObject = list.get(i);
            if (listObject instanceof List) {
                List<Object> listInstance = (List<Object>) listObject;
                setDependentRecursiveList(listInstance, dependencyValues);
            } else if (listObject instanceof Map) {
                Map<String, Object> mapInstane = (Map<String, Object>) listObject;
                setDependentRecursiveMap(mapInstane, dependencyValues);
            } else {
                String data;
                try {
                    data = listObject.toString();

                } catch (Exception e) {
                    data = "";
                }
                if (data.startsWith("#")) {
                    String dependencyKey = data.substring(1);
                    Object dependencyValue = dependencyValues.get(dependencyKey);
                    list.set(i, dependencyValue);

                }
            }
        }
    }

    private String setQueryParamHeader(Map<String, Object> queryParams) {
        if (queryParams != null) {
            String query = "";
            for (Entry<String, Object> iterable_element : queryParams.entrySet()) {
                query = query + iterable_element.getKey() + "=" + (String) iterable_element.getValue();
            }
            return query;
        }
        return null;
    }

    public void setDependencyMapFromMaster(Exchange exchange) {
        String masterJson = exchange.getIn().getBody(String.class);
        Map<String, Object> map = exchange.getProperty("DependencyValues", Map.class);
        Set<Entry<String, Object>> entries = map.entrySet();
        Map<String, Object> parsedDependencyValues = new HashMap<>();
        for (Entry<String, Object> entry : entries) {
            String masterDependencyJsonPath = (String) entry.getValue();
            Object masterDependencyValue = JsonPath.parse(masterJson).read(masterDependencyJsonPath);
            parsedDependencyValues.put(entry.getKey(), masterDependencyValue);
        }
        parsedDependencyValues.put("test", 10000);
        exchange.setProperty("ParsedDependencyValues", parsedDependencyValues);
    }
}
