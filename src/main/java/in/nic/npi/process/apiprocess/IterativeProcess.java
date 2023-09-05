package in.nic.npi.process.apiprocess;

import java.util.Map;

import org.apache.camel.Exchange;

import in.nic.npi.constants.CamelPropertiesConstants;
import in.nic.npi.constants.ConfigConstants;
import in.nic.npi.exception.InvalidOffsetException;

@SuppressWarnings("unchecked")
public class IterativeProcess {

    // independent is defined as no reliance on value from master
    public void createIndependentIterativeProcessPayload(Exchange exchange) throws InvalidOffsetException {
        String iterativeType = exchange.getProperty("IterativeType", String.class);
        if (iterativeType.equalsIgnoreCase("page")) {
            processPageIndexType(exchange);
        } else {
            processOffsetType(exchange);
        }

    }

    private void processPageIndexType(Exchange exchange) {
        boolean iteratesByHeader = Boolean
                .valueOf(exchange.getProperty(ConfigConstants.Iterates_By_Header, String.class));
        Map<String, String> iterativeKeys = (Map<String, String>) exchange.getProperty(ConfigConstants.Iterative_Keys);

        String pageKey = iterativeKeys.get("pageIndex");
        Map<String, Object> queryHeaders = exchange.getProperty(ConfigConstants.Query_Param, Map.class);
        Map<String, Object> pathHeaders = exchange.getProperty(ConfigConstants.Path_Param, Map.class);
        Integer increamenter = exchange.getProperty(CamelPropertiesConstants.Increamenter, Integer.class);

        // This block is for api which require a setting specific paramert in query
        // param to iterate current only query param is supported
        if (iteratesByHeader) {
            if (queryHeaders.containsKey(pageKey)) {
                if (increamenter == null) {
                    if (queryHeaders.get(pageKey) == null || queryHeaders.get(pageKey).toString().trim() == "") {
                        increamenter = 1;
                        queryHeaders.put(pageKey, increamenter);
                        exchange.setProperty(CamelPropertiesConstants.Increamenter, increamenter + 1);
                    } else {
                        String key = queryHeaders.get(pageKey).toString();
                        increamenter = Integer.parseInt(key);
                        queryHeaders.put(pageKey, increamenter);
                        exchange.setProperty(CamelPropertiesConstants.Increamenter, increamenter + 1);
                    }
                } else {
                    queryHeaders.put(pageKey, increamenter);
                    exchange.setProperty(CamelPropertiesConstants.Increamenter, increamenter + 1);

                }
            }

            else if (pathHeaders.containsKey(pageKey)) {
                if (increamenter == null) {
                    if (pathHeaders.get(pageKey) == null || pathHeaders.get(pageKey).toString().trim() == "") {
                        increamenter = 1;
                        pathHeaders.put(pageKey, increamenter);
                        exchange.setProperty(CamelPropertiesConstants.Increamenter, increamenter + 1);
                    } else {
                        String key = pathHeaders.get(pageKey).toString();
                        increamenter = Integer.parseInt(key);
                        pathHeaders.put(pageKey, increamenter);
                        exchange.setProperty(CamelPropertiesConstants.Increamenter, increamenter + 1);
                    }
                } else {
                    pathHeaders.put(pageKey, increamenter);
                    exchange.setProperty(CamelPropertiesConstants.Increamenter, increamenter + 1);

                }
            }

        }

        // iterate by payload
        else {

            Map<String, Object> payload = exchange.getProperty(ConfigConstants.Payload, Map.class);

            if (increamenter == null) {
                if (payload.get(pageKey) == null || payload.get(pageKey).toString().trim() == "") {
                    increamenter = 1;
                    payload.put(pageKey, increamenter);
                    exchange.setProperty(CamelPropertiesConstants.Increamenter, increamenter + 1);

                } else {
                    String key = payload.get(pageKey).toString();
                    increamenter = Integer.parseInt(key);
                    payload.put(pageKey, increamenter);
                    exchange.setProperty(CamelPropertiesConstants.Increamenter, increamenter + 1);
                }
            } else {
                payload.put(pageKey, increamenter);
                exchange.setProperty(CamelPropertiesConstants.Increamenter, increamenter + 1);
            }

        }

    }

    private void processOffsetType(Exchange exchange) throws InvalidOffsetException {
        boolean iteratesByHeader = Boolean
                .valueOf(exchange.getProperty(ConfigConstants.Iterates_By_Header, String.class));
        Map<String, String> iterativeKeys = (Map<String, String>) exchange.getProperty(ConfigConstants.Iterative_Keys);
        String startOffsetkey = iterativeKeys.get("startIndex");
        String limitKey = iterativeKeys.get("limit");

        Map<String, Object> queryHeaders = exchange.getProperty(ConfigConstants.Query_Param, Map.class);
        Map<String, Object> pathHeaders = exchange.getProperty(ConfigConstants.Path_Param, Map.class);
        Map<String, Object> payload = exchange.getProperty(ConfigConstants.Payload, Map.class);

        // This block is for api which require a setting specific paramert in query
        // param to iterate current only query param is supported
        if (iteratesByHeader) {
            if (queryHeaders.containsKey(startOffsetkey) && queryHeaders.containsKey(limitKey)) {
                setQueryOffset(exchange, queryHeaders);
            } else if (pathHeaders.containsKey(startOffsetkey) && pathHeaders.containsKey(limitKey)) {
                setPathOffset(exchange, pathHeaders);
            } else {
                throw new InvalidOffsetException();
            }
        } else if (payload.containsKey(startOffsetkey) && payload.containsKey(limitKey)) {
            setPayloadOffset(exchange, payload);
        } else {
            throw new InvalidOffsetException();
        }
    }

    private void setQueryOffset(Exchange exchange, Map<String, Object> queryHeaders) throws InvalidOffsetException {

        Map<String, String> iterativeKeys = (Map<String, String>) exchange.getProperty(ConfigConstants.Iterative_Keys);

        Integer startOffset = exchange.getProperty(CamelPropertiesConstants.START_OFFSET, Integer.class);
        Integer limitDiff = exchange.getProperty(CamelPropertiesConstants.OFFSET_LIMIT, Integer.class);

        String startOffsetkey = iterativeKeys.get("startIndex");
        String limitKey = iterativeKeys.get("limit");

        Integer configStartOffset;
        Integer configOffsetLimit;
        Integer configLimitDiff;

        if (limitDiff == null && startOffset == null) {
            try {
                configStartOffset = Integer.parseInt(queryHeaders.get(startOffsetkey).toString());
                configOffsetLimit = Integer.parseInt(queryHeaders.get(limitKey).toString());
                configLimitDiff = configOffsetLimit - configStartOffset;
                if (configOffsetLimit < 0 || configStartOffset < 0)
                    throw new InvalidOffsetException("Invalid Offset");
            } catch (Exception e) {
                throw new InvalidOffsetException("Invalid Offset");
            }
            queryHeaders.put(startOffsetkey, configStartOffset);
            queryHeaders.put(limitKey, configOffsetLimit);
            Integer newStartOffset = configLimitDiff + configStartOffset;
            exchange.setProperty(CamelPropertiesConstants.START_OFFSET, newStartOffset);
            exchange.setProperty(CamelPropertiesConstants.OFFSET_LIMIT, configLimitDiff);
        } else {
            queryHeaders.put(startOffsetkey, startOffset);
            queryHeaders.put(limitKey, limitDiff);
            Integer newStartOffset = limitDiff + startOffset;
            exchange.setProperty(CamelPropertiesConstants.START_OFFSET, newStartOffset);

        }
    }



    private void setPathOffset(Exchange exchange, Map<String, Object> pathHeaders) throws InvalidOffsetException {
        Map<String, String> iterativeKeys = (Map<String, String>) exchange.getProperty(ConfigConstants.Iterative_Keys);

        Integer startOffset = exchange.getProperty(CamelPropertiesConstants.START_OFFSET, Integer.class);
        Integer limitDiff = exchange.getProperty(CamelPropertiesConstants.OFFSET_LIMIT, Integer.class);

        String startOffsetkey = iterativeKeys.get("startIndex");
        String limitKey = iterativeKeys.get("limit");

        Integer configStartOffset;
        Integer configOffsetLimit;
        Integer configLimitDiff;

        if (limitDiff == null && startOffset == null) {
            try {
                configStartOffset = Integer.parseInt(pathHeaders.get(startOffsetkey).toString());
                configOffsetLimit = Integer.parseInt(pathHeaders.get(limitKey).toString());
                configLimitDiff = configOffsetLimit - configStartOffset;
                if (configOffsetLimit < 0 || configStartOffset < 0)
                    throw new InvalidOffsetException("Invalid Offset");
            } catch (Exception e) {
                throw new InvalidOffsetException("Invalid Offset");
            }
            pathHeaders.put(startOffsetkey, configStartOffset);
            pathHeaders.put(limitKey, configOffsetLimit);
            Integer newStartOffset = configLimitDiff + configStartOffset;
            exchange.setProperty(CamelPropertiesConstants.START_OFFSET, newStartOffset);
            exchange.setProperty(CamelPropertiesConstants.OFFSET_LIMIT, configLimitDiff);
        } else {
            pathHeaders.put(startOffsetkey, startOffset);
            pathHeaders.put(limitKey, limitDiff);
            Integer newStartOffset = limitDiff + startOffset;
            exchange.setProperty(CamelPropertiesConstants.START_OFFSET, newStartOffset);
        }
    }

    private void setPayloadOffset(Exchange exchange, Map<String, Object> payload) throws InvalidOffsetException {
        Map<String, String> iterativeKeys = (Map<String, String>) exchange.getProperty(ConfigConstants.Iterative_Keys);

        Integer startOffset = exchange.getProperty(CamelPropertiesConstants.START_OFFSET, Integer.class);
        Integer limitDiff = exchange.getProperty(CamelPropertiesConstants.OFFSET_LIMIT, Integer.class);

        String startOffsetkey = iterativeKeys.get("startIndex");
        String limitKey = iterativeKeys.get("limit");

        Integer configStartOffset;
        Integer configOffsetLimit;
        Integer configLimitDiff;

        if (limitDiff == null && startOffset == null) {
            try {
                configStartOffset = Integer.parseInt(payload.get(startOffsetkey).toString());
                configOffsetLimit = Integer.parseInt(payload.get(limitKey).toString());
                configLimitDiff = configOffsetLimit - configStartOffset;
                if (configOffsetLimit < 0 || configStartOffset < 0)
                    throw new InvalidOffsetException("Invalid Offset");
            } catch (Exception e) {
                throw new InvalidOffsetException("Invalid Offset");
            }
            payload.put(startOffsetkey, configStartOffset);
            payload.put(limitKey, configOffsetLimit);
            Integer newStartOffset = configLimitDiff + configStartOffset;
            exchange.setProperty(CamelPropertiesConstants.START_OFFSET, newStartOffset);
            exchange.setProperty(CamelPropertiesConstants.OFFSET_LIMIT, configLimitDiff);
        } else {
            payload.put(startOffsetkey, startOffset);
            payload.put(limitKey, limitDiff);
            Integer newStartOffset = limitDiff + startOffset;
            exchange.setProperty(CamelPropertiesConstants.START_OFFSET, newStartOffset);

        }
    }

}
