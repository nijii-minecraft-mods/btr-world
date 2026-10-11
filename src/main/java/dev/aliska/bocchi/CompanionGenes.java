package dev.aliska.bocchi;

import java.util.random.RandomGenerator;

/**
 * Генетика спутниц: типы родителей и глубина родословной с взвешенным наследованием героини.
 */
public record CompanionGenes(int heroineA, int heroineB, int generation) {
    // Веса наследования: Боччи (1), Нидзика (3), Рё (2), Кита (2), Сейка (2), Кикури (2), ПА-сан (1)
    private static final int[] WEIGHTS = {1, 3, 2, 2, 2, 2, 1};

    public CompanionGenes {
        heroineA = clamp(heroineA);
        heroineB = clamp(heroineB);
        generation = Math.max(0, Math.min(1024, generation));
    }

    private static int clamp(int n) {
        return Math.max(0, Math.min(6, n));
    }

    /**
     * Создаёт новый набор генов потомка на основе данных родителей.
     */
    public static CompanionGenes cross(int firstHeroine, int firstGeneration, int secondHeroine, int secondGeneration) {
        return new CompanionGenes(
            firstHeroine,
            secondHeroine,
            Math.max(firstGeneration, secondGeneration) + 1
        );
    }

    /**
     * Выбирает героиню для потомка на основе весов родителей.
     */
    public int heroine(RandomGenerator r) {
        int weightA = WEIGHTS[heroineA];
        int weightB = WEIGHTS[heroineB];
        return r.nextInt(weightA + weightB) < weightA ? heroineA : heroineB;
    }
}
