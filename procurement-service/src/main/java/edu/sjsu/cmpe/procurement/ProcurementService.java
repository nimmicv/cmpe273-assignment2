package edu.sjsu.cmpe.procurement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.client.Client;
import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.client.JerseyClientBuilder;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Environment;

import de.spinscale.dropwizard.jobs.JobsBundle;
import edu.sjsu.cmpe.procurement.api.resources.RootResource;
import edu.sjsu.cmpe.procurement.config.ProcurementServiceConfiguration;
import edu.sjsu.cmpe.procurement.stomp.StompConfiguration;

public class ProcurementService extends Service<ProcurementServiceConfiguration> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * FIXME: THIS IS A HACK!
     */
    public static Client jerseyClient;

    public static void main(String[] args) throws Exception {
	new ProcurementService().run(args);
    }

    @Override
    public void initialize(Bootstrap<ProcurementServiceConfiguration> bootstrap) {
	bootstrap.setName("procurement-service");
	/**
	 * NOTE: All jobs must be placed under edu.sjsu.cmpe.procurement.jobs
	 * package
	 */
	bootstrap.addBundle(new JobsBundle("edu.sjsu.cmpe.procurement.jobs"));
    }

    @Override
    public void run(ProcurementServiceConfiguration configuration,
	    Environment environment) throws Exception {
	jerseyClient = new JerseyClientBuilder()
	.using(configuration.getJerseyClientConfiguration())
	.using(environment).build();
	// This is how you pull the configurations from library_x_config.yml
    StompConfiguration stompconfig= new StompConfiguration();
    stompconfig.setApolloUser(configuration.getApolloUser());
    stompconfig.setPassword(configuration.getApolloPassword());
    stompconfig.setPort(configuration.getApolloPort());
    stompconfig.setQueueName(configuration.getStompQueueName());
    stompconfig.setTopicName(configuration.getStompTopicPrefix());
    stompconfig.setHostName(configuration.getApolloHost());

	/**
	 * Root API - Without RootResource, Dropwizard will throw this
	 * exception:
	 * 
	 * ERROR [2013-10-31 23:01:24,489]
	 * com.sun.jersey.server.impl.application.RootResourceUriRules: The
	 * ResourceConfig instance does not contain any root resource classes.
	 */
	environment.addResource(RootResource.class);
	
	
	log.debug("Queue name is {}. Topic is {}", configuration.getStompQueueName(), configuration.getStompTopicName());
	// TODO: Apollo STOMP Broker URL and login

    }
}
