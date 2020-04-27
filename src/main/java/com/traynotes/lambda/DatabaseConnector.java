package com.traynotes.lambda;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.*;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.traynotes.lambda.model.FoundPet;
import com.traynotes.lambda.model.LostPet;
import com.traynotes.lambda.model.SightedPet;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.joda.time.DateTime;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class DatabaseConnector {

    static String uri;
    static {
        String encodedUsername = null;
        try {
            encodedUsername = URLEncoder.encode("appuser", StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String encodedPassword = null;
        try {
            encodedPassword = URLEncoder.encode("eu8iFponZKM27NwJ", StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        uri = "mongodb+srv://" + encodedUsername + ":" + encodedPassword + "@cluster0-scvmv.mongodb.net/test-database?" +
                "retryWrites=true&w=majority&maxIdleTimeMS=25000&" +
                "readPreference=secondary&replicaSet=Cluster0-shard-0&ssl=true";
    }

    public static final Gson gson = new Gson();

    public static String getAllLost(Map<String, String> queryParams){
        if(queryParams.containsKey("id")){
            String id = queryParams.get("id");
            List<String> results = getAllById("test-database", "table-lost", id);
            if(results.size() ==  1){
                String result = results.get(0);
                return result;
            }
            return "{\"message\":\"empty - no results for id (" + id + ")\"}";
        }

        List<String> lostPets = getAll("test-database", "table-lost");
        List<LostPet> filtered = new ArrayList<>();
        for(String lost : lostPets){
            LostPet lostPet = gson.fromJson(lost, LostPet.class);

            if(queryParams.containsKey("animal")){
                String animal = queryParams.get("animal");
                if (!lostPet.getAnimal().toUpperCase().equals(animal.toUpperCase())) {
                    continue;
                }
            }

            if(queryParams.containsKey("breed")){
                String breed = queryParams.get("breed");
                if (!lostPet.getBreed().toUpperCase().equals(breed.toUpperCase())) {
                    continue;
                }
            }

            filtered.add(lostPet);
        }

        if(queryParams.containsKey("lat") && queryParams.containsKey("lng")){
            String targetLat = queryParams.get("lat");
            double lat = Double.parseDouble(targetLat);

            String targetLng = queryParams.get("lng");
            double lng = Double.parseDouble(targetLng);

            filtered.sort((one, two) -> {
                double oneLat = Double.parseDouble(one.getLat());
                double oneLng = Double.parseDouble(one.getLng());

                double twoLat = Double.parseDouble(two.getLat());
                double twoLng = Double.parseDouble(two.getLng());

                double distOne = distance(lat, oneLat, lng, oneLng);
                double distTwo = distance(lat, twoLat, lng, twoLng);

                return Double.compare(distOne, distTwo);
            });

        } else {
            List<LostPet> inverted = new ArrayList<>();
            for(LostPet lost : filtered){
                inverted.add(0, lost);
            }
            filtered = inverted;
        }

        JsonObject response = new JsonObject();
        JsonArray array = new JsonArray();
        for(LostPet pet : filtered){
            JsonObject petObject = (JsonObject) JsonParser.parseString(gson.toJson(pet));
            array.add(petObject);
        }
        response.add("result", array);
        return response.toString();
    }

    public static String getAllRetrieved(Map<String, String> queryParams) {
        List<String> foundPets = getAll("test-database", "table-found");
        List<String> sightedPets = getAll("test-database", "table-sighted");

        List<String> pets = new ArrayList<>();
        if(queryParams.containsKey("animal") || queryParams.containsKey("breed")) {
            for(String found : foundPets){
                LostPet foundPet = gson.fromJson(found, LostPet.class);
                if(queryParams.containsKey("animal")){
                    String animal = queryParams.get("animal");
                    if (!foundPet.getAnimal().toUpperCase().equals(animal.toUpperCase())) {
                        continue;
                    }
                }

                if(queryParams.containsKey("breed")){
                    String breed = queryParams.get("breed");
                    if (!foundPet.getBreed().toUpperCase().equals(breed.toUpperCase())) {
                        continue;
                    }
                }
                pets.add(found);
            }

        } else if (queryParams.containsKey("lat") && queryParams.containsKey("lng")){
            String targetLat = queryParams.get("lat");
            double lat = Double.parseDouble(targetLat);

            String targetLng = queryParams.get("lng");
            double lng = Double.parseDouble(targetLng);

            List<SightedPet> filtered = new ArrayList<>();
            for(String sighted : sightedPets){
                SightedPet sightedPet = gson.fromJson(sighted, SightedPet.class);
                filtered.add(sightedPet);
            }
            filtered.sort((one, two) -> {
                double oneLat = Double.parseDouble(one.getLat());
                double oneLng = Double.parseDouble(one.getLng());

                double twoLat = Double.parseDouble(two.getLat());
                double twoLng = Double.parseDouble(two.getLng());

                double distOne = distance(lat, oneLat, lng, oneLng);
                double distTwo = distance(lat, twoLat, lng, twoLng);

                return Double.compare(distOne, distTwo);
            });
            for(SightedPet sightedPet : filtered){
                pets.add(gson.toJson(sightedPet));
            }
        } else {
            pets.addAll(foundPets);
            pets.addAll(sightedPets);
        }

        JsonObject response = new JsonObject();
        JsonArray array = new JsonArray();

        for(String pet : pets){
            JsonObject petObject = (JsonObject) JsonParser.parseString(pet);
            array.add(petObject);
        }
        response.add("result", array);
        return response.toString();
    }

    public static String getAllFound() {
        return getAllGeneric("table-found");
    }

    public static String getAllSighted() {
        return getAllGeneric("table-sighted");
    }

    private static String getAllGeneric(String tableName){
        List<String> pets = getAll("test-database", tableName);
        JsonObject response = new JsonObject();
        JsonArray array = new JsonArray();

        for(String pet : pets){
            JsonObject petObject = (JsonObject) JsonParser.parseString(pet);
            array.add(petObject);
        }
        response.add("result", array);
        return response.toString();
    }

    public static String post(LostPet lostPet){
        System.out.println("Posting lost pet");
        String id = "";
        try {
            id = insertPet("test-database", "table-lost", gson.toJson(lostPet));
        } catch (UnsupportedEncodingException e) {
            return "error";
        }
        return id;
    }

    public static boolean post(FoundPet foundPet){
        try {
            insertPet("test-database", "table-found", gson.toJson(foundPet));
        } catch (UnsupportedEncodingException e) {
            return false;
        }
        return true;
    }

    public static boolean post(SightedPet sightedPet){
        try {
            insertPet("test-database", "table-sighted", gson.toJson(sightedPet));
        } catch (UnsupportedEncodingException e) {
            return false;
        }
        return true;
    }

    public static String insertPet(String dbName, String colName, String body) throws UnsupportedEncodingException {
        MongoClientURI clientUri = new MongoClientURI(uri);
        MongoClient mongo = new MongoClient(clientUri);
        MongoDatabase database = mongo.getDatabase(dbName);
        MongoCollection collection = database.getCollection(colName);

        Document hit = new Document("body", body)
                .append("randomUUID", UUID.randomUUID().toString())
                .append("creationTime", DateTime.now().toString());
        collection.insertOne(hit);

        ObjectId id = (ObjectId) hit.get("_id");
        if(id == null){
            return "";
        }
        System.out.println("Id of inserted document : " + id.toString());

        mongo.close();

        return id.toString();
    }

    private static List<String> getAll(String dbName, String colName) {
        MongoClientURI clientUri = new MongoClientURI(uri);
        MongoClient mongo = new MongoClient(clientUri);
        MongoDatabase database = mongo.getDatabase(dbName);
        MongoCollection collection = database.getCollection(colName);

        List<String> bodies = new ArrayList<>();
        FindIterable iterable = collection.find();
        MongoCursor cursor = iterable.cursor();
        while (cursor.hasNext()) {
            Document obj = (Document) cursor.next();
            bodies.add((String) obj.get("body"));
        }
        return bodies;
    }

    private static List<String> getAllById(String dbName, String colName, String id) {
        MongoClientURI clientUri = new MongoClientURI(uri);
        MongoClient mongo = new MongoClient(clientUri);
        MongoDatabase database = mongo.getDatabase(dbName);
        MongoCollection collection = database.getCollection(colName);

        List<String> bodies = new ArrayList<>();
        FindIterable iterable = collection.find();
        MongoCursor cursor = iterable.cursor();
        while (cursor.hasNext()) {
            Document obj = (Document) cursor.next();
            if(obj.get("_id").toString().equals(id)){
                bodies.add((String) obj.get("body"));
            }
        }
        return bodies;
    }

    /**
     * Calculate distance between two points in latitude and longitude taking
     * into account height difference. If you are not interested in height
     * difference pass 0.0. Uses Haversine method as its base.
     *
     * lat1, lon1 Start point lat2, lon2 End point el1 Start altitude in meters
     * el2 End altitude in meters
     * @returns Distance in Meters
     */
    public static double distance(double lat1, double lat2, double lon1, double lon2) {
        final int R = 6371; // Radius of the earth

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // convert to meters

        return distance;
    }

}
