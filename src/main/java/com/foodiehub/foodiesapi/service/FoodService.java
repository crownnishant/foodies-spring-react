package com.foodiehub.foodiesapi.service;

import com.foodiehub.foodiesapi.io.FoodRequest;
import com.foodiehub.foodiesapi.io.FoodResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface FoodService {

    String uploadFile(MultipartFile file) throws IOException;

    FoodResponse addFood(FoodRequest request, MultipartFile file) throws IOException;

    List<FoodResponse> getAllFoods();

    FoodResponse getSingleFood(Long id);

/* delete file image from S3 bucket as well once food is deleted */

    boolean deleteFile(String fileName);

    void deleteFood(Long id);
}
