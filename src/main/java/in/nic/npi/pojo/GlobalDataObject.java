package in.nic.npi.pojo;

import java.util.Set;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GlobalDataObject {

    public GlobalDataObject(){
        this.keyUpdateCount = 0;
    }
    private Set<String> recordKeyList;
    private Integer keyUpdateCount ;
}
