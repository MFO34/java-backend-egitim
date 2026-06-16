package com.parkticket.parkticket.service;
import com.parkticket.parkticket.model.Car;

public interface GarageService {
    String parkCar(Car car);
    String leaveSlot(int slotNumber);
    String getStatus();
}


