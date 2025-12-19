package ru.itis.scrabble.services;

public interface SecurityService {
    void signupUser(String username, String password, String passwordRepeat);
    void loginUser(String username, String password);
}