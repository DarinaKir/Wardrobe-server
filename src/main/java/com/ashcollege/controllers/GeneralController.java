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

import javax.annotation.PostConstruct;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;

import okhttp3.OkHttpClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

@RestController
public class GeneralController {
    private static final String IMGUR_CLIENT_ID = "f2b3bf941b0bad6";
    private static final String IMGUR_UPLOAD_URL = "https://api.imgur.com/3/upload";
    private static final String OPENAI_API_KEY = "";

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
        System.out.println("SIGN-IN");
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

    @RequestMapping(value = "/upload", method = RequestMethod.POST)
    public String uploadFile(@RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            return "File is empty!";
        }
//        try {
//            // שמירת הקובץ לתיקייה מקומית
//            String filePath = "C:/uploads/" + file.getOriginalFilename();
//            file.transferTo(new File(filePath));
//
//            return "File uploaded successfully: " + file.getOriginalFilename();
//        } catch (IOException e) {
//            e.printStackTrace();
//            return "File upload failed!";
//        }
        return "ok";
    }

    public static String uploadImageToImgur(@RequestParam("file") MultipartFile Multipartfile) throws Exception {

        String imageUrl = null;
        System.out.println("Uploading image...");

        OkHttpClient client = new OkHttpClient().newBuilder().build();
        File file = convertMultipartFileToFile(Multipartfile);

        if (!file.exists()) {
            System.out.println("File not found: " + file.getAbsolutePath());
            return "File not found";
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
                String jsonResponse = response.body().string();
                System.out.println(jsonResponse);  // הדפסת גוף התגובה

                try {
                    // פרס את ה-JSON
                    JSONObject jsonObject = new JSONObject(jsonResponse);

                    // גש לאובייקט "data"
                    JSONObject data = jsonObject.getJSONObject("data");

                    // קבל את ה-URL מתוך המפתח "link"
                    imageUrl = data.getString("link");

                    // הדפס את ה-URL
                    System.out.println("Image URL: " + imageUrl);
                    return imageUrl;
                } catch (Exception e) {
                    System.out.println(e);
                }

            } else {
                System.out.println("Upload failed with response code: " + response.code());
                System.out.println("Response body: " + response.body().string());
            }
        } catch (IOException e) {
            System.out.println("Error during the upload process: " + e.getMessage());
            e.printStackTrace();
        }
        return imageUrl;
    }


    @RequestMapping(value = "/upload-image", method = RequestMethod.POST)
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) throws Exception {
        String imageURL = uploadImageToImgur(file);
        System.out.println("imageURL: " + imageURL);
        try {
            // שמירה לקובץ זמני
            File tempFile = convertMultipartFileToFile(file);
            // ביצוע בקשה ל-OpenAI
            String jsonResponse = uploadToOpenAI(tempFile);
            System.out.println("imageUrl: " + jsonResponse);

            // מחיקת הקובץ הזמני
            tempFile.delete();

            System.out.println("ResponseEntity.ok(imageUrl):  " + ResponseEntity.ok(jsonResponse));
            return ResponseEntity.ok(jsonResponse);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error uploading image: " + e.getMessage());
        }
    }

    private String uploadToOpenAI(File file) throws IOException {
        OkHttpClient client = new OkHttpClient();

        // המרת הקובץ ל-Base64
        byte[] fileContent = Files.readAllBytes(file.toPath());
        String base64Image = Base64.getEncoder().encodeToString(fileContent);
        String dataUrl = "data:image/png;base64," + base64Image;

        // יצירת ה-JSON לשליחה
        JSONObject jsonContent = new JSONObject();
        jsonContent.put("model", "gpt-4o");

        JSONArray messagesArray = new JSONArray();
        JSONObject messageObject = new JSONObject();
        messageObject.put("role", "user");

        JSONArray contentArray = new JSONArray();
        contentArray.put(new JSONObject().put("type", "text").put("text", "return JSON with the features for the item in the image: type(shirt,pants...),style(elegant...),color,season and (short) description"));
        contentArray.put(new JSONObject().put("type", "image_url").put("image_url", new JSONObject().put("url", dataUrl)));

        messageObject.put("content", contentArray);
        messagesArray.put(messageObject);
        jsonContent.put("messages", messagesArray);

        // בקשת POST ל-OpenAI
        RequestBody body = RequestBody.create(
                jsonContent.toString(),
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .post(body)
                .addHeader("Authorization", "Bearer " + OPENAI_API_KEY)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                String responseBody = response.body().string();
                JSONObject jsonResponse = new JSONObject(responseBody);
                return jsonResponse.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
            } else {
                throw new IOException("Unexpected response code: " + response.code());
            }
        }
    }

    public static File convertMultipartFileToFile(MultipartFile multipartFile) throws IOException {
        File file = new File(System.getProperty("java.io.tmpdir") + "/" + multipartFile.getOriginalFilename());
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(multipartFile.getBytes());
        }
        return file;
    }

}