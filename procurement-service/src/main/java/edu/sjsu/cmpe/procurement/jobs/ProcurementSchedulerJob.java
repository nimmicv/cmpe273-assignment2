package edu.sjsu.cmpe.procurement.jobs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.Connection;
import de.spinscale.dropwizard.jobs.Job;
import de.spinscale.dropwizard.jobs.annotations.Every;
import edu.sjsu.cmpe.procurement.ProcurementService;

import edu.sjsu.cmpe.procurement.domain.BookRequest;
import edu.sjsu.cmpe.procurement.domain.ShippedBook;
import edu.sjsu.cmpe.procurement.stomp.StompConfig;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

/**
 * This job will run at every 5 minutes or 300 second.
 */
@Every("300s")
public class ProcurementSchedulerJob extends Job {
    private final Logger log = LoggerFactory.getLogger(getClass());

//    @Override
//    public void doJob() {
//	String strResponse = ProcurementService.jerseyClient.resource(
//		"http://ip.jsontest.com/").get(String.class);
//	log.debug("Response from jsontest.com: {}", strResponse);
//    }
//    
    @Override
    public void doJob() {
	
            StompConfig stompInstance = new StompConfig();
            BookRequest bookRequest;
            Connection connection;
  
            try {
                    connection = stompInstance.makeConnection();
                        bookRequest = stompInstance.reveiveQueueMessage(connection);
                    connection.close();
            
                    if (bookRequest.getOrder_book_isbns().size() != 0){
                            System.out.println("HTTP POST to Publisher");
                        Client client = Client.create();
                        String url = "http://54.215.133.131:9000/orders";        
                            WebResource webResource = client.resource(url);
                            ClientResponse response = webResource.accept("application/json")
                                            .type("application/json").entity(bookRequest, "application/json").post(ClientResponse.class);              
                            System.out.println(response.getEntity(String.class));
                        }
                        } catch ( Exception e) {
                                e.printStackTrace();
                        }  
          
            try {
                    
                    Client client = Client.create();
                    String url = "http://54.215.133.131:9000/orders/34440";        
                    WebResource webResource = client.resource(url);
                    ShippedBook response = webResource.accept("application/json")
                                    .type("application/json").get(ShippedBook.class);
                    connection = stompInstance.makeConnection();
                    stompInstance.publishTopicMessage(connection, response);
                     
                        } catch ( Exception e) {
                                e.printStackTrace();
       }     
    }
}
