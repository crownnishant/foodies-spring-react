package com.foodiehub.foodiesapi.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodiehub.foodiesapi.io.FoodRequest;
import com.foodiehub.foodiesapi.io.FoodResponse;
import com.foodiehub.foodiesapi.service.FoodService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/foods")
@RequiredArgsConstructor
@CrossOrigin("*")
public class FoodController {

    private final FoodService foodService;

    @PostMapping
    public FoodResponse addFood(@RequestPart("food") String foodString,
                                @RequestPart("file")MultipartFile file) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        FoodRequest request= null;
        try{
            request= objectMapper.readValue(foodString, FoodRequest.class);
        }
        catch (JsonProcessingException e){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid food data format", e);
        }
        FoodResponse response=foodService.addFood(request, file);
        return response;
    }

    @GetMapping
    public List<FoodResponse> getAllFoods(){
        return foodService.getAllFoods();
    }

    @GetMapping("/{id}")
    public FoodResponse getSingleFood(@PathVariable Long id){
        return foodService.getSingleFood(id);

    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFood(@PathVariable Long id) {
        foodService.deleteFood(id);
    }
}
