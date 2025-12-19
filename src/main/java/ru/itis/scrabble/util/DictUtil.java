package ru.itis.scrabble.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

public class DictUtil {
    private static final Set<String> dictionary = new HashSet<>();
    private static final String DICTIONARY_PATH = "dictionary.txt";
    private static boolean isLoaded = false;

    static {
        loadDictionary();
    }

    private static void loadDictionary() {
        try {
            Path path = Paths.get(DICTIONARY_PATH);
            if (Files.exists(path)) {
                Files.lines(path)
                        .map(String::trim)
                        .map(String::toUpperCase)
                        .filter(word -> word.length() > 1) // Игнорируем одиночные буквы
                        .forEach(dictionary::add);
                System.out.println("Словарь загружен: " + dictionary.size() + " слов");
                isLoaded = true;
            } else {
                System.out.println("Файл словаря не найден: " + DICTIONARY_PATH);
                // Загружаем небольшой встроенный словарь для тестирования
                loadDefaultDictionary();
            }
        } catch (IOException e) {
            System.err.println("Ошибка загрузки словаря: " + e.getMessage());
            loadDefaultDictionary();
        }
    }

    private static void loadDefaultDictionary() {
        // Небольшой набор слов для тестирования
        String[] defaultWords = {
                "CAT", "DOG", "HOUSE", "COMPUTER", "SCRIBBLE", "GAME",
                "PLAY", "WORD", "LETTER", "BOARD", "TILE", "SCORE",
                "TEST", "HELLO", "WORLD", "JAVA", "PROGRAM", "SERVER"
        };
        for (String word : defaultWords) {
            dictionary.add(word);
        }
        isLoaded = true;
        System.out.println("Загружен тестовый словарь: " + dictionary.size() + " слов");
    }

    public static boolean isValidWord(String word) {
        if (!isLoaded) {
            loadDictionary();
        }
        return dictionary.contains(word.toUpperCase());
    }

    public static int getWordCount() {
        return dictionary.size();
    }

    public static void addWord(String word) {
        dictionary.add(word.toUpperCase());
    }
}