package in.nic.npi.pojo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BatchMetaData {
    public Object meta_id;
    public Object batch_id;
    public Object batchRetrievedRecordCount;
    public Object batchDataSize;
    public Object batchCapacity;
    public Object batchHash;
}
