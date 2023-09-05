package in.nic.npi.process.metadata;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Set;

import org.apache.camel.Exchange;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import in.nic.npi.constants.CamelPropertiesConstants;
import in.nic.npi.constants.HardcodedValues;
import in.nic.npi.pojo.ApiMetaData;
import in.nic.npi.pojo.BatchMetaData;
import in.nic.npi.pojo.CMSPojo;
import in.nic.npi.pojo.GlobalDataObject;
import in.nic.npi.pojo.RecordMetaData;
import in.nic.npi.utilities.CounterClass;
import in.nic.npi.utilities.ProcessRecord;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SuppressWarnings("unchecked")
public class SetMetadata {
    public final ObjectMapper mapper = new ObjectMapper();
    public final ProcessRecord keyListGen = new ProcessRecord();

    public static int batchCapacity = 0;

    public void setExtApiMetadata(Exchange exchange) throws JsonProcessingException {
        ApiMetaData apiMetaData = new ApiMetaData();
        CounterClass obj = exchange.getProperty("RECORD_OBJECT", CounterClass.class);
        GlobalDataObject globalDataObject = (GlobalDataObject) exchange.getProperty("GLOBAL_OBJECT");

        String apiId = exchange.getProperty(CamelPropertiesConstants.API_REQUEST_ID, String.class);

        apiMetaData.setMeta_id(apiId);
        apiMetaData.setDatasource(exchange.getProperty(CamelPropertiesConstants.ACQUISITION_API));
        apiMetaData.setDatasourceType(exchange.getProperty(CamelPropertiesConstants.ENDPOINT_NAME));
        apiMetaData.setMaster(null);
        apiMetaData.setLastHitStatusCode(exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE));
        apiMetaData.setSourceUrl(exchange.getProperty(CamelPropertiesConstants.EXTERNAL_API_URL));
        apiMetaData.setTotalBatchCount(obj.BATCH_ID_INDEX);
        apiMetaData.setTotalRecordCount(obj.RECORD_COUNTER);
        apiMetaData.setTotalRetrievedRecords(obj.TOTAL_RECORD_KAFKA_COUNTER);
        apiMetaData.setTotalDuplicateRecords(obj.RECORD_COUNTER - obj.TOTAL_RECORD_KAFKA_COUNTER);
        apiMetaData.setRecordKeyList(globalDataObject.getRecordKeyList());
        exchange.setProperty(CamelPropertiesConstants.API_Metadata_Body, apiMetaData);
    }

    public void setBatchMetadata(Exchange exchange) throws JsonProcessingException, NoSuchAlgorithmException {
        List<Object> recordList = exchange.getProperty(CamelPropertiesConstants.Ext_Api_Record_List, List.class);
        CMSPojo metadata = exchange.getProperty(CamelPropertiesConstants.Metadata_Body, CMSPojo.class);
        BatchMetaData batchMetaData = new BatchMetaData();
        String extApiBody = exchange.getIn().getBody(String.class);
        CounterClass obj = exchange.getProperty("RECORD_OBJECT", CounterClass.class);

        byte[] extApiBodyByte = ((String) extApiBody).getBytes();
        int size = extApiBodyByte.length;

        if (obj.BATCH_ID_INDEX == 0)
            batchCapacity = recordList.size();

        String apiId = exchange.getProperty("API_REQUEST_ID", String.class);
        String batchId = apiId + "-" + "Batch" + "-" + obj.BATCH_ID_INDEX;
        String batchHash = calcSHA((String) extApiBody);

        batchMetaData.setMeta_id(apiId);
        batchMetaData.setBatch_id(batchId);
        batchMetaData.setBatchCapacity(batchCapacity);
        batchMetaData.setBatchHash(batchHash);
        batchMetaData.setBatchDataSize(size);
        batchMetaData.setBatchRetrievedRecordCount(recordList.size());

        metadata.setBatchMetaData(batchMetaData);

        exchange.setProperty(CamelPropertiesConstants.Batch_Metadata_Body, batchMetaData);
        exchange.setProperty(CamelPropertiesConstants.Global_Batch_ID, batchId);
        log.info("Processing BatchID count {} with Capacity {}", obj.BATCH_ID_INDEX, recordList.size());
        obj.increamentBatchId();

    }

    public void setRecordMetadata(Exchange exchange) throws UnsupportedEncodingException, JsonMappingException,
            JsonProcessingException, NoSuchAlgorithmException {

        log.info("Inside SetMetadata Process setRecordMetadata");
        // Record Body is of map type
        CounterClass obj = exchange.getProperty("RECORD_OBJECT", CounterClass.class);
        GlobalDataObject globalDataObject = (GlobalDataObject) exchange.getProperty("GLOBAL_OBJECT");


        Object record = exchange.getIn().getBody();
        
        Set<String> keylist = keyListGen.getRecordKeys(record);
        globalDataObject.getRecordKeyList().addAll(keylist);

        String recordJson = mapper.writeValueAsString(record);
        // Property is stored as string need to convert it to map to add values
        RecordMetaData recordMetaData = new RecordMetaData();
        String apiId = exchange.getProperty(CamelPropertiesConstants.API_REQUEST_ID, String.class);
        // Record Metadata
        byte[] recordByte = recordJson.getBytes();
        int recordSize = recordByte.length;
        String globalBatchId = exchange.getProperty(CamelPropertiesConstants.Global_Batch_ID, String.class);
        String recId = globalBatchId + "-" + "Record" + "-"
                + obj.RECORD_ID_INDEX;
        String hash = calcSHA(recordJson);

        recordMetaData.setMeta_id(apiId);
        recordMetaData.setBatch_id(globalBatchId);
        recordMetaData.setCreated_at(Instant.now().toString());
        recordMetaData.setRecordDataSize(recordSize);
        recordMetaData.setRecordHash(hash);
        recordMetaData.setRecord_id(recId);
        recordMetaData.setWorkflow_state(new ArrayList<>().add(HardcodedValues.WORKFLOW_STATE));
        recordMetaData.setRecordBody(recordJson);
        exchange.setProperty(CamelPropertiesConstants.Record_Hash, hash);
        exchange.setProperty(CamelPropertiesConstants.Record_Metadata_Body, mapper.writeValueAsString(recordMetaData));
    }

    public String calcSHA(String input) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] result = md.digest(input.getBytes());
        return new String(Base64.getEncoder().encode(result));

    }
}
