package in.nic.npi;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.infinispan.remote.InfinispanRemoteConfiguration;
import org.apache.camel.component.infinispan.remote.InfinispanRemoteIdempotentRepository;

import org.apache.camel.model.dataformat.JsonLibrary;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.infinispan.client.hotrod.DefaultTemplate;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.commons.marshall.UTF8StringMarshaller;
import org.infinispan.protostream.SerializationContextInitializer;
import org.jboss.logmanager.MDC;

import in.nic.npi.constants.CamelPropertiesConstants;
import in.nic.npi.constants.ConfigConstants;
import in.nic.npi.exception.EndOfDataException;
import in.nic.npi.pojo.CMSPojo;
import in.nic.npi.pojo.GlobalDataObject;
import in.nic.npi.process.CreateKafkaBody;
import in.nic.npi.process.GeneralProcess;
import in.nic.npi.process.apiprocess.ApiHeaders;
import in.nic.npi.process.apiprocess.IterativeProcess;
import in.nic.npi.process.metadata.SetMetadata;
import in.nic.npi.process.properties.SetPropertiesBean;
import in.nic.npi.utilities.BcryptGen;
import in.nic.npi.utilities.CounterClass;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DataAcquisationRoute extends RouteBuilder {

        @ConfigProperty(name = "EXTERNAL_API_URL")
        String extApiUrl;
        @ConfigProperty(name = "POD_NAME")
        String podName;
        @ConfigProperty(name = "ENDPOINT_NAME")
        String endpointName;
        @ConfigProperty(name = "API_NAME")
        String apiName;
        @ConfigProperty(name = "CMS_LAYOUT")
        String cmsheaders;
        @ConfigProperty(name = "CONFIG_PAYLOAD")
        String payload;
        @ConfigProperty(name = "INFINISPAN_URL")
        String infinispanUrl;
        @ConfigProperty(name = "INFINISPAN_USERNAME")
        String infinispanUsername;
        @ConfigProperty(name = "INFINISPAN_PASSWORD")
        String infinispanPassword;
        @ConfigProperty(name = "IDEMPOTENT_CACHE")
        String idempotentCache;

        String checkLogKeys;

        @Override
        public void configure() throws Exception {

                checkLogKeys = new String(Files.readAllBytes(Paths.get("LogCheckKeys.json")));
                payload = new String(Files.readAllBytes(Paths.get(payload)));

                CamelContext context = getCamelContext();
                context.setStreamCaching(true);
                InfinispanRemoteConfiguration conf = new InfinispanRemoteConfiguration();
                conf.setHosts(infinispanUrl);
                conf.setUsername(infinispanUsername);
                conf.setPassword(infinispanPassword);
                conf.setSecure(true);
                conf.setSaslMechanism("SCRAM-SHA-256");
                URI uri = new URI("hotrod://" + infinispanUsername + ":" + infinispanPassword + "@" + infinispanUrl);
                RemoteCacheManager cacheManager = new RemoteCacheManager(uri);
                cacheManager.administration().getOrCreateCache(idempotentCache, DefaultTemplate.LOCAL);
                cacheManager.getMarshallerRegistry().registerMarshaller(new UTF8StringMarshaller());
                cacheManager.getMarshallerRegistry().getMarshaller(MediaType.TEXT_PLAIN).start();

                InfinispanRemoteIdempotentRepository repo = new InfinispanRemoteIdempotentRepository(idempotentCache);
                repo.setCacheContainer(cacheManager);
                repo.setConfiguration(conf);
                repo.setCamelContext(context);

                rest("/independent")
                                .post("/processData")
                                .to("direct:processData")
                                // .to("direct:LoadConfigFromDb")
                                .get()
                                .to("direct:bcrypt");

                onException(Exception.class)
                                .log("${body}")
                                .to("direct:CreateResponse");

                from("direct:LoadConfigFromDb")
                                .setBody().jsonpath("$.api_name")
                                .transform().simple("{{ACQUISATION_CONFIG_QUERY}}")
                                .toD("jdbc:camel")
                                .log("${body}");

                from("direct:processData").routeId("ProcessData")
                                .setProperty("RECORD_OBJECT", constant(new CounterClass()))
                                .setProperty("GLOBAL_OBJECT", constant(new GlobalDataObject()))

                                .log("${exchangeProperty.RECORD_OBJECT}")
                                .threads()
                                .setProperty("LogKeys", constant(checkLogKeys))
                                .setProperty("API_REQUEST_ID", simple("{{ID_PREFIX}}-${uuid:classic}"))
                                .setProperty("EXTERNAL_API_URL", constant(extApiUrl))
                                .setProperty("ENDPOINT_NAME", constant(endpointName))
                                .setProperty("API_NAME", constant(apiName))

                                .removeHeaders("*")
                                .process(e -> {
                                        MDC.put("EXTERNAL_API_URL", extApiUrl);
                                        MDC.put("POD_NAME", podName);
                                        MDC.put("API_NAME", apiName);
                                        MDC.put("API_REQUEST_ID", e.getProperty("API_REQUEST_ID").toString());
                                })
                                // set input configuration from file will change in future to fetch from cms
                                .setBody().constant(payload)
                                // This parameter is needed to set auth headers by checking if its swaas or igod

                                .log(LoggingLevel.INFO,
                                                " Data Acquisation Started ")

                                // Set configuration values from body in camel property
                                .bean(SetPropertiesBean.class, "setProperties")

                                // body before going through processing
                                .log(LoggingLevel.DEBUG,
                                                " Body - ${body}")
                                .log(LoggingLevel.INFO,
                                                " Checking configuration value IsDependent")

                                // Check configuration property IsDependent which specifies if api requires
                                // attributes from master or not
                                .choice()
                                .when(simple("${exchangeProperty." + ConfigConstants.Is_Iterative + "} == false"))
                                .to("direct:IndependentCallApi")
                                .otherwise()
                                .to("direct:IndependentIterativeCallApi")
                                .end()

                                .to("direct:CreateResponse")

                ;

                from("direct:IndependentIterativeCallApi")
                                .bean(ApiHeaders.class, "setRequiredHeaders")
                                .setProperty(CamelPropertiesConstants.Iterative_Flag, constant(true))

                                .loopDoWhile(simple("${exchangeProperty." + CamelPropertiesConstants.Iterative_Flag
                                                + "} == true"))
                                .choice().when(simple("${exchangeProperty.AuthParam} != null"))
                                .to("direct:SetAuthHeaders")
                                .bean(IterativeProcess.class, "createIndependentIterativeProcessPayload")
                                .to("direct:ExtApiCall")
                                .end();

                from("direct:IndependentCallApi")
                                .bean(ApiHeaders.class, "setRequiredHeaders")
                                .choice().when(simple("${exchangeProperty.AuthParam} != null"))
                                .to("direct:SetAuthHeaders")
                                .to("direct:ExtApiCall")
                                .end();

                from("direct:ExtApiCall")

                                .setBody().exchangeProperty(ConfigConstants.Payload)
                                .marshal().json(JsonLibrary.Jackson)
                                .setHeader("CamelHttpMethod",
                                                simple("${exchangeProperty." + ConfigConstants.Api_Request_Method
                                                                + "}"))
                                .setHeader("Content-Type", simple("application/json"))
                                .bean(ApiHeaders.class, "setRequiredHeaders")

                                .log("${body}")
                                .log("${headers}")
                                .doTry()
                                // Calling external swaas api
                                .toD("{{EXTERNAL_API_URL}}")
                                .setProperty(CamelPropertiesConstants.Metadata_Body, constant(new CMSPojo()))
                                .convertBodyTo(String.class)
                                .setProperty(CamelPropertiesConstants.Ext_Api_Output, simple("${body}"))

                                .bean(GeneralProcess.class, "kafkaStorageProcess")
                                .doCatch(EndOfDataException.class)
                                .log(LoggingLevel.ERROR, "${body}")
                                .to("direct:CreateResponse")
                                .endDoTry()
                                .end()
                                .bean(SetMetadata.class, "setBatchMetadata")
                                .wireTap("direct:StoreBatchMetadata")
                                .log(LoggingLevel.DEBUG,
                                                "${exchangeProperty.POD_NAME} - ${exchangeProperty.ENDPOINT_NAME} - Response Body for external api call - ${body}")
                                .to("direct:SplitAndStoreInKafka")
                                .setBody().constant(null)

                ;

                from("direct:SplitAndStoreInKafka")
                                .setBody().exchangeProperty(CamelPropertiesConstants.Ext_Api_Record_List)
                                .split().body().streaming().shareUnitOfWork()
                                .removeHeaders("*")
                                .bean(SetMetadata.class, "setRecordMetadata")

                                .choice()
                                .when(simple("${exchangeProperty." + ConfigConstants.Unique_Record_Key_Path
                                                + "} != null"))
                                .setHeader("uniqueId", jsonpath("${exchangeProperty."+ConfigConstants.Unique_Record_Key_Path+"}"))
                                .endChoice()
                                .otherwise()
                                .setHeader("uniqueId",
                                                simple("${exchangeProperty." + CamelPropertiesConstants.Record_Hash
                                                                + "}"))
                                .end()
                                .process(e -> {
                                        CounterClass obj = e.getProperty("RECORD_OBJECT", CounterClass.class);
                                        obj.increamentRecordCounter();
                                        obj.increamentRecordId();

                                })
                                .idempotentConsumer(simple("${header.uniqueId}"), repo)
                                .bean(SetMetadata.class, "setRecordMetadata")

                                .wireTap("direct:CreateAuditCSV")
                                .process(new CreateKafkaBody())

                                .toD("kafka:{{ACQUISATION_STORAGE_TOPIC}}?brokers={{KAFKA_URL}}&partitionKey=0")
                                // increament a counter after body is created successfully
                                .process(e -> {
                                        CounterClass obj = e.getProperty("RECORD_OBJECT", CounterClass.class);

                                        obj.increamentRecordKafkaCounter();
                                })
                                .end()

                                .process(e -> {
                                        CounterClass obj = e.getProperty("RECORD_OBJECT", CounterClass.class);
                                        obj.resetRecordIndexId();
                                })

                ;

                from("direct:CreateResponse")
                                .to("direct:StoreApiMetadata")
                                .bean(GeneralProcess.class, "endOfDataBody")
                                .log(LoggingLevel.INFO,
                                                " Data Acquisation Ended ")
                                .stop();

                from("direct:StoreApiMetadata")
                                .bean(SetMetadata.class, "setExtApiMetadata")
                                .setBody().exchangeProperty(CamelPropertiesConstants.API_Metadata_Body)
                                .marshal().json(JsonLibrary.Jackson)
                                .removeHeaders("*")
                                .log(LoggingLevel.INFO, "Adding Metadata to Kafka")
                                // can be changed to db or somewhere else
                                .toD("kafka:{{METADATA_TOPIC}}?brokers={{KAFKA_URL}}")

                ;

                from("direct:StoreBatchMetadata")
                                .setBody().exchangeProperty(CamelPropertiesConstants.Batch_Metadata_Body)
                                .marshal().json(JsonLibrary.Jackson)
                                .removeHeaders("*")
                                .log(LoggingLevel.INFO, "Adding Batch Metadata to Kafka")
                                .toD("kafka:{{BATCH_METADATA_TOPIC}}?brokers={{KAFKA_URL}}")

                ;

                from("direct:SetAuthHeaders")
                                // send body to auth token api to get back headers in this format
                                .choice()
                                .when(simple("${exchangeProperty.ENDPOINT_NAME} == 'swaas'"))
                                .to("direct:bcrypt")
                                .process(e -> {
                                        Map<String, Object> map = new HashMap<>();
                                        map.put("Authorization", e.getIn().getBody());
                                        e.getIn().setBody(map);
                                })
                                .bean(ApiHeaders.class, "setAuthHeaders")
                                .when(simple("${exchangeProperty.ENDPOINT_NAME} == 'cms'"))
                                .setBody()
                                .constant("{\"Authorization\":\"Bearer 427f442482358e64e9c088bbc2a971c54f4235e4008cd36262aa54b41e605f58dbb82e1632e18d2e4d076989ff80ed13e43091b8dcefef97b05ac42d11d2171194860b86c707ba2bfdeeca06d76665d80a7bdd942461070da7d252c1afef4971b25c965e5a3de1cf2acf23d1645db524b1924d5aa6f58bf58c739373f5d36697\"}")
                                .unmarshal().json(Map.class)
                                .bean(ApiHeaders.class, "setAuthHeaders")
                                .otherwise()
                                .setBody().constant("{\"X-API-KEY\":\"a4Gc2d2SHWcJ79j1+dqszPd/L4jKXG3Yc3Ck2TLdwDo=\"}")
                                .unmarshal().json(Map.class)
                                .bean(ApiHeaders.class, "setAuthHeaders");
                // Auth headers from external Api set
                ;

                from("direct:bcrypt")
                                .bean(BcryptGen.class, "getHash")
                                .log("${body}")
                                .removeHeaders("Camel*")
                                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                                .setHeader("Content-Type", constant("application/json"))
                                .to("https://api.s3waas.gov.in/api/v1/login")
                                .setBody().jsonpath("$.token")
                                .transform().simple("Bearer ${body}");

                from("direct:CreateAuditCSV")
                                .bean(GeneralProcess.class, "setCheckKeysInCSV");

        }
}
