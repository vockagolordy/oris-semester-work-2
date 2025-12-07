package ru.itis.scrabble.models;

/**
 * Перечисление типов клеток игрового поля.
 * <p>
 * Определяет различные бонусные клетки, которые могут влиять на подсчет очков
 * при размещении фишек на игровом поле.
 * </p>
 *
 * <br>
 * <b>Значения перечисления:</b>
 * <ul>
 *     <li>{@code NONE} - клетка без бонусных эффектов</li>
 *     <li>{@code DLS} - Double Letter Score (удвоение очков за букву)</li>
 *     <li>{@code TLS} - Triple Letter Score (утроение очков за букву)</li>
 *     <li>{@code DWS} - Double Word Score (удвоение очков за все слово)</li>
 *     <li>{@code TWS} - Triple Word Score (утроение очков за все слово)</li>
 * </ul>
 */
public enum CellType {
    NONE,
    DLS,
    TLS,
    DWS,
    TWS
}
