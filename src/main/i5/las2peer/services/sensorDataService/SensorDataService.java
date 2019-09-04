package i5.las2peer.services.sensorDataService;

import java.io.Serializable;
import java.net.HttpURLConnection;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import i5.las2peer.api.Context;
import i5.las2peer.api.ManualDeployment;
import i5.las2peer.api.execution.InternalServiceException;
import i5.las2peer.api.execution.ServiceAccessDeniedException;
import i5.las2peer.api.execution.ServiceInvocationFailedException;
import i5.las2peer.api.execution.ServiceMethodNotFoundException;
import i5.las2peer.api.execution.ServiceNotAuthorizedException;
import i5.las2peer.api.execution.ServiceNotAvailableException;
import i5.las2peer.api.execution.ServiceNotFoundException;
import i5.las2peer.restMapper.RESTService;
import i5.las2peer.restMapper.annotations.ServicePath;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Contact;
import io.swagger.annotations.Info;
import io.swagger.annotations.SwaggerDefinition;
import net.minidev.json.JSONObject;

/**
 * Sensor-Data-Service
 * 
 * This is a service for the connection between the ARLEM services database and Learning Locker.
 * This service connects with the MySQL database of the ARLEM services, which is responsible for
 * for storing learner traces measured by the multisensor data fusion framework.
 * Then identifies and retrieves the data important for the analytics part and then based on that 
 * creates xAPI statements and sends them to an LRS
 */
@Api
@SwaggerDefinition(
		info = @Info(
				title = "Sensor-Data-Service",
				version = "1.0",
				description = "A service for the connection between the ARLEM services database and Learning Lockers.",
				contact = @Contact(
						name = "Philipp Roytburg",
						email = "philipp.roytburg@rwth-aachen.de")))

@ManualDeployment
@ServicePath("/sensor")
public class SensorDataService extends RESTService {

	private String mysqlUser;
	private String mysqlPassword;
	private String mysqlHost;
	private String mysqlPort;
	private String mysqlDatabase;
	
	private Connection con;
	
	/**
	 * 
	 * Constructor of the Service. Sets values of the configuration file.
	 * 
	 */
	public SensorDataService() {
		setFieldValues();
		
	}
	
	
	/**
	 * A function that is called by the user to send sensor data to the LRS 
	 * It firsts tries to connect to the database and then gets the statements and sends them to an LRS
	 * 
	 * @return a response message if everything went ok
	 * 
	 */
	@POST
	@Path("/sendData")
	@Produces(MediaType.TEXT_PLAIN)
	@ApiResponses(
			value = { @ApiResponse(
					code = HttpURLConnection.HTTP_OK,
					message = "Moodle connection is initiaded") })
	public Response sendSensorData() {
		// connect with the database
		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
			con=DriverManager.getConnection("jdbc:mysql://" + mysqlHost + ":" + mysqlPort + "/" + mysqlDatabase,
					mysqlUser, mysqlPassword);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ArrayList<String> newstatements = getSensorData();
		try {
                Context.get().invoke("i5.las2peer.services.learningLockerService.LearningLockerService@1.0", "sendXAPIstatement", 
                        (Serializable) newstatements);
            } catch (ServiceNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (ServiceNotAvailableException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (InternalServiceException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (ServiceMethodNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (ServiceInvocationFailedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (ServiceAccessDeniedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (ServiceNotAuthorizedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }


		try {
			con.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return Response.ok().entity("Sensor data was sent to LRS").build();
	}
	
	/**
	 * This function is called by the sendSensorData function. 
	 * It tries to an sql query to get the data from the database 
	 * and then calls statementGenerator function to create statements out of the data.
	 * 
	 * @return an Arraylist of statements
	 * 
	 */
	private ArrayList<String> getSensorData() {
		ArrayList<String> newstatements = new ArrayList<String>();
		try {
			String query = "select b.personName, b.personMbox, actions.id as actionId, actions.name as actionName, actions.instructionDescription "
					+ "from actions "
					+ "join ( "
					+ "select a.personName, a.personMbox, action_triggers.action "
					+ "from action_triggers "
					+ "join ( "
					+ "select persons.name as personName, persons.mbox as personMbox, action_trigger_operations.actionTrigger "
					+ "from persons "
					+ "join action_trigger_operations "
					+ "on action_trigger_operations.entityId = persons.id "
					+ "where action_trigger_operations.entityType = 'Person' ) "
					+ "as a on a.actionTrigger = action_triggers.id ) "
					+ "as b on b.action = actions.id;";
			Statement stmt=con.createStatement();
			ResultSet rs=stmt.executeQuery(query);
			while(rs.next()) {
				String personName = rs.getString("personName");
				String personMbox = rs.getString("personMbox");
				String actionId = rs.getString("actionId");
				String actionName = rs.getString("actionName");
				String instructionDescription = rs.getString("instructionDescription");
				
				String statement = statementGenerator(personName, personMbox, actionId, actionName, instructionDescription);
				
				newstatements.add(statement.toString());
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return newstatements;
	}
	
	/**
	 * This function is called by the getSensorData function. 
	 * It gets the relevant data to create an xAPI statement out of Sensor data 
	 * and creates a statement as a JSONObject.
	 *
	 * @param personName the name of a person who performed an action
	 * @param personMbox the email of a person who performed an action
	 * @param actionId the id of an action that was performed
	 * @param actionName the name of an action that was performed
	 * @param instructionDescription the instruction description of an action that was performed
	 * 
	 * @return an xAPI statement in string format
	 * 
	 */
	private String statementGenerator(String personName, String personMbox, String actionId, String actionName, String instructionDescription) {
		JSONObject statement = new JSONObject();
		
		JSONObject actor = new JSONObject();
		actor.put("name", personName);
		actor.put("mbox", "mailto:" + personMbox);
		statement.put("actor", actor);
		
		JSONObject verb = new JSONObject();
		verb.put("id", "http://example.com/xapi/performed");
		JSONObject display = new JSONObject();
		display.put("en-US", "performed");
		verb.put("display", display);
		statement.put("verb", verb);
		
		JSONObject object = new JSONObject();
		object.put("id", "http://example.com/actionId=" + actionId);
		JSONObject definition = new JSONObject();
		JSONObject name = new JSONObject();
		name.put("en-US", actionName);
		definition.put("name", name);
		if (instructionDescription != null && !instructionDescription.equals("")) {
			JSONObject description = new JSONObject();
			description.put("en-US", instructionDescription);
			definition.put("description", description);
		}
		object.put("definition", definition);
		statement.put("object", object);
		
		//TODO if you find results
		/*
		JSONObject result = new JSONObject();
		JSONObject score = new JSONObject();
		score.put("score", value)
		*/
		
		return statement.toString();
	}

}
