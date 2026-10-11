package dev.aliska.bocchi;

/**
 * Проверка музыкальных фраз репетиций:
 * - Все ноты строго укладываются в диапазон ванильного нотного блока (0..24).
 * - Музыкальные фразы развиваются и превышают короткие восьмидолевые циклы.
 */
public final class MusicCheck {
    public static void main(String[] args) {
        for (int heroine = 0; heroine < 7; heroine++) {
            boolean longerThanEight = false;
            for (int beat = 0; beat < 2048; beat++) {
                int note = OriginalMusic.note(heroine, beat);
                if (note < 0 || note > 24) {
                    throw new AssertionError("Outside note-block range: heroine=" + heroine + ", note=" + note);
                }
                if (beat >= 8 && beat < 32 && note != OriginalMusic.note(heroine, beat % 8)) {
                    longerThanEight = true;
                }
            }
            if (!longerThanEight) {
                throw new AssertionError("Short eight-note loop instead of long phrase for heroine " + heroine);
            }
        }
        System.out.println("PASS: original phrases stay in note-block range and exceed eight-note loops.");
    }
}
