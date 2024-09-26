package com.ashcollege;


import com.ashcollege.entities.OutfitItem;
import com.ashcollege.entities.OutfitSuggestion;
import com.ashcollege.entities.User;
import com.ashcollege.responses.BasicResponse;
import com.ashcollege.responses.UserResponse;
import okhttp3.*;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.google.gson.*;
import org.apache.poi.ss.usermodel.*;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static com.ashcollege.utils.Errors.*;

@Transactional
@Component
@SuppressWarnings("unchecked")
public class Persist {

    private static final Logger LOGGER = LoggerFactory.getLogger(Persist.class);

    private final SessionFactory sessionFactory;

    private static final String IMGUR_CLIENT_ID = "f2b3bf941b0bad6";
//    private static final String API_KEY = "";
    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String OPENAI_API_KEY = "";

//    private final List<OutfitItem> outfits = new ArrayList<>();

    @Autowired
    public Persist(SessionFactory sf) {
        this.sessionFactory = sf;
    }

    public Session getQuerySession() {
        return sessionFactory.getCurrentSession();
    }

    public void save(Object object) {
        this.sessionFactory.getCurrentSession().saveOrUpdate(object);
    }

    public <T> T loadObject(Class<T> clazz, int oid) {
        return this.getQuerySession().get(clazz, oid);
    }

    public <T> List<T> loadList(Class<T> clazz) {
        return this.sessionFactory.getCurrentSession().createQuery("FROM User").list();
    }

    private boolean isUsernameAvailable(String username) {
        User user = null;
        if (username != null && !username.isEmpty()) {
            user = (User) this.sessionFactory.getCurrentSession().createQuery(
                            "FROM User WHERE username = :username")
                    .setParameter("username", username)
                    .uniqueResult();

        }
        return (user == null);
    }

    private boolean isEmailAvailable(String email) {
        User user = null;
        try {
            user = (User) this.sessionFactory.getCurrentSession().createQuery(
                            "FROM User WHERE email = :email")
                    .setParameter("email", email)
                    .uniqueResult();

        } catch (Exception e) {
            System.out.println("error:  " + e);
        }
        return (user == null);
    }

    private boolean isEmailCorrect(String email) {
        return email.contains("@") && email.contains(".") && (email.lastIndexOf(".") - email.indexOf("@") > 1) && (email.indexOf("@") != 0);
    }

    private boolean isPasswordStrong(String password) {
        return password != null && password.length() >= 8;
    }

    public User getUserById(int id) {
        User user = null;
        user = (User) this.sessionFactory.getCurrentSession().createQuery(
                        "FROM User WHERE id = :id")
                .setParameter("id", id)
                .setMaxResults(1)
                .uniqueResult();

        return user;
    }

    public OutfitItem getOutfitById(int id) {
        OutfitItem outfitItem = null;
        outfitItem = (OutfitItem) this.sessionFactory.getCurrentSession().createQuery(
                        "FROM OutfitItem WHERE id = :id")
                .setParameter("id", id)
                .setMaxResults(1)
                .uniqueResult();

        return outfitItem;
    }

    public void deleteOutfitByImageId(int id) {
        this.sessionFactory.getCurrentSession().createQuery("DELETE FROM OutfitItem WHERE id = :id")
                .setParameter("id", id)
                .executeUpdate();
    }


    public List<OutfitItem> getUserOutfits(int userId) {
        return (List<OutfitItem>) this.sessionFactory.getCurrentSession().createQuery(
                        "FROM OutfitItem WHERE userId = :userId")
                .setParameter("userId", userId)
                .list();
    }

    public BasicResponse login(String email, String password) {
        BasicResponse basicResponse;
        Integer errorCode = null;
        User user = null;

        if (email != null && !email.isEmpty()) {
            if (password != null && !password.isEmpty()) {
                user = (User) this.sessionFactory.getCurrentSession().createQuery(
                                "FROM User WHERE email = :email AND password = :password")
                        .setParameter("email", email)
                        .setParameter("password", password)
                        .setMaxResults(1)
                        .uniqueResult();

            } else {
                errorCode = ERROR_SIGN_UP_NO_PASSWORD;
            }
        } else {
            errorCode = ERROR_SIGN_UP_NO_EMAIL;
        }

        if (user == null) {
            if (errorCode == null) {
                errorCode = ERROR_LOGIN_WRONG_CREDS;
            }
            basicResponse = new BasicResponse(false, errorCode);
        } else {
            basicResponse = new UserResponse(true, null, user);
        }
        return basicResponse;
    }

    public BasicResponse signUp(String username, String email, String password) {
        Integer errorCode = null;
        if (username != null && !username.isEmpty()) {
            if (password != null && !password.isEmpty()) {
                if (email != null && !email.isEmpty()) {
                    if (isEmailCorrect(email)) {
                        if (isUsernameAvailable(username)) {
                            if (isPasswordStrong(password)) {
                                if (isEmailAvailable(email)) {
                                    System.out.println("here");
                                    User user = new User(username, email, password);
                                    save(user);
                                    return new UserResponse(true, null, user);
                                } else {
                                    errorCode = EMAIL_ALREADY_IN_USE;
                                }
                            } else {
                                errorCode = ERROR_WEAK_PASSWORD;
                            }
                        } else {
                            errorCode = ERROR_SIGN_UP_USERNAME_TAKEN;
                        }
                    } else {
                        errorCode = ERROR_EMAIL_FORMAT;
                    }
                } else {
                    errorCode = ERROR_SIGN_UP_NO_EMAIL;
                }
            } else {
                errorCode = ERROR_SIGN_UP_NO_PASSWORD;
            }
        } else {
            errorCode = ERROR_SIGN_UP_NO_USERNAME;
        }
        return new BasicResponse(false, errorCode);
    }


    private List<OutfitItem> parseOutfitJson(JsonObject outfitSuggestionJson,int userId) {
        List<OutfitItem> outfitItems = new LinkedList<>();
        for (String key : outfitSuggestionJson.keySet()) {
            if (!key.equals("explanation")) {
                JsonElement element = outfitSuggestionJson.get(key);

                // Assuming all IDs are integers
                int id = element.getAsInt();

                OutfitItem outfit = getUserOutfits(userId).stream()
                        .filter(item -> item.getId() == id)
                        .findFirst()
                        .orElse(null);
                if (outfit != null) {
                    // Do something with the outfit
                    outfitItems.add(outfit);
                } else {
                    // Handle the case where the outfit is not found
                    System.out.println("oopsie");
                }
            }
        }
        return outfitItems;
    }


//    public void extractOutfitItemsFromExcel() {
//        String excelFilePath = "src/main/java/com/ashcollege/files/Classification of clothes.xlsx";
//        int startRow = 1; // Start from row 2 (index 1)
//        int endRow = 49;  // End at row 50 (index 49)
//        try (FileInputStream fileInputStream = new FileInputStream(new File(excelFilePath));
//             Workbook workbook = WorkbookFactory.create(fileInputStream)) {
//            Sheet sheet = workbook.getSheetAt(0);
//            // Iterate over the rows from startRow to endRow
//            for (int i = startRow; i <= endRow && i <= sheet.getLastRowNum(); i++) {
//                Row row = sheet.getRow(i);
//                if (row != null) {
//                    OutfitItem outfitItem = new OutfitItem(i);
//                    for (int j = 1; j < 6; j++) {
//                        Cell cell = row.getCell(j);
//                        if (cell != null) {
//                            switch (j) {
//                                case 1:
//                                    outfitItem.setType(cell.getStringCellValue());
//                                    break;
//                                case 2:
//                                    outfitItem.setStyle(cell.getStringCellValue());
//                                    break;
//                                case 3:
//                                    outfitItem.setColor(cell.getStringCellValue());
//                                    break;
//                                case 4:
//                                    outfitItem.setSeason(cell.getStringCellValue());
//                                    break;
//                                case 5:
//                                    outfitItem.setDescription(cell.getStringCellValue());
//                                    break;
//                            }
//                        }
//                    }
//                    outfits.add(outfitItem);
//                }
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        System.out.println("Values in the sixth column from row " + (startRow + 1) + " to row " + (endRow + 1) + ":");
//        for (OutfitItem outfitItem : outfits) {
//            System.out.println(outfitItem);
//        }
//    }

    public List<OutfitSuggestion> sendOutfitRequest(String occasion, int userId, String style) {
        Gson gson = new Gson();
        JsonArray jsonArray = new JsonArray();

        List<OutfitItem> outfitItems = getUserOutfits(userId);
        for (OutfitItem outfitItem : outfitItems) {
            JsonObject jsonObject = getJsonObject(outfitItem);
            jsonArray.add(jsonObject);
        }
        String clothes = gson.toJson(jsonArray);

        System.out.println(style);
        String requestPayload = gson.toJson(Map.of(
                "model", "gpt-4o",
                "messages", List.of(
                        Map.of("role", "system", "content", "You are a helpful assistant."),
                        Map.of("role", "user", "content", "You are a stylist. Choose 3 " + ((style.isEmpty())?  "" : style + " ")  + "outfits (each must include either a top, bottom, or dress, plus shoes; bag and other accessories are optional) to suit" + occasion + " from the following items. Ensure the colors match. Return a JsonArray with each outfit as a JsonObject. Use the following naming convention for the item IDs in the JSON: \"top\", \"bottom\", \"dress\", \"shoes\", \"accessory\". Each outfit should also include an (short) explanation for your choices. Only include the IDs and explanation in the JSON: " + clothes
                        )
                )
        ));

        // Print the request payload to debug
        System.out.println("Request Payload:");
        System.out.println(requestPayload);

        // Send request to OpenAI API
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OPENAI_API_URL))
                .header("Authorization", "Bearer " + OPENAI_API_KEY)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestPayload))
                .build();

        List<OutfitSuggestion> outfitSuggestions = new ArrayList<>();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Response from GPT API:");
            System.out.println(response.body());

            // Step 1: Parse the main JSON response
            String responseBody = response.body();

            // הוספת ניקוי התגובה
            String cleanedResponseBody = responseBody.replace("```json", "").replace("```", "").trim();

            // פרש את ה-JSON המנוקה
            JsonObject jsonResponse = JsonParser.parseString(cleanedResponseBody).getAsJsonObject();
            JsonArray choicesArray = jsonResponse.getAsJsonArray("choices");

            // Get the content string from the big Json inside choicesArray
            JsonObject firstChoice = choicesArray.get(0).getAsJsonObject();
            String content = firstChoice.getAsJsonObject("message").get("content").getAsString();

            String cleanedContent = content.replaceAll("json\\n|\\n", "").replace("\\n", "\n");

            try {
                JsonArray outfitSuggestionsArray = JsonParser.parseString(cleanedContent).getAsJsonArray();

                for (int i = 0; i < outfitSuggestionsArray.size(); i++) {
                    List<OutfitItem> itemsOfSuggestion = new LinkedList<>();
                    JsonObject outfitSuggestionJson = outfitSuggestionsArray.get(i).getAsJsonObject();
                    String explanation = outfitSuggestionJson.get("explanation").getAsString();
                    itemsOfSuggestion = parseOutfitJson(outfitSuggestionJson, userId);
                    OutfitSuggestion outfitSuggestion = new OutfitSuggestion(itemsOfSuggestion, explanation);
                    outfitSuggestions.add(outfitSuggestion);
                }
                //PRINTS OUTFIT SUGGESTION ARRAY !!! :)
                System.out.println("outfit Suggestions:  ");
                for (OutfitSuggestion outfitSuggestion : outfitSuggestions) {
                    System.out.println(outfitSuggestion);
                    System.out.println(" ");
                }
            } catch (JsonSyntaxException e) {
                e.printStackTrace();
                System.out.println("Malformed JSON: " + e.getMessage());
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return outfitSuggestions;
    }


    @NotNull
    private static JsonObject getJsonObject(OutfitItem outfitItem) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("id", outfitItem.getId());
        jsonObject.addProperty("type", outfitItem.getType());
        jsonObject.addProperty("style", outfitItem.getStyle());
        jsonObject.addProperty("color", outfitItem.getColor());
        jsonObject.addProperty("season", outfitItem.getSeason());
        jsonObject.addProperty("season", outfitItem.getSeason());
        jsonObject.addProperty("description", outfitItem.getDescription());
        return jsonObject;
    }

//    public List<OutfitItem> getOutfits() {
//        return outfits;
//    }

    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file, int userId) {
        System.out.println("userId: " + userId);
        try {
            // שמירה לקובץ זמני
            File tempFile = convertMultipartFileToFile(file);
            // ביצוע בקשה ל-OpenAI
            String jsonResponse = uploadToOpenAI(tempFile);
            System.out.println("jsonResponse: " + jsonResponse);

            String json = jsonResponse.substring(jsonResponse.indexOf("{") , jsonResponse.lastIndexOf("```"));

            JSONObject jsonObject = new JSONObject(json);

            // חילוץ כל מאפיין
            String type = jsonObject.getString("type");
            String style = jsonObject.getString("style");
            String color = jsonObject.getString("color");
            String season = jsonObject.getString("season");
            String description = jsonObject.getString("description");

            System.out.println("Type: " + type);
            System.out.println("Style: " + style);
            System.out.println("Color: " + color);
            System.out.println("Season: " + season);
            System.out.println("Description: " + description);

            String imageURL = null;
            try {
                imageURL = uploadImageToImgur(file);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            String name = imageURL.substring(imageURL.lastIndexOf('/') + 1, imageURL.lastIndexOf('.'));
            OutfitItem outfitItem = new OutfitItem(getUserById(userId),name,type,style,color,season,description);
            save(outfitItem);
            // מחיקת הקובץ הזמני
            tempFile.delete();

            System.out.println("ResponseEntity.ok(imageUrl):  " + ResponseEntity.ok(jsonResponse));
            return ResponseEntity.ok(jsonResponse);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error uploading image: " + e.getMessage());
        }
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
                .addHeader("Authorization", "Client-ID " + IMGUR_CLIENT_ID)
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
//        contentArray.put(new JSONObject().put("type", "text").put("text", "return JSON with the features for the item in the image: type(shirt,pants...),style(elegant...),color,season and (short) description"));
        contentArray.put(new JSONObject()
                .put("type", "text")
                .put("text", "Return JSON with the features for the item in the image: type (shirt, pants...), style (elegant...), color in one word (if dominant, otherwise 'colorful'), season (summer, winter, fall, spring; if suitable for multiple seasons, separate with '/') and a short description."));
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

    public void deleteImageFromImgur(String deleteHash) throws IOException {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url("https://api.imgur.com/3/image/" + deleteHash)
                .delete(null)
                .addHeader("Authorization", "Client-ID " + IMGUR_CLIENT_ID)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to delete image from Imgur");
            }
        }
    }

}