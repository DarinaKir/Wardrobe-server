package com.ashcollege.controllers;

import com.ashcollege.Persist;
import com.ashcollege.entities.OutfitItem;
import com.ashcollege.entities.OutfitSuggestion;
import com.ashcollege.entities.User;
import com.ashcollege.responses.BasicResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import javax.annotation.PostConstruct;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
public class GeneralController {

    @Autowired
    private Persist persist;

//    @PostConstruct
//    public void init() {
//        persist.extractOutfitItemsFromExcel();
//    }

//    @RequestMapping(value = "/", method = {RequestMethod.GET, RequestMethod.POST})
//    public Object hello() {
//        return "Hello From Server";
//    }


    @RequestMapping(value = "/sign-up", method = {RequestMethod.POST})
    public BasicResponse signUp(String username, String email, String password) {
        return persist.signUp(username, email, password);
    }

    @RequestMapping(value = "/login", method = {RequestMethod.POST})
    public BasicResponse login(String email, String password) {
        System.out.println("***SIGN-IN***");
        return persist.login(email, password);
    }


//    @RequestMapping(value = "/get-outfit-items", method = {RequestMethod.GET})
//    public List<OutfitItem> getOutfitItems() {
//        return persist.getOutfits();
//    }

    @RequestMapping(value = "/get-outfit-suggestions", method = {RequestMethod.GET, RequestMethod.POST})
    public List<OutfitSuggestion> getOutfitSuggestions(String occasion, int userId, String style) {
        System.out.println(occasion + "  -  " + userId);
        return persist.sendOutfitRequest(occasion,userId,style);
    }


//    @RequestMapping(value = "/upload", method = RequestMethod.POST)
//    public String uploadFile(@RequestParam("file") MultipartFile file) {
//        if (file.isEmpty()) {
//            return "File is empty!";
//        }
////        try {
////            // שמירת הקובץ לתיקייה מקומית
////            String filePath = "C:/uploads/" + file.getOriginalFilename();
////            file.transferTo(new File(filePath));
////
////            return "File uploaded successfully: " + file.getOriginalFilename();
////        } catch (IOException e) {
////            e.printStackTrace();
////            return "File upload failed!";
////        }
//        return "ok";
//    }


    //    @PostMapping("/upload-image")
    @RequestMapping(value = "/upload-image", method = RequestMethod.POST)
    public ResponseEntity<String> uploadImage(@RequestParam("file") MultipartFile file,
                                              @RequestParam("userId") int userId) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is missing");
        }

        System.out.println("User ID: " + userId);
        System.out.println("File received: " + file.getOriginalFilename());

        return (ResponseEntity<String>) persist.uploadImage(file,userId);
    }

    @RequestMapping(value = "/get-user-clothes", method = RequestMethod.POST)
    public List<OutfitItem> getUserClothes(@RequestParam int userId) {
        System.out.println(userId);
        List<OutfitItem> outfitItems = persist.getUserOutfits(userId);
        System.out.println(outfitItems.get(0));
        return outfitItems;
    }

    @PostMapping("/delete-image")
    public ResponseEntity<String> deleteImage(@RequestBody Map<String, Integer> requestData) {
//        int imageId = requestData.get("id");
        Integer imageId = requestData.get("imageId");
        if (imageId == null) {
            return ResponseEntity.badRequest().body("Image ID is missing");
        }

        // Find the image in the database
        OutfitItem outfitItem = persist.getOutfitById(imageId);

        if (outfitItem != null) {
            try {
                persist.deleteOutfitByImageId(imageId);
//                // Delete image from Imgur
//                String deleteHash = outfitItem.getDeleteHash(); // You should store this in your database
//                persist.deleteImageFromImgur(deleteHash);
//
//                // Remove from the database
//                outfitItemRepository.delete(outfitItem);

                return ResponseEntity.ok("Image deleted successfully");
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error deleting image");
            }
        } else {
            return ResponseEntity.badRequest().body("Image not found");
        }
    }

    @RequestMapping(value = "/modify-user", method = {RequestMethod.GET, RequestMethod.POST})
    public BasicResponse getModifiedUser(String username, String newEmail, String newPassword, String newUsername) {
        return persist.modifyUser(username, newEmail, newPassword, newUsername);
    }

//    @RequestMapping(value = "/remove-background", method = {RequestMethod.GET, RequestMethod.POST})
//    public ResponseEntity<byte[]> removeBackground(@RequestParam("file") MultipartFile file) {
//        if (file.isEmpty()) {
//            return ResponseEntity.badRequest().body("File is missing".getBytes());
//        }
//
//        System.out.println("Received file: " + file.getOriginalFilename()); // לוג שם הקובץ
//
//        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
//            HttpPost post = new HttpPost("https://api.remove.bg/v1.0/removebg");
//            post.addHeader("X-Api-Key", "");
//
//            // Build the request entity
//            MultipartEntityBuilder builder = MultipartEntityBuilder.create()
//                    .addBinaryBody("image_file", file.getInputStream(), ContentType.parse(Objects.requireNonNull(file.getContentType())), file.getOriginalFilename())
//                    .addTextBody("size", "auto");
//
//            post.setEntity(builder.build());
//
//            // Execute the request
//            try (CloseableHttpResponse response = httpClient.execute(post)) {
//                // Check response status
//                int statusCode = response.getStatusLine().getStatusCode();
//                if (statusCode != 200) {
//                    System.out.println("Error from remove.bg: " + statusCode);
//                    return ResponseEntity.status(statusCode).body("Error from remove.bg".getBytes());
//                }
//
//                // Read the response content
//                InputStream inputStream = response.getEntity().getContent();
//                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//                byte[] buffer = new byte[1024];
//                int bytesRead;
//
//                while ((bytesRead = inputStream.read(buffer)) != -1) {
//                    outputStream.write(buffer, 0, bytesRead);
//                }
//
//                byte[] imageBytes = outputStream.toByteArray();
//
//                // Set response headers for the image
//                HttpHeaders headers = new HttpHeaders();
//                headers.setContentType(org.springframework.http.MediaType.IMAGE_PNG);
//                headers.setContentLength(imageBytes.length);
//
//                return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
//            }
//        } catch (IOException e) {
//            System.out.println("IOException occurred: " + e.getMessage()); // לוג של השגיאה
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing the image".getBytes());
//        }
//    }

}