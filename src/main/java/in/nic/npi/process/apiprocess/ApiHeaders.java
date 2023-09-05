package in.nic.npi.process.apiprocess;


import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.camel.Exchange;


@SuppressWarnings("unchecked")
public class ApiHeaders{
    

    public void setRequiredHeaders(Exchange exchange){

        Map<String,Object> pathHeaders = (Map<String, Object>) exchange.getProperty("PathParam");
        Map<String,Object> queryHeaders = (Map<String, Object>) exchange.getProperty("QueryParam");
        Map<String,Object> headerMap = exchange.getIn().getHeaders();
        if (pathHeaders != null) {
            setHeadersMap(pathHeaders,headerMap);

        }
        if (queryHeaders != null) {
                String query = "";
                Set<Entry<String, Object>> entries = queryHeaders.entrySet();
                for (Entry<String,Object> entry : entries) {
                    query = query + entry.getKey() + "=" + entry.getValue().toString() + "&";
                }
                String finalQuery = query.substring(0, query.length()-1);
                exchange.getIn().setHeader(Exchange.HTTP_QUERY, finalQuery);
            
        }

        exchange.getIn().setHeaders(headerMap);
        

    }
    private void setHeadersMap(Map<String,Object> object,Map<String,Object> headerMap){
        if (object != null) {
            headerMap.putAll(object);
        }
    }

    public void setAuthHeaders(Exchange exchange){
        Map<String,Object> authHeaderMap = ( Map<String,Object>) exchange.getIn().getBody();
        Map<String,Object> headers = exchange.getIn().getHeaders();
        headers.putAll(authHeaderMap);
        exchange.getIn().setHeaders(headers);
    }
    
}
