"""One localization policy for entities, summon tokens and hidden model items."""

import json
from pathlib import Path

LANG_DIR = Path(__file__).resolve().parents[1] / 'src/main/resources/assets/bocchi/lang'

NAMES = {
    'ru_ru': ['Хитори Гото', 'Нидзика Идзити', 'Рё Ямада', 'Икуё Кита', 'Сейка Идзити', 'Кикури Хирои', 'ПА-сан'],
    'be_by': ['Хіторы Гота', 'Нідзіка Ідзіці', 'Ро Ямада', 'Ікуё Кіта', 'Сейка Ідзіці', 'Кікуры Хіроі', 'ПА-сан'],
    'en_us': ['Hitori Gotoh', 'Nijika Ijichi', 'Ryo Yamada', 'Ikuyo Kita', 'Seika Ijichi', 'Kikuri Hiroi', 'PA-san']
}


def standardize():
    for locale, names in NAMES.items():
        path = LANG_DIR / (locale + '.json')
        data = json.loads(path.read_text(encoding='utf-8'))

        forms = {
            'ru_ru': ['тсум', 'чиби'],
            'be_by': ['тсум', 'чыбі'],
            'en_us': ['Tsum', 'Chibi']
        }[locale]

        moods = {
            'ru_ru': ['радость', 'сон', 'удивление', 'зажмуренные глаза'],
            'be_by': ['радасць', 'сон', 'здзіўленне', 'заплюшчаныя вочы'],
            'en_us': ['Happy', 'Sleepy', 'Surprised', 'Squinting']
        }[locale]

        for kind, name in zip(['bocchi', 'nijika', 'ryo', 'kita', 'seika', 'kikuri', 'pa_san'], names):
            for form, label in zip(['plush', 'chibi'], forms):
                stem = f'{kind}_{form}'
                title = f'{name} · {label}'
                data[f'entity.bocchi.{stem}'] = title

                prefix = {'ru_ru': 'Позвать: ', 'be_by': 'Паклікаць: ', 'en_us': 'Summon: '}[locale]
                data[f'item.bocchi.{stem}_summoner'] = prefix + title

                model_suffix = {'ru_ru': 'модель', 'be_by': 'мадэль', 'en_us': 'Model'}[locale]
                data[f'item.bocchi.{stem}_model'] = f'{title} · {model_suffix}'

                for mood, word in zip(['happy', 'sleepy', 'surprised', 'squint'], moods):
                    data[f'item.bocchi.{stem}_{mood}_model'] = f'{title} · {word}'

            figurine_prefix = {'ru_ru': 'Фигурка: ', 'be_by': 'Фігурка: ', 'en_us': 'Figurine: '}[locale]
            data[f'block.bocchi.{kind}_figurine'] = figurine_prefix + name

        movement_messages = {
            'ru_ru': ['Следовать при удалении больше 32 блоков', 'Держаться рядом', 'Гулять в радиусе 16 блоков'],
            'be_by': ['Ісці следам пры адлегласці больш за 32 блокі', 'Трымацца побач', 'Гуляць у радыусе 16 блокаў'],
            'en_us': ['Follow beyond 32 blocks', 'Stay close', 'Roam within 16 blocks']
        }[locale]

        for mode, text in zip(['follow', 'close', 'roam'], movement_messages):
            data[f'message.bocchi.movement.{mode}'] = text

        data['itemGroup.bocchi.little_stage'] = 'BTR World'
        path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')


if __name__ == '__main__':
    standardize()
