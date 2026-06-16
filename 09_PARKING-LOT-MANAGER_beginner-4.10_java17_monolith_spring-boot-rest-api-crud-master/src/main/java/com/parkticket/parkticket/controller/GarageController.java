package com.parkticket.parkticket.controller;
import com.parkticket.parkticket.model.Car;
import com.parkticket.parkticket.service.GarageServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/garage")
public class GarageController {

    @Autowired
    private GarageServiceImpl garageService;

    @PostMapping("/park")
    public String parkCar(@RequestBody Car car) {
        return garageService.parkCar(car);
    }

    @PostMapping("/leave")
    public String leaveSlot(@RequestParam int slotNumber) {
        return garageService.leaveSlot(slotNumber);
    }

    @GetMapping("/status")
    public String getStatus() {
        return garageService.getStatus();
    }
}

