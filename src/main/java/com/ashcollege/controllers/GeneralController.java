package com.ashcollege.controllers;

import com.ashcollege.Persist;
import com.ashcollege.entities.OutfitItem;
import com.ashcollege.entities.OutfitSuggestion;
import com.ashcollege.responses.BasicResponse;

import okhttp3.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;

import java.io.File;
import java.io.IOException;
import java.util.List;

@RestController
public class GeneralController {

    @Autowired
    private Persist persist;

    @PostConstruct
    public void init() {
        persist.extractOutfitItemsFromExcel();
    }

//    @RequestMapping(value = "/", method = {RequestMethod.GET, RequestMethod.POST})
//    public Object hello() {
//        return "Hello From Server";
//    }

    @RequestMapping(value = "/get-outfit-items", method = {RequestMethod.GET})
    public List<OutfitItem> getOutfitItems() {
        return persist.getOutfits();
    }

    @RequestMapping(value = "/get-outfit-suggestions", method = {RequestMethod.GET, RequestMethod.POST})
    public List<OutfitSuggestion> getOutfitSuggestions(String occasion) {
        return persist.sendOutfitRequest(occasion);
    }

    @RequestMapping(value = "/sign-up", method = {RequestMethod.POST})
    public BasicResponse signUp(String username, String email, String password) {
            return persist.signUp(username, email, password);
    }

    @RequestMapping(value = "/login", method = {RequestMethod.POST})
    public BasicResponse login(String email, String password) {
        return persist.login(email, password);
    }

    @RequestMapping(value = "/upload-image", method = {RequestMethod.GET, RequestMethod.POST})
    public void UploadImageToImgur(String uri) throws Exception {
        System.out.println(uri);
        persist.uploadImageToImgur(uri);
    }

//    @RequestMapping(value = "/upload-image", method = {RequestMethod.POST})
//    public ResponseEntity<String> uploadImage(@RequestParam("photo") MultipartFile file) {
//        System.out.println("1");
//        System.out.println("Received file: " + file.getOriginalFilename());
//        // בדיקת סוג הקובץ
//        System.out.println("File type: " + file.getContentType());
//        // בדיקת גודל הקובץ
//        System.out.println("File size: " + file.getSize());
//
//        if (file.isEmpty()) {
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("File is empty");
//        }
//
//        // לעבד את הקובץ כאן
//
//        return ResponseEntity.ok("Image uploaded successfully");
//    }

}
