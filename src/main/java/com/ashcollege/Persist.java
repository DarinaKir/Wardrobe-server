package com.ashcollege;


import com.ashcollege.entities.OutfitItem;
import com.ashcollege.entities.OutfitSuggestion;
import com.ashcollege.entities.User;
import com.ashcollege.responses.BasicResponse;
import com.ashcollege.responses.ImgurUploadResponse;
import com.ashcollege.responses.UserResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import okhttp3.MediaType;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.google.gson.*;
import org.apache.poi.ss.usermodel.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.io.*;
import java.net.URL;

import static com.ashcollege.utils.Errors.*;

@Transactional
@Component
@SuppressWarnings("unchecked")
public class Persist {

    private static final Logger LOGGER = LoggerFactory.getLogger(Persist.class);

    private final SessionFactory sessionFactory;

    private static final String API_KEY = "";
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private final List<OutfitItem> outfits = new ArrayList<>();

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

        }catch (Exception e) {
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
                                }else {
                                    errorCode = EMAIL_ALREADY_IN_USE;
                                }
                            }else {
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

    private List<String> getSeasonArray(String season) {
        List<String> seasons = new ArrayList<>();
        if (season.equals("all") || season.equals("all season")) {
            seasons = List.of("winter", "spring", "summer", "fall");
        }else {
            String[] splitSeasons = season.split("/");
            seasons = Arrays.asList(splitSeasons);
        }
        return seasons;
    }

    private List<OutfitItem> parseOutfitJson(JsonObject outfitSuggestionJson) {
        List<OutfitItem> outfitItems = new LinkedList<>();
        for (String key : outfitSuggestionJson.keySet()) {
            if (!key.equals("explanation")) {
                JsonElement element = outfitSuggestionJson.get(key);

                // Assuming all IDs are integers
                int id = element.getAsInt();

                OutfitItem outfit = outfits.stream()
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

    public void extractOutfitItemsFromExcel () {
        String excelFilePath = "src/main/java/com/ashcollege/files/Classification of clothes.xlsx";
        int startRow = 1; // Start from row 2 (index 1)
        int endRow = 49;  // End at row 50 (index 49)
        try (FileInputStream fileInputStream = new FileInputStream(new File(excelFilePath));
             Workbook workbook = WorkbookFactory.create(fileInputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            // Iterate over the rows from startRow to endRow
            for (int i = startRow; i <= endRow && i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    OutfitItem outfitItem = new OutfitItem(i);
                    for (int j = 1; j < 6; j++) {
                        Cell cell = row.getCell(j);
                        if (cell != null) {
                            switch (j) {
                                case 1:
                                    outfitItem.setType(cell.getStringCellValue());
                                    break;
                                case 2:
                                    outfitItem.setStyle(cell.getStringCellValue());
                                    break;
                                case 3:
                                    outfitItem.setColor(cell.getStringCellValue());
                                    break;
                                case 4:
                                    outfitItem.setSeason(cell.getStringCellValue());
                                    break;
                                case 5:
                                    outfitItem.setDescription(cell.getStringCellValue());
                                    break;
                            }
                        }
                    }
                    outfits.add(outfitItem);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Values in the sixth column from row " + (startRow + 1) + " to row " + (endRow + 1) + ":");
        for (OutfitItem outfitItem : outfits) {
            System.out.println(outfitItem);
        }
    }

    public List<OutfitSuggestion> sendOutfitRequest (String answer) {
        Gson gson = new Gson();
        JsonArray jsonArray = new JsonArray();

        for (OutfitItem outfitItem : outfits) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("id", outfitItem.getId());
            jsonObject.addProperty("type", outfitItem.getType());
            jsonObject.addProperty("style", outfitItem.getStyle());
            jsonObject.addProperty("color", outfitItem.getColor());
            jsonObject.addProperty("season", outfitItem.getSeason());
            jsonObject.addProperty("description", outfitItem.getDescription());
            jsonArray.add(jsonObject);
        }
        String clothes = gson.toJson(jsonArray);

        String requestPayload = gson.toJson(Map.of(
                "model", "gpt-4o",
                "messages", List.of(
                        Map.of("role", "system", "content", "You are a helpful assistant."),
                        Map.of("role", "user", "content", "You are a stylist. Choose 3 outfits (each must include either a top, bottom, or dress, plus shoes; bag and other accessories are optional) for" + answer + "from the following items. Ensure the colors match. Return a JsonArray with each outfit as a JsonObject. Use the following naming convention for the item IDs in the JSON: \"top\", \"bottom\", \"dress\", \"shoes\", \"accessory\". Each outfit should also include an explanation for your choices. Only include the IDs and explanation in the JSON: " + clothes
                        )
                )

                // old request in case we want to use it:
                //You are a stylist, choose a look (shirt and pants/skirt or dresses/suits, you can add accessories and suitable shoes) for a party from the following items. Note that the colors match, return JSON with only their ID:" + clothes
        ));

// Print the request payload to debug
        System.out.println("Request Payload:");
        System.out.println(requestPayload);

// Send request to OpenAI API
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Authorization", "Bearer " + API_KEY)
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
            JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();
            JsonArray choicesArray = jsonResponse.getAsJsonArray("choices");

            // Get the content string from the big Json inside choicesArray
            JsonObject firstChoice = choicesArray.get(0).getAsJsonObject();
            String content = firstChoice.getAsJsonObject("message").get("content").getAsString();

            String cleanedContent = content.replaceAll("```json\\n|\\n```", "").replace("\\n", "\n");

            try {
                JsonArray outfitSuggestionsArray = JsonParser.parseString(cleanedContent).getAsJsonArray();

                for (int i = 0; i < outfitSuggestionsArray.size(); i++) {
                    List<OutfitItem> itemsOfSuggestion = new LinkedList<>();
                    JsonObject outfitSuggestionJson = outfitSuggestionsArray.get(i).getAsJsonObject();
                    String explanation = outfitSuggestionJson.get("explanation").getAsString();
                    itemsOfSuggestion = parseOutfitJson(outfitSuggestionJson);
                    OutfitSuggestion outfitSuggestion = new OutfitSuggestion(itemsOfSuggestion, explanation);
                    outfitSuggestions.add(outfitSuggestion);
                }
                //PRINTS OUTFIT SUGGESTION ARRAY !!! :)
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

    public List<OutfitItem> getOutfits() {
        return outfits;
    }

}