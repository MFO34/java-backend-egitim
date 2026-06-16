package com.parkticket.parkticket.service;

import com.parkticket.parkticket.model.Car;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Service
public class GarageServiceImpl implements GarageService {
    private final int garageCapacity = 10;
    private Map<String, Car> parkedCars;
    private boolean[] parkingSlots;

    public GarageServiceImpl() {
        this.parkedCars = new HashMap<>();
        this.parkingSlots = new boolean[garageCapacity];
        Arrays.fill(parkingSlots, false);
    }

    @Override
    public String parkCar(Car car) {
        int availableSlot = findAvailableSlot(car.getWidth());
        if (availableSlot != -1) {
            parkedCars.put(car.getPlateNumber(), car);
            markSlotOccupied(availableSlot, car.getWidth());
            return "Allocated " + car.getWidth() + " slot(s).";
        } else {
            return "Garage is full.";
        }
    }


    @Override
    public String leaveSlot(int slotNumber) {
        if (parkedCars.containsKey(slotNumber)) {
            Car leavingCar = parkedCars.get(slotNumber);
            parkedCars.remove(slotNumber);
            markSlotVacant(slotNumber, leavingCar.getWidth());
            return "Slot " + slotNumber + " vacated.";
        } else {
            return "Slot " + slotNumber + " is already vacant.";
        }
    }

    @Override
    public String getStatus() {
        StringBuilder status = new StringBuilder("Status:\n");
        for (Map.Entry<String, Car> entry : parkedCars.entrySet()) {
            String plateNumber = entry.getKey();
            Car slotNumber = entry.getValue();
            Car car = parkedCars.get(plateNumber);
            status.append(car.getPlateNumber()).append(" ").append(car.getColor()).append(" [").append(plateNumber).append("]\n");
        }
        return status.toString();
    }


    private int findAvailableSlot(int carWidth) {
        for (int i = 0; i <= garageCapacity - carWidth; i++) {
            boolean canPark = true;
            for (int j = i; j < i + carWidth; j++) {
                if (parkingSlots[j]) {
                    canPark = false;
                    break;
                }
            }
            if (canPark) return i;
        }
        return -1;
    }

    private void markSlotOccupied(int startSlot, int carWidth) {
        for (int i = startSlot; i < startSlot + carWidth; i++) {
            parkingSlots[i] = true;
        }
    }

    private void markSlotVacant(int startSlot, int carWidth) {
        for (int i = startSlot; i < startSlot + carWidth; i++) {
            parkingSlots[i] = false;
        }
    }
}