package in.nic.npi.process;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import com.fasterxml.jackson.databind.ObjectMapper;

import in.nic.npi.constants.CamelPropertiesConstants;

@SuppressWarnings("unchecked")
public class CreateKafkaBody implements Processor{

    ObjectMapper mapper = new ObjectMapper();
    @Override
    public void process(Exchange exchange) throws Exception {
       String recordMetadata = exchange.getProperty(CamelPropertiesConstants.Record_Metadata_Body,String.class);
       Map<String,Object> body = exchange.getIn().getBody(Map.class);
       Map<String,Object> recordMetadataMap = mapper.readValue(recordMetadata, Map.class);
       Map<String,Object> object = new HashMap<>();
       object.put("OutputTaskAquisition", body); 
       object.put("RecordMetaData", recordMetadataMap);
       String finalOutput = mapper.writeValueAsString(object);
       exchange.getIn().setBody(finalOutput);
    }
}
