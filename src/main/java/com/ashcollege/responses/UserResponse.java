package com.ashcollege.responses;

import com.ashcollege.entities.User;

public class UserResponse extends BasicResponse {
    private User user;

    public UserResponse(boolean success, Integer errorCode, User user) {
        super(success, errorCode);
        this.user = user;
    }

    public User getUser() {
        return user;
    }
}
