package com.restApi.stepDefinitions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.Assert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import lombok.var;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.http.Header;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class UserDefinedStepDefinitions {

	public static RequestSpecification requestSpec;
	public static int orderId;
	public static int petId;
	public static String userName;
	public static Response response;
	public static String requestBody;
	public static ArrayList<Response> responseMapList = new ArrayList<Response>();
	public static ArrayList<HashMap<Object, Object>> requestMapList = new ArrayList<HashMap<Object, Object>>();
	protected static final Logger log = LogManager.getLogger();
	

	@Given("^I Perform get operation for (login|logout) url$")
	public void performGetOperation(String url, DataTable params) throws Throwable {
		log.info("Executing Step - I Perform get operation for "+url);
		url = "/user/"+url;
		Response response;
		List<Map<String, String>> data = params.asMaps(String.class, String.class);
		Map<String, String> dataMap = data.get(0);
		System.out.println(dataMap);
		if(url.contains("logout")) {
			response = requestSpec.get(url);
		}else {
			response = getWithQueryParams(url, dataMap);
		}
		log.info(response.asPrettyString());
		log.info("End of Step - I Perform get operation for "+url);
	}



	@Given("^I create a new (store order|single user|multiple user|pet) with url \"([^\"]*)\" and (.*) variable$")
	public void performCreateOperation( String creationType, String url, String variableName, DataTable table) throws Throwable {
		Response response;
		//String requestBody;
		this.requestBody = "";
		List<Map<String, String>> createStoreMap =table.transpose().asMaps();
		if(!creationType.equalsIgnoreCase("pet")) {
		HashMap<Object, Object> updatedDataMap = new HashMap<Object, Object>();
		ArrayList<HashMap<Object, Object>> dataMapList = new ArrayList<HashMap<Object, Object>>();
		for(Map<String, String> map : createStoreMap) {
			updatedDataMap=generateDataFromHashMap(map);
			dataMapList.add(updatedDataMap);
			
		}

		if(dataMapList.size()>1) {
			List<JSONObject> jsonObj = new ArrayList<JSONObject>();

			for(HashMap<Object, Object> data : dataMapList) {
				JSONObject obj = new JSONObject(data);
				jsonObj.add(obj);
				requestMapList.add(data);
			}

			/*
			 * JSONArray test = new JSONArray(); test.add(jsonObj);
			 */
			this.requestBody=jsonObj.toString();
			log.info("****************************************************************************");
			log.info("Request JSON created for CreateUserList operation is " + jsonObj.toString());
			log.info("****************************************************************************");
		}else {
			JSONObject json =  new JSONObject(updatedDataMap);
			this.requestBody=json.toString();
			log.info("****************************************************************************");
			log.info("Request JSON created for Create" +creationType+" operation is " + this.requestBody);
			log.info("****************************************************************************");
		}
		}else {
			HashMap<Object, Object> updatedDataMap = new HashMap<Object, Object>();
			updatedDataMap=generateDataFromHashMap(createStoreMap.get(0));
			String requestBody = "{\r\n" + 
					"  \"id\": "+updatedDataMap.get("id")+",\r\n" + 
					"  \"name\": \""+updatedDataMap.get("name")+"\",\r\n" + 
					"  \"category\": {\r\n" + 
					"    \"id\": "+updatedDataMap.get("category_id")+",\r\n" + 
					"    \"name\": \""+updatedDataMap.get("category_name")+"\"\r\n" + 
					"  },\r\n" + 
					"  \"photoUrls\": [\r\n" + 
					"    \""+updatedDataMap.get("photoUrls")+"\"\r\n" + 
					"  ],\r\n" + 
					"  \"tags\": [\r\n" + 
					"    {\r\n" + 
					"      \"id\": "+updatedDataMap.get("tags_id")+",\r\n" + 
					"      \"name\": \""+updatedDataMap.get("tags_name")+"\"\r\n" + 
					"    }\r\n" + 
					"  ],\r\n" + 
					"  \"status\": \""+updatedDataMap.get("status")+"\"\r\n" + 
					"}";
			
			this.requestBody = requestBody;
		}
		response = postOpsWithBodyParams(url, this.requestBody);
		System.out.println(response.asPrettyString());
		Field field = getClass().getDeclaredField(variableName);
		if(creationType.contains("user")) {
			field.set(this, response.jsonPath().getString("username"));
		}	
	   else {
			field.setInt(this, response.jsonPath().getInt("id"));
		}
		field.setAccessible(true);
	}

	//I Perform get operation to fetch order details for url     
	@Given("^I perform (get|delete) operation to (fetch|delete) (order|user|pet) details for url \"([^\"]*)\" with (.*) variable$")
	public void getOrderDetails(String operation, String action , String type, String url, String variableName) throws Throwable {
		Response response;
		Object field = getClass().getDeclaredField(variableName).get(variableName);
		if(field.toString().contains(",")) {
			String text = field.toString().replaceAll("\\[", "");
			text = text.replaceAll("\\]", "");	    		  
			ArrayList<String> variableNamesList = new ArrayList<String>(Arrays.asList(text.split(",")));
			for(String varName : variableNamesList) {
				String updatedUrl = "";
				updatedUrl = url + varName.trim();	    	    	  
			//	buildRequest();
				this.response = requestSpec.get(updatedUrl);
			//	System.out.println(this.response.asPrettyString());
				log.info("****************************************************************************");
				log.info("Response JSON created for Get User List operation is " + this.response.asPrettyString());
				log.info("****************************************************************************");
				responseMapList.add(this.response);
			}
		}else {
			url = url + String.valueOf(field);	    	  
			buildRequest();
			if(operation.contains("delete")) {
				if(type.equalsIgnoreCase("pet")) {
					requestSpec.header("api_key", "test");
				}
				this.response = requestSpec.delete(url);  
			}else {

				this.response = requestSpec.get(url);
			}
		}
		log.info("****************************************************************************");
		log.info("Response JSON created for " +operation + " " +type+" operation is " + this.response.asPrettyString());
		log.info("****************************************************************************");
		System.out.println(this.response.asPrettyString());

	}
	
	@Given("^I verify response (details|details list)$")
	public void verifyResponseData(String type) throws Throwable {
		JSONParser parser = new JSONParser();
		JSONObject requestjson = null;
		JSONObject responsejson = null;
		if(type.equalsIgnoreCase("details list")) {
			int i = 0;
			for(Response map : responseMapList) {
				responsejson = (JSONObject) parser.parse(map.asPrettyString()); 
			for(Object key: responsejson.keySet()) {
				String expected = requestMapList.get(i).get(key).toString();
				String actual = responsejson.get(key).toString();	
				Assert.assertTrue(actual.equalsIgnoreCase(expected), "Expected Response value for "+key + " is: "+expected + " But Actual is: "+actual);
				log.info("Value is correct for "+key+ ": "+expected);
			}
			i++;
			}
		}else {
			requestjson = (JSONObject) parser.parse(this.requestBody);  
			responsejson = (JSONObject) parser.parse(this.response.asPrettyString());  
		
		for(Object key: responsejson.keySet()) {
			String expected = String.valueOf(requestjson.get(key));
			String actual = responsejson.get(key).toString();
			if(key.toString().contains("Date")) {
				Assert.assertTrue(actual.contains(expected), "Expected Response value for "+key + " is: "+expected + " But Actual is: "+actual);
			}else {
			Assert.assertTrue(actual.equalsIgnoreCase(expected), "Expected Response value for "+key + " is: "+expected + " But Actual is: "+actual);
			}
			log.info("Value is correct for "+key+ ": "+expected);
		}	
		}
		
	}
	
	
	//JSONObject json = (JSONObject) parser.parse(this.requestBody);  

	@Given("^I perform get operation to fetch (order|pet) details for url \"([^\"]*)\" with (orderId|tags|status|petId) value (.*)$")
	public void getOrderDetailsById(String type, String url,  String attribute, String variableName) throws Throwable {
		Response response;
		// Object field = getClass().getDeclaredField(variableName).get(variableName);
		buildRequest();
		if(url.contains("findBy")) {
			this.requestSpec.queryParam(attribute, variableName);
			this.response = this.requestSpec.get(url);
		}else {
		url = url +variableName;
		this.response = requestSpec.get(url);
		}
	}

	@Given("^I perform get operation to fetch inventory details for url \"([^\"]*)\"$")
	public void getInventoryDetailsById( String url) throws Throwable {
		buildRequest();
		this.response = requestSpec.get(url);
	}

	//I verify response has   
	@Given("^I verify response has (.*) with status code (.*)$")
	public void verifyResponseValues(String message, int code) throws Throwable {

		Assert.assertTrue(this.response.getStatusCode()==code, "Expected status code: "+ code + " Actual: "+this.response.getStatusCode());
		if(!message.isEmpty()) {
			message = message.replaceAll("\"", "");
			Assert.assertTrue(this.response.asPrettyString().contains(message), "Expected Error Message: "+message+ " Actual: "+this.response.asPrettyString());

		}
	}
	@Given("^I verify response with status code (.*)$")
	public void verifyResponseValues(int code) throws Throwable {
		Assert.assertTrue(this.response.getStatusCode()==code, "Expected status code: "+ code + " Actual: "+this.response.getStatusCode());
		
	}

	@Given("^I verify response received has (.*)$")
	public void verifyResponseValues(String messageList) throws Throwable {
		messageList = messageList.replaceAll("\"", "");
		for(String message : messageList.split("\\|")) {
			Assert.assertTrue(this.response.asPrettyString().contains(message), "Expected Error Message: "+message+ " Actual: "+this.response.asPrettyString());

		}
	}


	@Given("^I perform put operation for url \"([^\"]*)\" with (.*) variable updating (.*) as (.*)$")
	public void performUpdateUserOperation( String url, String userName, String identifier, String value) throws Throwable {
		Response response;
		Object field = getClass().getDeclaredField(userName).get(userName);
		if(!url.contains("pet")) {
		url = url +String.valueOf(field);
		}
		JSONParser parser = new JSONParser();  
		JSONObject json = (JSONObject) parser.parse(this.requestBody);  
		json.put(identifier, value);
		this.requestBody = json.toString();
		response = putOpsWithBodyParams(url, this.requestBody);
		log.info("****************************************************************************");
		log.info("Response JSON created for PUT USER operation is " + response.asPrettyString());
		log.info("****************************************************************************");
	}

	@Given("^I perform post operation for url \"([^\"]*)\" with (.*) variable updating name as (.*) and status as (.*)$")
	public void performUpdateUserFormDataOperation( String url, String userName, String name, String status) throws Throwable {
		Response response;
		Object field = getClass().getDeclaredField(userName).get(userName);
		url = url +String.valueOf(field);
		HashMap<String, String> queryParamMap = new HashMap<String, String>();
		queryParamMap.put("name", name);
		queryParamMap.put("status", status);
		JSONParser parser = new JSONParser();  
		JSONObject requestjson = (JSONObject) parser.parse(this.requestBody);  
		requestjson.put("name", name);
		requestjson.put("status", status);
		this.requestBody = requestjson.toString();
		response = postWithQueryParams(url, queryParamMap);
		log.info("****************************************************************************");
		log.info("Response JSON created for PUT USER operation is " + response.asPrettyString());
		log.info("****************************************************************************");
	}

	
	/*@Given("^I Perform upload file operation for \"([^\"]*)\" with (.*) image file name$")
	public void performUploadFileOperation(String url, String image) throws Throwable {

		File avatarFile = new File(System.getProperty("user.dir")+"\\src\\test\\resources\\animation1.jpg");
		System.out.println(System.getProperty("user.dir")+"\\src\\test\\resources\\animation1.jpg");
		
		CloseableHttpClient httpClient = HttpClients.createDefault();
		HttpPost uploadFile = new HttpPost("http://localhost:8080/api/v3/pet/4/uploadImage");
		MultipartEntityBuilder builder = MultipartEntityBuilder.create();
		builder.addBinaryBody(
		    "file",
		    new FileInputStream(avatarFile),
		   org.apache.http.entity.ContentType.APPLICATION_OCTET_STREAM,
		   avatarFile.getName()
		);

		HttpEntity multipart = builder.build();
		uploadFile.setEntity(multipart);
		uploadFile.addHeader("accept", "application/json");
		CloseableHttpResponse response = httpClient.execute(uploadFile);
		HttpEntity responseEntity = response.getEntity();
		// A Simple JSON Response Read
        InputStream instream = responseEntity.getContent();
        
        JSONParser parser = new JSONParser();
        JSONObject responsejson = (JSONObject) parser.parse(convertStreamToString(instream).toString());
      //  result = convert(instream);
		System.out.println(responsejson.toString());
	}*/
	
	
	//==============================================================================================================================================

	public static void buildRequest() {
		//Arrange
		RequestSpecBuilder builder = new RequestSpecBuilder();
		builder.setBaseUri("http://localhost:8080/api/v3");
		//builder.setContentType(ContentType.MULTIPART);
		requestSpec= builder.build();
		requestSpec = RestAssured.given().spec(requestSpec);
	}

	public static Response postOpsWithBodyParams(String url,String body)  {
		//  requestSpec.pathParams(pathParams);
		buildRequest();
		requestSpec.contentType(ContentType.JSON);
		requestSpec.body(body);
		return requestSpec.post(url);
	}

	public static Response getWithQueryParams(String url,Map<String, String> pathParams)  {
		buildRequest();
		requestSpec.queryParams(pathParams);
		return requestSpec.get(url);
	} 

	public static Response putOpsWithBodyParams(String url, String body) {
		//buildRequest();
		requestSpec.body(body);
		return requestSpec.put(url);
	}
	
	 public static Response postWithQueryParams(String url,Map<String, String> pathParams)  {
	        requestSpec.queryParams(pathParams);
	        return requestSpec.post(url);
	    }
	 

		int getRandomDigit(){   
			int x = 0;
			for (int i = 1; i <= 10; i++) {
				x = 10 + (int) (Math.random() * 999);
			}
			return x;

		}

		HashMap<Object, Object> generateDataFromHashMap(Map<String, String> createStoreMap) {
			HashMap<Object, Object> updatedDataMap = new HashMap<Object, Object>();
			for (String key : createStoreMap.keySet()) {

				if(createStoreMap.get(key).equalsIgnoreCase("GenerateNumber")){
					String digit = String.valueOf(getRandomDigit());
					updatedDataMap.put(key, digit);
				}
				else if(createStoreMap.get(key).equalsIgnoreCase("CurrentDate")) {
					DateTimeFormatter dtf = DateTimeFormatter.ISO_DATE_TIME;;  
					LocalDateTime now = LocalDateTime.now();  
					String formattedDateTime = now.format(dtf);
					updatedDataMap.put(key, formattedDateTime);
				}else if(createStoreMap.get(key).equalsIgnoreCase("GenerateString")) {
					String digit = String.valueOf(getRandomDigit());
					updatedDataMap.put(key, "Test"+digit);
				}
				else {
					updatedDataMap.put(key, createStoreMap.get(key));    
				}

			}
			return updatedDataMap;
		}

	 private static StringBuilder convertStreamToString(InputStream is) {

		    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		    StringBuilder sb = new StringBuilder();

		    String line = null;
		    try {
		        while ((line = reader.readLine()) != null) {
		            sb.append(line + "\n");
		        }
		    } catch (IOException e) {
		        e.printStackTrace();
		    } finally {
		        try {
		            is.close();
		        } catch (IOException e) {
		            e.printStackTrace();
		        }
		    }
		    return sb;

}
	 
}
