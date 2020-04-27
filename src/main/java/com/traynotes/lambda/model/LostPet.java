package com.traynotes.lambda.model;

public class LostPet {

    private String name;
    private String email;
    private String animal;
    private String breed;
    private String lat;
    private String lng;
    private String imageUrl;

    private LostPet(){

    }

    public String getAnimal() {
        return animal;
    }

    public String getBreed() {
        return breed;
    }

    public String getLat() {
        return lat;
    }

    public String getLng() {
        return lng;
    }

    public String getEmail() {
        return email;
    }
}
