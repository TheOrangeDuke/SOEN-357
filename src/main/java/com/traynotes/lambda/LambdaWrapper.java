package com.traynotes.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2ProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2ProxyResponseEvent;
import com.google.gson.Gson;
import com.traynotes.lambda.model.FoundPet;
import com.traynotes.lambda.model.LostPet;
import com.traynotes.lambda.model.SightedPet;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;


public class LambdaWrapper implements RequestHandler<APIGatewayV2ProxyRequestEvent, APIGatewayV2ProxyResponseEvent> {
    public APIGatewayV2ProxyResponseEvent handleRequest(APIGatewayV2ProxyRequestEvent request, Context context) {

        Map<String, String> queryParams = request.getQueryStringParameters();

        String method = request.getHttpMethod();
        String type = request.getQueryStringParameters().get("type");
        String endpoint = request.getPath();
        String body = request.getBody();

        String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime());
        context.getLogger().log("Invocation started: " + timeStamp);
        context.getLogger().log("Method:" + method);
        context.getLogger().log("Type:" + type);
        context.getLogger().log("Endpoint:" + endpoint);
        context.getLogger().log("Body:" + body);
        timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime());
        context.getLogger().log("Invocation completed: " + timeStamp);

        String mess = routing(type, method, body, queryParams);
        context.getLogger().log("Routing result : " + mess);

        APIGatewayV2ProxyResponseEvent response = new APIGatewayV2ProxyResponseEvent();
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Access-Control-Allow-Headers", "Content-Type");
        headers.put("Access-Control-Allow-Methods", "OPTIONS,POST,GET");
        headers.put("Access-Control-Allow-Origin", "*");
        response.setHeaders(headers);
        response.setBody(mess);
        response.setStatusCode(200);
        return response;
    }

    public String routing(String endpoint, String method, String body, Map<String, String> queryParams){
        System.out.println("Routing started");
        Gson gson = new Gson();
        switch (endpoint){
            case "lost":
                if(method.equals("GET")){
                    return DatabaseConnector.getAllLost(queryParams);
                } else if(method.equals("POST")){
                    if(body == null){
                        return "{\"message\":\"empty body cannot be inserted\"}";
                    }
                    LostPet lostPet = gson.fromJson(body, LostPet.class);
                    String id = DatabaseConnector.post(lostPet);
                    if(!id.equals("error")){
                        EmailSender.sendConfirmationEmail(lostPet.getEmail(), "http://www.lostpawsmtl.com?id=" + id);
                        return "{\"message\":\"OK\", \"id\":\"" + id + "\"}";
                    } else {
                        return "{\"message\":\"failed insertion\"}";
                    }
                }
                break;
            case "found":
                if(method.equals("GET")){
                    return DatabaseConnector.getAllFound();
                } else if(method.equals("POST")){
                    FoundPet foundPet = gson.fromJson(body, FoundPet.class);
                    if(DatabaseConnector.post(foundPet)){
                        return "{\"message\":\"OK\"}";
                    } else {
                        return "{\"message\":\"failed insertion\"}";
                    }
                }
                break;
            case "sighted":
                if(method.equals("GET")){
                    return DatabaseConnector.getAllSighted();
                } else if(method.equals("POST")){
                    SightedPet sightedPet = gson.fromJson(body, SightedPet.class);
                    if(DatabaseConnector.post(sightedPet)){
                        return "{\"message\":\"OK\"}";
                    } else {
                        return "{\"message\":\"failed insertion\"}";
                    }
                }
                break;
            case "retrieved":
                if(method.equals("GET")){
                    return DatabaseConnector.getAllRetrieved(queryParams);
                }
                break;
            default:
                return "{\"message\":\"Endpoint not found\"}";
        }
        return "{\"message\":\"Runtime hit empty endpoint\"}";
    }
}
