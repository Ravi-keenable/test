package in.nic.npi.utilities;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("unchecked")
public class ProcessRecord {

    public Set<String> getRecordKeys(Object input) {
        Set<String> keys = new HashSet<>();
        recurseKeys(input, "", keys);
        return keys;
    }

    public void recurseKeys(Object input, String tracedPath, Set<String> keyList) {
        if (!(input instanceof Map) && !(input instanceof List)) {
            if (tracedPath != "") {
                keyList.add(tracedPath.substring(1, tracedPath.length()));
            }
            return;
        } else if (input instanceof Map) {
            processMapObj(input, tracedPath, keyList);
        } else if (input instanceof List) {
            processListObj(input, tracedPath, keyList);
        } else {
            throw new NullPointerException("Invalid input");
        }
    }

    private void processListObj(Object input, String tracedPath, Set<String> keyList) {
        List<Object> list = (List<Object>) input;
        // log.info("{}",list);

        int size = list.size();
        for (int i = 0; i < size; i++) {
            Object obj = list.get(i);
            if (obj instanceof Map) {
                String newTracedPath = tracedPath + "[" + i + "]";
                recurseKeys(obj, newTracedPath, keyList);
            } else if (obj instanceof List) {
                recurseKeys(obj, tracedPath, keyList);
            } else {
                recurseKeys(obj, tracedPath, keyList);
            }
        }
        return;
    }

    private void processMapObj(Object input, String tracedPath, Set<String> keyList) {
        Map<String, Object> map = (Map<String, Object>) input;
        Set<String> keySet = map.keySet();
        for (String key : keySet) {
            String newTracedPath = tracedPath + "." + key;
            recurseKeys(map.get(key), newTracedPath, keyList);
        }
        return ;
    }

}

