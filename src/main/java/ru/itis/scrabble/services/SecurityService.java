package ru.itis.scrabble.services;

import ru.itis.scrabble.models.User;

public interface SecurityService {
    void signupUser(String username, String password, String passwordRepeat);

    void loginUser(String username, String password);

    boolean logoutUser();
}
