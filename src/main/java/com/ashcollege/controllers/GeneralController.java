package com.ashcollege.controllers;

import com.ashcollege.Persist;
import com.ashcollege.responses.BasicResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import static com.ashcollege.utils.Errors.*;

@RestController
public class GeneralController {

    @Autowired
    private Persist persist;

//    @RequestMapping(value = "/", method = {RequestMethod.GET, RequestMethod.POST})
//    public Object hello() {
//        return "Hello From Server";
//    }

    @RequestMapping(value = "/", method = {RequestMethod.GET, RequestMethod.POST})
    public Object test() {
        return "test";
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
