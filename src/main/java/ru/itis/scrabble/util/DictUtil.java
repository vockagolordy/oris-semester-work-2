package ru.itis.scrabble.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

/**
 * Утилитарный класс для лингвистической проверки слов.
 */
public class DictUtil {
    private static final Set<String> dictionary = new HashSet<>();
    private static final String WORDS_FILE = "/words.txt";

    static {
        loadDictionary();
    }

    /**
     * Загружает слова из файла ресурсов в HashSet при старте приложения.
     */
    private static void loadDictionary() {
        try (InputStream is = DictUtil.class.getResourceAsStream(WORDS_FILE)) {
            if (is == null) {
                System.err.println("Файл словаря не найден: " + WORDS_FILE);
                return;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String word;
                while ((word = reader.readLine()) != null) {
                    word = word.trim().toUpperCase();
                    if (!word.isEmpty()) {
                        dictionary.add(word);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Ошибка при загрузке словаря: " + e.getMessage());
        }
    }

    /**
     * Проверяет, существует ли слово в словаре.
     * @param word Слово для проверки.
     * @return true, если слово найдено.
     */
    public static boolean isWordValid(String word) {
        if (word == null) return false;
        return dictionary.contains(word.toUpperCase());
    }

    /**
     * Позволяет проверить список слов (например, основное и перпендикулярные).
     */
    public static boolean areAllWordsValid(Iterable<String> words) {
        for (String word : words) {
            if (!isWordValid(word)) {
                return false;
            }
        }
        return true;
    }
}