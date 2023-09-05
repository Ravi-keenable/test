package in.nic.npi.pojo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RecordMetaData {

    public Object meta_id;
    public Object batch_id;
    public Object record_id;
    public Object workflow_state;
    public Object updated_by;
    public Object created_at;
    public Object updated_at;
    public Object archival_date;
    public Object recordDataSize;
    public Object recordHash;
    public Object requireCMSVerification;
    public Object urlVerifyStatus;
    public Object recordBody;

}
