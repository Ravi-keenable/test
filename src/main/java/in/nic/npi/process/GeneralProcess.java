package in.nic.npi.process;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.camel.Exchange;
import org.apache.camel.ExchangePropertyKey;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import in.nic.npi.constants.CamelPropertiesConstants;
import in.nic.npi.constants.ConfigConstants;
import in.nic.npi.exception.EndOfDataException;
import in.nic.npi.pojo.GlobalDataObject;
import in.nic.npi.utilities.CounterClass;
import in.nic.npi.utilities.ProcessRecord;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SuppressWarnings("unchecked")
public class GeneralProcess {

    public final ObjectMapper mapper = new ObjectMapper();
    public final ProcessRecord keyListGen = new ProcessRecord();
    public void masterCallProcess(Exchange exchange) {
        log.debug("Inside General Process masterCallProcess method start");
        List<String> masterlist = (List<String>) exchange.getProperty(ConfigConstants.Dependent_Master_Api);
        exchange.getIn().setHeader("master_path_param", masterlist.get(0));
        log.debug("Inside General Process masterCallProcess method end");

    }

    public void kafkaStorageProcess(Exchange exchange) throws EndOfDataException {
        GlobalDataObject globalDataObject = (GlobalDataObject) exchange.getProperty("GLOBAL_OBJECT");
        String body = exchange.getIn().getBody(String.class);
        String ressplitpath = (String) exchange
                .getProperty(ConfigConstants.Response_Split_Path);
        List<Object> object = null;



        try {
            object = JsonPath.parse(body).read(ressplitpath);
            /*
             * case to check if response comes empty array like in iterative api with
             * multiple calls
             */
            if (object.size() == 0)
                throw new EndOfDataException("Reached end of iterative process");
            else if (object.size() > 0){
                if(globalDataObject.getRecordKeyList() == null){
                    Set<String> globalKeyList = keyListGen.getRecordKeys(object.get(0));
                    globalDataObject.setRecordKeyList(globalKeyList);
                }
                else{
                    Set<String> keyList = keyListGen.getRecordKeys(object.get(0));
                    if(keyList.size() == 0){
                        log.error("Recieved key list is zero");
                        throw new EndOfDataException("Reached end of iterative process");
                    }
                }
            }
        } catch (Exception e) {
            throw new EndOfDataException("Reached end of iterative process");
        }
        exchange.setProperty(CamelPropertiesConstants.Ext_Api_Record_List, object);
    }

    public void endOfDataBody(Exchange exchange) throws JsonProcessingException {
        CounterClass obj = exchange.getProperty("RECORD_OBJECT", CounterClass.class);
        Map<String, Object> responseBody = new HashMap<>();

        if (exchange.getProperty(ExchangePropertyKey.EXCEPTION_CAUGHT) instanceof EndOfDataException) {
            responseBody.put("MessageProcessed", obj.RECORD_COUNTER);
            responseBody.put("DuplicateMessage", obj.RECORD_COUNTER - obj.TOTAL_RECORD_KAFKA_COUNTER);
            responseBody.put("BatchProcessed", obj.BATCH_ID_INDEX);
            responseBody.put("Remarks", "Processing Completed");
            exchange.getIn().setBody(mapper.writeValueAsString(responseBody));
            exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 200);
            exchange.getIn().setHeader(Exchange.CONTENT_TYPE, "application/json");

        } else {
            responseBody.put("MessageProcessed", obj.RECORD_COUNTER);
            responseBody.put("DuplicateMessage", obj.RECORD_COUNTER - obj.TOTAL_RECORD_KAFKA_COUNTER);
            responseBody.put("BatchProcessedProcessed", obj.BATCH_ID_INDEX);
            responseBody.put("Remarks", "Encountered error at batch " + obj.BATCH_ID_INDEX + " and index "
                    + obj.RECORD_ID_INDEX + " Closing pipeline");
            exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 500);
            exchange.getIn().setHeader(Exchange.CONTENT_TYPE, "application/json");
            exchange.getIn().setBody(mapper.writeValueAsString(responseBody));
        }
        obj.resetBatchId();
        obj.resetRecordIndexId();
        obj.resetRecordCounter();
        obj.resetRecordKafkaCounter();

    }

    public void setCheckKeysInCSV(Exchange exchange) throws IOException {
        String logKeys = exchange.getProperty("LogKeys").toString();
        Map<String, Object> body = (Map<String, Object>) exchange.getIn().getBody();
        String jsonBody = mapper.writeValueAsString(body);
        Map<String, String> logMap = mapper.readValue(logKeys, Map.class);
        File csvFile = new File("Records.csv");
        FileWriter fw = new FileWriter(csvFile, true);

        Set<Entry<String, String>> entryset = logMap.entrySet();
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .build();
        // String pageIndex = String.valueOf(((Map<String,String>)
        // exchange.getProperty(ConfigConstants.Payload)).get("pageIndex")) ;
        List<Object> recordList = new ArrayList<>();
        // recordList.add(pageIndex);
        try (final CSVPrinter printer = new CSVPrinter(fw, csvFormat)) {
            for (Entry<String, String> entry : entryset) {
                try {
                    recordList.add(JsonPath.parse(jsonBody).read(entry.getValue()).toString());
                } catch (Exception e) {
                    recordList.add(" ");
                }
            }
            printer.printRecord(recordList);
        } catch (Exception e) {
            e.printStackTrace();
            ;
        }

    }

}
