package com.foodiehub.foodiesapi.service.impl;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.foodiehub.foodiesapi.entities.FoodEntity;
import com.foodiehub.foodiesapi.io.FoodRequest;
import com.foodiehub.foodiesapi.io.FoodResponse;
import com.foodiehub.foodiesapi.repository.FoodRepository;
import com.foodiehub.foodiesapi.service.FoodService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FoodServiceImpl implements FoodService {

    private final AmazonS3 amazonS3;
    private final FoodRepository foodRepository;

    @Value("${aws.s3.bucket.name}")
    private String bucketName;

    @Override
    public String uploadFile(MultipartFile file) throws IOException {
        String fileNameExtension = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf(".") + 1);
        String key = UUID.randomUUID().toString() + "." + fileNameExtension;

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentType(file.getContentType());

        amazonS3.putObject(bucketName,key,file.getInputStream(), metadata);
        return amazonS3.getUrl(bucketName, key).toString();
    }

    @Override
    public FoodResponse addFood(FoodRequest request, MultipartFile file) throws IOException {
            FoodEntity newFoodEntity = toEntity(request);
            String imageUrl = uploadFile(file);
            newFoodEntity.setImageUrl(imageUrl);
            newFoodEntity = foodRepository.save(newFoodEntity);
            return convertToResponse(newFoodEntity);
    }

    @Override
    public List<FoodResponse> getAllFoods() {
        List<FoodEntity> databaseEntries = foodRepository.findAll();
        return databaseEntries.stream()
                .map(this::convertToResponse)
                .toList();
    }

    @Override
    public FoodResponse getSingleFood(Long id) {
        FoodEntity existingFood = foodRepository.findById(id).orElseThrow(() -> new RuntimeException("Food not found with id: " + id));
        return convertToResponse(existingFood);
    }

    @Override
    public boolean deleteFile(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return false;
        }
        try {
            String key = fileName.startsWith("/") ? fileName.substring(1) : fileName;
            amazonS3.deleteObject(new DeleteObjectRequest(bucketName, key));
            return true;
        } catch (Exception e) {
            // log if you have a logger
            return false;
        }
    }

    @Override
    public void deleteFood(Long id) {
        FoodResponse response=getSingleFood(id);
        String imageUrl = response.getImageUrl();
        String filename = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
        boolean isFileDeleted = deleteFile(filename);
        if(isFileDeleted){
            foodRepository.deleteById(id);
        }
    }

    private FoodEntity toEntity(FoodRequest request){
        return FoodEntity.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .category(request.getCategory())
                .build();

    }

    private FoodResponse convertToResponse(FoodEntity foodEntity) {
        return FoodResponse.builder()
                .id(foodEntity.getId())
                .name(foodEntity.getName())
                .description(foodEntity.getDescription())
                .imageUrl(foodEntity.getImageUrl())
                .price(foodEntity.getPrice())
                .category(foodEntity.getCategory())
                .build();
    }

}