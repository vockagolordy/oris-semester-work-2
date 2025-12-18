package ru.itis.scrabble.models;

/**
 * Перечисление фишек для игры Scrabble.
 * <p>
 * Определяет все возможные фишки с их буквами и соответствующим количеством очков.
 * Распределение очков соответствует классическим правилам английского Scrabble.
 * </p>
 *
 * <br>
 * <b>Значения перечисления:</b>
 * <ul>
 *     <li>{@code BLANK} - пустая фишка (джокер), 0 очков</li>
 *     <li>Фишки стоимостью 1 очко: {@code A, E, I, L, N, O, R, S, T, U}</li>
 *     <li>Фишки стоимостью 2 очка: {@code D, G}</li>
 *     <li>Фишки стоимостью 3 очка: {@code B, C, M, P}</li>
 *     <li>Фишки стоимостью 4 очка: {@code F, H, V, W, Y}</li>
 *     <li>Фишки стоимостью 5 очков: {@code K}</li>
 *     <li>Фишки стоимостью 8 очков: {@code J, X}</li>
 *     <li>Фишки стоимостью 10 очков: {@code Q, Z}</li>
 * </ul>
 *
 * <br>
 * <b>Атрибуты каждого значения:</b>
 * <ul>
 *     <li>{@code points} - количество очков за фишку</li>
 *     <li>{@code letter} - символ фишки</li>
 * </ul>
 */
public enum Tile {
    A(1, 'A'),
    E(1, 'E'),
    I(1, 'I'),
    L(1, 'L'),
    N(1, 'N'),
    O(1, 'O'),
    R(1, 'R'),
    S(1, 'S'),
    T(1, 'T'),
    U(1, 'U'),

    D(2, 'D'),
    G(2, 'G'),

    B(3, 'B'),
    C(3, 'C'),
    M(3, 'M'),
    P(3, 'P'),

    F(4, 'F'),
    H(4, 'H'),
    V(4, 'V'),
    W(4, 'W'),
    Y(4, 'Y'),

    K(5, 'K'),

    J(8, 'J'),
    X(8, 'X'),

    Q(10, 'Q'),
    Z(10, 'Z');

    private final int points;
    private final char letter;

    Tile(int points, char letter) {
        this.points = points;
        this.letter = letter;
    }

    public int getPoints() {
        return points;
    }

    public char getLetter() {
        return letter;
    }
}