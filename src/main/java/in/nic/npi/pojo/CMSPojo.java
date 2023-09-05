package in.nic.npi.pojo;

import com.fasterxml.jackson.annotation.JsonRootName;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonRootName(value = "Metadata")
public class CMSPojo {
    
    private ApiMetaData apiMetaData;
    private RecordMetaData recordMetadata;
    private CMSHeaders cmsHeaders;
    private BatchMetaData BatchMetaData;

}
