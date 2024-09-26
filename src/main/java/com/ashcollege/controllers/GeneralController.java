package com.ashcollege.controllers;

import com.ashcollege.Persist;
import com.ashcollege.entities.OutfitItem;
import com.ashcollege.entities.OutfitSuggestion;
import com.ashcollege.responses.BasicResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import javax.annotation.PostConstruct;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;

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
}
