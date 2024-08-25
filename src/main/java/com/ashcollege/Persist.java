package com.ashcollege;


import com.ashcollege.entities.User;
import com.ashcollege.responses.BasicResponse;
import com.ashcollege.responses.UserResponse;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.ashcollege.utils.Errors.*;

@Transactional
@Component
@SuppressWarnings("unchecked")
public class Persist {

    private static final Logger LOGGER = LoggerFactory.getLogger(Persist.class);

    private final SessionFactory sessionFactory;


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
}