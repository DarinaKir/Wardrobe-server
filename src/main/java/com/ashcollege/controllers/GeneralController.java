package com.ashcollege.controllers;

import com.ashcollege.Persist;
import com.ashcollege.entities.OutfitItem;
import com.ashcollege.entities.OutfitSuggestion;
import com.ashcollege.responses.BasicResponse;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import javax.annotation.PostConstruct;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import okhttp3.OkHttpClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;


import java.util.List;

@RestController
public class GeneralController {
    private static final String IMGUR_CLIENT_ID = "f2b3bf941b0bad6";
    private static final String IMGUR_UPLOAD_URL = "https://api.imgur.com/3/upload";

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


    @RequestMapping(value = "/sign-up", method = {RequestMethod.POST})
    public BasicResponse signUp(String username, String email, String password) {
            return persist.signUp(username, email, password);
    }

    @RequestMapping(value = "/login", method = {RequestMethod.POST})
    public BasicResponse login(String email, String password) {
        System.out.println("***SIGN-IN***");
        return persist.login(email, password);
    }


    @RequestMapping(value = "/get-outfit-items", method = {RequestMethod.GET})
    public List<OutfitItem> getOutfitItems() {
        return persist.getOutfits();
    }

    @RequestMapping(value = "/get-outfit-suggestions", method = {RequestMethod.GET, RequestMethod.POST})
    public List<OutfitSuggestion> getOutfitSuggestions(String occasion) {
        return persist.sendOutfitRequest(occasion);
    }
    @RequestMapping(value = "/upload-image", method = {RequestMethod.GET, RequestMethod.POST})
    public static void uploadImageToImgur() throws Exception {
        System.out.println("Uploading image...");

        OkHttpClient client = new OkHttpClient().newBuilder().build();
        File file = new File("item.jpg");  // השתמש בנתיב מלא אם הקובץ לא נמצא באותו תיקיה

        if (!file.exists()) {
            System.out.println("File not found: " + file.getAbsolutePath());
            return;
        }

        RequestBody body = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("image", file.getName(),
                        RequestBody.create(MediaType.parse("application/octet-stream"), file))
                .addFormDataPart("type", "image")
                .addFormDataPart("title", "Simple upload")
                .addFormDataPart("description", "This is a simple image upload to Imgur")
                .build();

        Request request = new Request.Builder()
                .url("https://api.imgur.com/3/image")
                .method("POST", body)
                .addHeader("Authorization", "Client-ID f2b3bf941b0bad6")
                .build();

        try (Response response = client.newCall(request).execute()) {
            System.out.println("Response Code: " + response.code());
            System.out.println("Response Message: " + response.message());

            if (response.isSuccessful()) {
                System.out.println("Upload successful! Response body: ");
                System.out.println(response.body().string());  // הדפסת גוף התגובה
            } else {
                System.out.println("Upload failed with response code: " + response.code());
                System.out.println("Response body: " + response.body().string());
            }
        } catch (IOException e) {
            System.out.println("Error during the upload process: " + e.getMessage());
            e.printStackTrace();
        }
    }
    @RequestMapping(value = "/upload", method = RequestMethod.POST)
    public String uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return "File is empty!";
        }

        try {
            // שמירת הקובץ לתיקייה מקומית
            String filePath = "C:/uploads/" + file.getOriginalFilename();
            file.transferTo(new File(filePath));

            return "File uploaded successfully: " + file.getOriginalFilename();
        } catch (IOException e) {
            e.printStackTrace();
            return "File upload failed!";
        }
    }

}
