package dev.aliska.bocchi;

/**
 * Проверка старых автоматически сгенерированных русских имён спутниц
 * для их корректной миграции на стандартную систему переводов.
 */
public final class CompanionNames {
    private CompanionNames() {}

    /**
     * Возвращает true, если переданное имя совпадает с одним из автоматически генерировавшихся в старых версиях.
     */
    public static boolean isLegacyGeneratedName(String name) {
        for (String heroine : new String[]{"Хитори", "Нидзика", "Рё", "Кита", "Сейка", "Кикури", "ПА-сан"}) {
            for (String form : new String[]{"тсум", "чиби"}) {
                if (name.equals(heroine + " · " + form)) {
                    return true;
                }
            }
        }
        return false;
    }
}
