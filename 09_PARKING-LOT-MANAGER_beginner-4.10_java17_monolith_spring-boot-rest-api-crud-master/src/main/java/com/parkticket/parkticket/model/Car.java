package com.parkticket.parkticket.model;
public class Car {
    private String plateNumber;
    private String color;
    private int width;

    public Car(String plateNumber, String color, int width) {
        this.plateNumber = plateNumber;
        this.color = color;
        this.width = width;
    }

    public String getPlateNumber() {
        return plateNumber;
    }

    public void setPlateNumber(String plateNumber) {
        this.plateNumber = plateNumber;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }
}
