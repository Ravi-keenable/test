package in.nic.npi.utilities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.jayway.jsonpath.JsonPath;
import in.nic.npi.exception.PathNotFoundException;
import lombok.extern.slf4j.Slf4j;

@SuppressWarnings("unchecked")
@Slf4j
public class CustomConfigMapper {

    public int i = 1;
    private final Pattern pattern = Pattern.compile("\\[[0-9]\\]");

    protected boolean checkIfJsonArrayPath(String path) {
        //log.info(i++ + "----------" + path);
        Matcher matcher = pattern.matcher(path);
        if (matcher.find()) {
            matcher.reset();
            return true;
        }
        matcher.reset();
        return false;
    }

    protected String[] getJsonArrayKeyAndIndex(String path) {

        Matcher matcher = pattern.matcher(path);
        if (matcher.find()) {
            String matchedString = matcher.group(0);
            log.debug(matchedString + "}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}");
            String index = path.substring(0, path.length() - matchedString.length());
            log.debug(index + "}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}");
            String intval = matchedString.substring(1, matchedString.length() - 1);
            log.debug(intval + "}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}");
            String[] arr = { index, intval };
            return arr;
        }
        return null;

    }

    protected Object parseJsonPathExpression(String jsonPathExpression, boolean keepKeyFlag, String json) {
        // for hardcoded value
        //log.info(jsonPathExpression);
        if (jsonPathExpression.startsWith("#")) {
            return jsonPathExpression.substring(1);
        } else {
            Object jsonpathvalue = JsonPath.parse(json).read(jsonPathExpression);
            // check if key needs to be mapped if evaluated expression and return data
            // accordingly
            if (keepKeyFlag) {
                String[] splitConfig = jsonPathExpression.split("\\.");
                String lastElementPathInExp = splitConfig[splitConfig.length - 1];
                Map<String, Object> map = new HashMap<>();
                if (checkIfJsonArrayPath(lastElementPathInExp)) {
                    String[] arr = getJsonArrayKeyAndIndex(lastElementPathInExp);
                    String key = arr[0];
                    map.put(key, jsonpathvalue);
                    return map;
                } else {
                    map.put(lastElementPathInExp, jsonpathvalue);
                    return map;
                }
            } else {
                return jsonpathvalue;
            }

        }

    }

    /* Stack contains paths , overwriteValue is used to specify to */
    public void recursiveFindAndSet(Object skeletalBody, Queue<String> stack, Object jsonpathValue,
            boolean overwriteValue)
            throws PathNotFoundException {

        //log.info("Inside recursiveFindAndSet - Stack Size of nested paths to process {}  ---  {} ", stack.size(),stack);
        // Base case for recursion
        if (stack.size() == 1) {
            // Last path process will always return map as xyz[0] specifies xyz keys value
            // at 0 index
            processBaseCase(skeletalBody, stack, jsonpathValue, overwriteValue);
            return;
        }

        if (skeletalBody instanceof Map) {
            parseMap(skeletalBody, stack, jsonpathValue, overwriteValue);
            return;
        } else if (skeletalBody instanceof List) {
            parseList(skeletalBody, stack, jsonpathValue, overwriteValue);
            return;
        } else {
            throw new NullPointerException("Invalid jsonpath expression ");
        }

    }

    private void processBaseCase(Object skeletalBody, Queue<String> stack, Object jsonpathValue, boolean overwriteValue)
            throws PathNotFoundException {

        //log.info("Inside processBaseCase - Printing stack size of paths to process {}", stack);
        String path = stack.poll();
        //log.info("Inside processBaseCase - Printing path to process {}", path);
        Map<String, Object> mapObject = (Map<String, Object>) skeletalBody;

        if (checkIfJsonArrayPath(path)) {
            //log.info("Inside processBaseCase - Processing path as its a jsonarray path");
            setValueInJsonPathArray(mapObject, path, jsonpathValue, overwriteValue);
            //log.info("Inside processBaseCase - Value set in desired location");
            return;
        } else if (mapObject.containsKey(path)) {

            // if not master since some keys in canonical body dont require overriding we
            // will use processmaster for now
            // need to add new feature later to configure to pass a list of fixed keys when
            // declaring mapper object to make it configurable like in builder class
            //log.info("Inside processBaseCase - Setting Value in specified path");
            if (overwriteValue)
            mapObject.put(path, jsonpathValue);
            else
            appendValueInKey(mapObject, path, jsonpathValue);
            return;
        } else {
            throw new PathNotFoundException("key not found in json ");
        }

    }

     // this method is to any value in a list to already existing one
     private void appendValueInKey(Map<String, Object> mapObject, String path, Object jsonpathValue) {
        Object obj = mapObject.get(path);
        List<Object> list = new ArrayList<>();
        if (obj == null){
            list.add(jsonpathValue);
            mapObject.put(path, list);
        }
        else if (obj instanceof Map) {
            list.add((Map<String, Object>) obj);
            list.add(jsonpathValue);
            mapObject.put(path,list);
        } else if (obj instanceof List) {
            
            ((List<Object>) obj).add(jsonpathValue);            
        }
        return;
    }

    private void setValueInJsonPathArray(Map<String, Object> mapObject, String path, Object jsonpathValue,
            boolean overwriteValue) throws PathNotFoundException {
        String[] arr = getJsonArrayKeyAndIndex(path);
        if (mapObject.containsKey(arr[0])) {
            try {
                // Extract list from map before setting its value
                Object pathobj = mapObject.get(arr[0]);
                List<Object> obj = (List<Object>) pathobj;
                int index = Integer.parseInt(arr[1]);
                // if it is master we will just add the data and ignore the index specified
                if (overwriteValue)
                    obj.set(index, jsonpathValue);
                else
                    obj.add(jsonpathValue);
            } catch (Exception e) {
                throw new NullPointerException("Invalid json expression ");
            }
            return;
        } else {
            throw new PathNotFoundException("Invalid json path does not exist");
        }
    }

    private void parseMap(Object skeletalBody, Queue<String> stack, Object jsonpathValue, boolean overwriteValue)
            throws PathNotFoundException {
        Map<String, Object> mapObject = (Map<String, Object>) skeletalBody;
        String path = stack.poll();
        //log.info("Processing path {} and path left to process {}",path,stack.size());

        if (checkIfJsonArrayPath(path)) {
            String[] arr = getJsonArrayKeyAndIndex(path);
            if (mapObject.containsKey(arr[0])) {
                try {
                    Object pathobj = mapObject.get(arr[0]);
                    List<Object> obj = (List<Object>) pathobj;
                    int index = Integer.parseInt(arr[1]);
                    recursiveFindAndSet(obj.get(index), stack, jsonpathValue, overwriteValue);
                } catch (Exception e) {
                    throw new NullPointerException("Invalid json expression index not present ---- parseMap  --- JsonpathCheck");
                }
            }
        } else if (mapObject.containsKey(path) && mapObject.get(path) != null) {
            //log.info(path);
            recursiveFindAndSet(mapObject.get(path), stack, jsonpathValue, overwriteValue);
        } else {
            throw new NullPointerException("Invalid json expression ---- parseMap");
        }
    }

    private void parseList(Object skeletalBody, Queue<String> stack, Object jsonpathValue, boolean overwriteValue) {
        try {
            List<Object> listObjects = (List<Object>) skeletalBody;
            String path = stack.poll();
            String[] arr = getJsonArrayKeyAndIndex(path);
            //log.info(path + stack.size());
            int index = Integer.parseInt(arr[1]);

            Object obj = listObjects.get(index);
            recursiveFindAndSet(obj, stack, jsonpathValue, overwriteValue);
        } catch (Exception e) {

            throw new NullPointerException("Invalid json expression index not present ----- parseList");
        }
    }

}
