package in.nic.npi.pojo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ApiMetaData {
    public Object meta_id;
    public Object totalRecordCount;
    public Object sourceUrl;
    public Object recordKeyList;
    public Object lastHitStatusCode;
    public Object recordKeyCount;
    public Object datasource;
    public Object datasourceType;
    public Object totalRetrievedRecords;
    public Object totalUniqueRecords;
    public Object totalDuplicateRecords;
    public Object totalBatchCount;
    public Object remarks;
    public Object master;
}
