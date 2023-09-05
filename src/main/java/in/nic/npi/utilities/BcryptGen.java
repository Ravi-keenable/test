package in.nic.npi.utilities;

import java.time.Instant;

import org.apache.camel.Exchange;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.core5.http.HttpEntity;
import org.mindrot.jbcrypt.BCrypt;

public class BcryptGen {

    public void getHash(Exchange exchange) {
        // Generate timestamp
        // String timestamp = Long.toString(System.currentTimeMillis());
        long timestamp = Instant.now().getEpochSecond();
        String key = "NIC-Elastic-V1-mT=rK^T43E@p^PK";

        // Generate BCrypt password using timestamp
        String bcryptPassword = BCrypt.hashpw(String.valueOf(timestamp) + key, BCrypt.gensalt());

        // Concatenate timestamp and BCrypt password
        String response = String.valueOf(timestamp) + bcryptPassword;

        // Set the response as the body

        System.out.println(response);
        HttpEntity entity = MultipartEntityBuilder.create()
                .addTextBody("device_id", "1BC986C3-0B2E-4698-AE3986npi2")
                .addTextBody("password", response)
                .addTextBody("pushToken", "pushToken").build();

        exchange.getIn().setBody(entity);
    }
}
