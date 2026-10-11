"""Generate the Russian crafting reference from the actual recipe JSON."""

import collections
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RESOURCES = ROOT / 'src/main/resources'
LANG = json.loads((RESOURCES / 'assets/bocchi/lang/ru_ru.json').read_text(encoding='utf-8'))

VANILLA_NAMES = dict(zip(
    'amethyst_shard black_dye blue_dye brown_dye clay_ball cookie glass_pane glow_ink_sac glowstone_dust '
    'gold_ingot gold_nugget golden_apple iron_ingot iron_nugget leather leather_boots leather_chestplate '
    'leather_helmet leather_leggings lime_wool note_block oak_planks oak_slab paper pink_dye pink_wool '
    'quartz red_dye redstone slime_ball smooth_stone stick string white_concrete white_dye white_wool yellow_dye'.split(),
    ['осколок аметиста', 'чёрный краситель', 'синий краситель', 'коричневый краситель', 'комок глины',
     'печенье', 'стеклянная панель', 'светящийся чернильный мешок', 'светокаменная пыль', 'золотой слиток',
     'кусочек золота', 'золотое яблоко', 'железный слиток', 'кусочек железа', 'кожа', 'кожаные ботинки',
     'кожаная куртка', 'кожаный шлем', 'кожаные штаны', 'лаймовая шерсть', 'нотный блок', 'дубовые доски',
     'дубовая плита', 'бумага', 'розовый краситель', 'розовая шерсть', 'кварц', 'красный краситель',
     'редстоун', 'слизь', 'гладкий камень', 'палка', 'нить', 'белый бетон', 'белый краситель', 'белая шерсть',
     'жёлтый краситель']
))


def get_display_name(identifier: str) -> str:
    namespace, path = identifier.split(':', 1)
    if namespace == 'minecraft':
        return VANILLA_NAMES.get(path, identifier)
    return LANG.get('block.' + namespace + '.' + path, LANG.get('item.' + namespace + '.' + path, identifier))


def main():
    groups = collections.defaultdict(list)

    for recipe_file in sorted((RESOURCES / 'data/bocchi/recipe').glob('*.json')):
        recipe = json.loads(recipe_file.read_text(encoding='utf-8'))
        ident = recipe_file.stem

        if any(ident.endswith('_' + x) for x in ['matte', 'tile', 'lamp']):
            group = 'Палитра блоков'
        elif ident.endswith('_summoner'):
            group = 'Призыв спутниц'
        elif ident.endswith(('_figurine', '_plush')):
            group = 'Фигурки и игрушки'
        elif ident in [
            'mango_box_hat', 'nijika_ahoge', 'bocchi_hairclip', 'nijika_ribbon',
            'ryo_hairclip', 'kita_hairclip', 'tsuchinoko_hat',
            'stop_sign', 'pink_hood', 'pink_jacket', 'pink_pants', 'pink_shoes'
        ]:
            group = 'Одежда и аксессуары'
        else:
            group = 'Сцена и декор'

        if 'pattern' in recipe:
            pattern = ' / '.join(row.replace(' ', '·') for row in recipe['pattern'])
            counter = collections.Counter(''.join(recipe['pattern']))
            ingredients = [
                f"{symbol} = {get_display_name(value['item'])} ×{counter[symbol]}"
                for symbol, value in recipe['key'].items()
            ]
            how = f'`{pattern}`'
            parts = '; '.join(ingredients)
        else:
            counter = collections.Counter(i['item'] for i in recipe['ingredients'])
            how = 'Любой порядок'
            parts = '; '.join(f'{get_display_name(item)} ×{count}' for item, count in counter.items())

        result_name = get_display_name(recipe['result']['id'])
        output_count = recipe['result'].get('count', 1)
        groups[group].append(f"| {result_name} (`{ident}`) | {how} | {parts} | {output_count} |")

    lines = [
        '<!-- CRAFT GUIDE -->',
        '## Полный справочник крафтов',
        '',
        'Все рецепты выполняются на верстаке. В схеме строки отделены `/`, знак `·` означает пустую ячейку. '
        'Например, `PPP / PIP / PPP`: три доски сверху, доска–ингредиент–доска посередине и три доски снизу. '
        'Буквы указаны в колонке ингредиентов; количества относятся ко всему рецепту. «Любой порядок» — бесформенный рецепт, расположение не важно.',
        '',
        'Предметы имеют префикс `bocchi:`. Рецепты доступны в книге рецептов после входа; '
        'команда оператора `/recipe give @s *` открывает все рецепты. Для просмотра в JEI нужны клиентские JEI, BTR World и его зависимости.',
        ''
    ]

    for group in ['Призыв спутниц', 'Палитра блоков', 'Фигурки и игрушки', 'Сцена и декор', 'Одежда и аксессуары']:
        lines += [
            '### ' + group,
            '',
            '| Предмет / ID | Схема | Ингредиенты | Выход |',
            '|---|---|---|---|',
            *groups[group],
            ''
        ]

    crafting_md = ROOT / 'docs/CRAFTING.md'
    crafting_md.write_text('# Крафты BTR World\n\n' + '\n'.join(lines) + '\n', encoding='utf-8')

    total_recipes = sum(map(len, groups.values()))
    readme = ROOT / 'README.md'
    base = readme.read_text(encoding='utf-8').split('<!-- CRAFT GUIDE -->')[0].rstrip()
    readme.write_text(
        base + f'\n\n<!-- CRAFT GUIDE -->\n<details>\n<summary>Полный справочник всех {total_recipes} рецептов</summary>\n\n'
        + '\n'.join(lines[1:]) + '\n</details>\n',
        encoding='utf-8'
    )

    print(f'Craft guide: {total_recipes} recipes.')


if __name__ == '__main__':
    main()
