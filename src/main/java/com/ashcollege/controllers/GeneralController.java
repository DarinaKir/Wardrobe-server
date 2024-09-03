package com.ashcollege.controllers;

import com.ashcollege.Persist;
import com.ashcollege.entities.OutfitItem;
import com.ashcollege.entities.OutfitSuggestion;
import com.ashcollege.responses.BasicResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import javax.annotation.PostConstruct;

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
}
