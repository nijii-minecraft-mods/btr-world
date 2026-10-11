"""Generate textures, 3D item models, recipes, loot tables and localizations for costumes, panties and club loot."""
from pathlib import Path
from PIL import Image, ImageDraw
import json

ROOT = Path(__file__).resolve().parents[1] / 'src/main/resources'
AS = ROOT / 'assets/bocchi'
DATA = ROOT / 'data/bocchi'

for d in ['textures/block/model', 'models/item', 'lang']:
    (AS / d).mkdir(parents=True, exist_ok=True)
for d in ['recipe', 'loot_table/chests']:
    (DATA / d).mkdir(parents=True, exist_ok=True)

def js(path, obj):
    path.write_text(json.dumps(obj, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

# Helper for element cuboids
def element(a, b, tex, north=None, rotation=None):
    faces = {s: {'uv': [0, 0, 16, 16], 'texture': '#' + tex} for s in ['north', 'south', 'east', 'west', 'up', 'down']}
    if north:
        faces['north']['texture'] = '#' + north
    res = {'from': a, 'to': b, 'faces': faces}
    if rotation:
        res['rotation'] = rotation
    return res

# --- 1. Пиксельные текстуры ---

# Тетрапак они-напитка Кикури (зелёный бамбук + красные и белые иероглифы)
im = Image.new('RGB', (32, 32), '#387346')
d = ImageDraw.Draw(im)
d.rectangle((2, 2, 29, 29), outline='#eedb9d', width=1)
d.rectangle((5, 5, 26, 12), fill='#8a2638')
d.text((7, 5), '鬼殺', fill='#fff5e7')
d.rectangle((5, 15, 26, 26), fill='#f5ebd5')
d.text((7, 16), 'SICK', fill='#2d5a37')
im.save(AS / 'textures/block/model/kikuri_pack.png')

# Банка газировки Симокитадзавы
im = Image.new('RGB', (16, 16), '#53a5c7')
d = ImageDraw.Draw(im)
d.rectangle((0, 0, 15, 2), fill='#b8c5cc')
d.rectangle((0, 13, 15, 15), fill='#b8c5cc')
d.line([(3, 5), (12, 10)], fill='#fff5e7', width=1)
d.point([(5, 4), (10, 6), (7, 9), (11, 11)], fill='#cbf0ff')
im.save(AS / 'textures/block/model/soda.png')

# Концертный билет STARRY
im = Image.new('RGB', (32, 16), '#ffd275')
d = ImageDraw.Draw(im)
d.rectangle((0, 0, 31, 15), outline='#9a7322', width=1)
d.line([(22, 0), (22, 15)], fill='#9a7322', width=1)
for y in range(1, 15, 3):
    d.point((22, y), fill='#ffd275')
d.rectangle((4, 3, 18, 12), fill='#342a34')
d.text((6, 4), 'STARRY', fill='#ffd275')
im.save(AS / 'textures/block/model/starry_ticket.png')

# Гитарный медиатор
im = Image.new('RGB', (16, 16), '#ef627a')
d = ImageDraw.Draw(im)
d.polygon([(8, 14), (2, 3), (14, 3)], fill='#e84a65', outline='#ffe28a')
d.polygon([(8, 6), (7, 9), (9, 9)], fill='#ffe28a')
im.save(AS / 'textures/block/model/pick.png')

# Кружева ободка горничной
im = Image.new('RGB', (16, 16), '#fbfbfb')
d = ImageDraw.Draw(im)
for x in range(0, 16, 4):
    d.arc((x, 0, x + 3, 6), 0, 180, fill='#27222c', width=1)
    d.rectangle((x + 1, 7, x + 2, 15), fill='#e3dbe8')
im.save(AS / 'textures/block/model/maid_lace.png')

# Текстуры трусиков для каждой героини
panty_colors = {
    'bocchi': ('#f5a7cc', '#e8f4fa', '#f5db76'), # розовые в полоску с сине-желтыми акцентами
    'nijika': ('#fce38a', '#ffffff', '#e84a65'), # желто-белые с красным бантиком
    'ryo': ('#5b7aa6', '#e0ecf8', '#ffffff'),     # сине-голубые минималистичные
    'kita': ('#e65965', '#ffffff', '#ffd275'),    # кораллово-красные со звездочкой
    'seika': ('#28262c', '#403c47', '#ffd275'),   # черные кружевные
    'kikuri': ('#943b7e', '#457d54', '#eedb9d'),  # пурпурно-зеленые
    'pa_san': ('#383747', '#5d5c70', '#9cb2bd')   # темно-готические с металлическим кольцом
}

for kind, (c1, c2, c3) in panty_colors.items():
    im = Image.new('RGB', (16, 16), c1)
    d = ImageDraw.Draw(im)
    for y in range(0, 16, 4):
        d.line([(0, y), (15, y)], fill=c2, width=1)
    d.rectangle((6, 2, 9, 5), fill=c3)
    im.save(AS / f'textures/block/model/panties_{kind}.png')

# Регистрируем текстуры в словаре
tex = {
    'kikuri_pack': 'bocchi:block/model/kikuri_pack',
    'soda': 'bocchi:block/model/soda',
    'starry_ticket': 'bocchi:block/model/starry_ticket',
    'pick': 'bocchi:block/model/pick',
    'maid_lace': 'bocchi:block/model/maid_lace',
    'white': 'bocchi:block/model/white',
    'black': 'bocchi:block/model/black',
    'metal': 'bocchi:block/model/metal',
    'cardboard': 'bocchi:block/model/cardboard',
    'wood': 'bocchi:block/model/wood',
    'red': 'bocchi:block/model/red',
    'yellow': 'bocchi:block/model/yellow',
    'blue': 'bocchi:block/model/blue',
    'pink': 'bocchi:block/model/pink'
}
for kind in panty_colors:
    tex[f'panties_{kind}'] = f'bocchi:block/model/panties_{kind}'

def make_model(elements, display=None):
    obj = {'textures': tex, 'elements': elements}
    if display:
        obj['display'] = display
    else:
        obj['display'] = {
            'gui': {'rotation': [20, 135, 0], 'translation': [0, 0, 0], 'scale': [0.85, 0.85, 0.85]},
            'ground': {'translation': [0, 2, 0], 'scale': [0.5, 0.5, 0.5]},
            'thirdperson_righthand': {'rotation': [0, 0, 0], 'translation': [0, 2, 0], 'scale': [0.6, 0.6, 0.6]},
            'firstperson_righthand': {'rotation': [0, 45, 0], 'translation': [0, 1, 0], 'scale': [0.7, 0.7, 0.7]}
        }
    return obj

# --- 2. Модели 3D предметов ---

# Ободок горничной (maid_headband)
e_headband = [
    element([3.5, 15.0, 7.0], [12.5, 17.0, 9.0], 'white'),
    element([3.0, 16.5, 6.8], [13.0, 19.0, 9.2], 'maid_lace'),
    element([2.8, 12.0, 7.5], [3.8, 16.0, 8.5], 'black'),
    element([12.2, 12.0, 7.5], [13.2, 16.0, 8.5], 'black'),
    element([2.0, 11.0, 7.0], [3.5, 13.0, 9.0], 'white'),
    element([12.5, 11.0, 7.0], [14.0, 13.0, 9.0], 'white')
]
m_headband = make_model(e_headband)
m_headband['display']['head'] = {'rotation': [0, 0, 0], 'translation': [0, 0, 0], 'scale': [1, 1, 1]}
m_headband['display']['gui'] = {'rotation': [15, -45, 0], 'translation': [0, -3, 0], 'scale': [0.85, 0.85, 0.85]}
js(AS / 'models/item/maid_headband.json', m_headband)

# Напиток Кикури в тетрапаке (kikuri_drink)
e_drink = [
    element([5.0, 0.0, 6.0], [11.0, 11.0, 10.0], 'kikuri_pack'),
    element([5.5, 11.0, 6.5], [10.5, 12.5, 9.5], 'kikuri_pack'),
    element([9.0, 12.0, 7.5], [10.0, 15.5, 8.5], 'white') # трубочка
]
js(AS / 'models/item/kikuri_drink.json', make_model(e_drink))

# Смятый пустой тетрапак (empty_pack)
e_empty = [
    element([5.0, 0.0, 6.0], [11.0, 3.5, 10.0], 'kikuri_pack', rotation={'origin': [8, 2, 8], 'axis': 'z', 'angle': -22.5}),
    element([6.0, 3.0, 6.5], [10.5, 5.0, 9.5], 'cardboard')
]
js(AS / 'models/item/empty_pack.json', make_model(e_empty))

# Банка газировки (shimokitazawa_soda)
e_soda = [
    element([5.5, 0.0, 5.5], [10.5, 10.0, 10.5], 'soda'),
    element([6.0, 10.0, 6.0], [10.0, 10.5, 10.0], 'metal')
]
js(AS / 'models/item/shimokitazawa_soda.json', make_model(e_soda))

# Медиатор Kessoku Band (guitar_pick)
e_pick = [
    element([4.0, 2.0, 7.5], [12.0, 12.0, 8.5], 'pick')
]
js(AS / 'models/item/guitar_pick.json', make_model(e_pick))

# Билет на концерт STARRY (starry_ticket)
e_ticket = [
    element([1.0, 4.0, 7.8], [15.0, 12.0, 8.2], 'starry_ticket')
]
js(AS / 'models/item/starry_ticket.json', make_model(e_ticket))

# Уникальные 3D-модели трусиков для каждой героини
for kind in panty_colors:
    e_panty = [
        element([3.5, 5.0, 6.5], [12.5, 10.0, 9.5], f'panties_{kind}'),
        element([4.5, 2.0, 7.0], [11.5, 5.0, 9.0], f'panties_{kind}'),
        element([6.0, 0.5, 7.2], [10.0, 2.0, 8.8], f'panties_{kind}'),
        element([7.0, 9.0, 6.0], [9.0, 10.5, 7.0], 'white' if kind != 'bocchi' else 'pink')
    ]
    # Характерные украшения
    if kind == 'bocchi':
        e_panty.append(element([9.2, 8.5, 6.0], [10.8, 10.1, 6.8], 'blue'))
        e_panty.append(element([9.2, 6.8, 6.0], [10.8, 8.4, 6.8], 'yellow'))
    elif kind == 'nijika':
        e_panty.append(element([6.5, 9.5, 5.8], [9.5, 11.0, 6.6], 'red'))
    elif kind == 'ryo':
        e_panty.append(element([7.2, 8.8, 6.0], [8.8, 10.4, 6.6], 'white'))
    elif kind == 'kita':
        e_panty.append(element([7.0, 8.6, 5.8], [9.0, 10.6, 6.6], 'yellow'))
    elif kind == 'seika':
        e_panty.append(element([7.3, 8.8, 5.9], [8.7, 10.2, 6.5], 'yellow'))
    elif kind == 'pa_san':
        e_panty.append(element([7.5, 8.8, 6.0], [8.5, 9.8, 6.5], 'metal'))
    
    m_panty = make_model(e_panty)
    m_panty['display']['head'] = {'rotation': [0, 0, 0], 'translation': [0, -2, 0], 'scale': [0.9, 0.9, 0.9]}
    m_panty['display']['gui'] = {'rotation': [15, -45, 0], 'translation': [0, -1, 0], 'scale': [1.1, 1.1, 1.1]}
    js(AS / f'models/item/{kind}_panties.json', m_panty)

# Модели одежды на основе кожаной брони
costume_bases = {
    'shuka_blouse': 'leather_chestplate',
    'shuka_skirt': 'leather_leggings',
    'shuka_shoes': 'leather_boots',
    'shimokitazawa_vest': 'leather_chestplate',
    'shimokitazawa_skirt': 'leather_leggings',
    'starry_shirt': 'leather_chestplate',
    'maid_dress': 'leather_chestplate',
    'maid_skirt': 'leather_leggings'
}
for item_id, base in costume_bases.items():
    js(AS / f'models/item/{item_id}.json', {'parent': 'minecraft:item/' + base})

# --- 3. Рецепты крафта ---

recipes = {
    'shuka_blouse': ['leather_chestplate', 'blue_wool', 'white_wool', 'string'],
    'shuka_skirt': ['leather_leggings', 'blue_wool', 'blue_dye', 'string'],
    'shuka_shoes': ['leather_boots', 'brown_dye', 'iron_nugget', 'string'],
    'shimokitazawa_vest': ['leather_chestplate', 'light_gray_wool', 'yellow_dye', 'string'],
    'shimokitazawa_skirt': ['leather_leggings', 'gray_wool', 'light_gray_dye', 'string'],
    'starry_shirt': ['leather_chestplate', 'black_wool', 'yellow_dye', 'gold_nugget'],
    'maid_headband': ['white_wool', 'string', 'white_dye', 'iron_nugget'],
    'maid_dress': ['leather_chestplate', 'black_wool', 'white_wool', 'white_dye'],
    'maid_skirt': ['leather_leggings', 'black_wool', 'white_wool', 'string'],
    'bocchi_panties': ['pink_wool', 'pink_wool', 'string', 'blue_dye', 'yellow_dye'],
    'nijika_panties': ['yellow_wool', 'yellow_wool', 'string', 'red_dye', 'white_dye'],
    'ryo_panties': ['blue_wool', 'blue_wool', 'string', 'cyan_dye', 'iron_nugget'],
    'kita_panties': ['red_wool', 'red_wool', 'string', 'glowstone_dust', 'white_dye'],
    'seika_panties': ['black_wool', 'black_wool', 'string', 'gray_dye', 'amethyst_shard'],
    'kikuri_panties': ['purple_wool', 'purple_wool', 'string', 'green_dye', 'glass_bottle'],
    'pa_san_panties': ['gray_wool', 'gray_wool', 'string', 'black_dye', 'iron_nugget'],
    'guitar_pick': ['flint', 'iron_nugget', 'gold_nugget'],
    'kikuri_drink': ['glass_bottle', 'sweet_berries', 'sugar', 'paper'],
    'empty_pack': ['paper', 'paper', 'string', 'green_dye'],
    'shimokitazawa_soda': ['iron_nugget', 'sugar', 'glow_berries', 'glass_bottle'],
    'starry_ticket': ['paper', 'gold_nugget', 'yellow_dye']
}

for item_id, ing_list in recipes.items():
    js(DATA / f'recipe/{item_id}.json', {
        'type': 'minecraft:crafting_shapeless',
        'ingredients': [{'item': 'minecraft:' + ing} for ing in ing_list],
        'result': {'id': 'bocchi:' + item_id, 'count': 1}
    })

# --- 4. Лут в сундуках статуй и бэкстейджа ---

# Сундуки статуй (statue_<kind>.json)
statue_treats = {
    'bocchi': 'cookie',
    'nijika': 'cake',
    'ryo': 'wheat',
    'kita': 'glow_berries',
    'seika': 'sweet_berries',
    'kikuri': 'honey_bottle',
    'pa_san': 'dried_kelp'
}

for kind in panty_colors:
    treat = statue_treats[kind]
    js(DATA / f'loot_table/chests/statue_{kind}.json', {
        'type': 'minecraft:chest',
        'pools': [
            {
                'rolls': 1,
                'entries': [
                    {'type': 'minecraft:item', 'name': f'bocchi:{kind}_panties', 'weight': 1}
                ]
            },
            {
                'rolls': 1,
                'entries': [
                    {'type': 'minecraft:item', 'name': f'bocchi:{kind}_figurine', 'weight': 1}
                ]
            },
            {
                'rolls': {'type': 'minecraft:uniform', 'min': 2, 'max': 5},
                'entries': [
                    {'type': 'minecraft:item', 'name': f'minecraft:{treat}', 'weight': 5,
                     'functions': [{'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': 2, 'max': 8}}]},
                    {'type': 'minecraft:item', 'name': 'bocchi:guitar_pick', 'weight': 3},
                    {'type': 'minecraft:item', 'name': 'bocchi:starry_ticket', 'weight': 3},
                    {'type': 'minecraft:item', 'name': 'bocchi:shimokitazawa_soda', 'weight': 3},
                    {'type': 'minecraft:item', 'name': 'bocchi:kikuri_drink', 'weight': 2}
                ]
            }
        ]
    })

# Обновляем сундук бэкстейджа сцен
js(DATA / 'loot_table/chests/stage_backstage.json', {
    'type': 'minecraft:chest',
    'pools': [
        {
            'rolls': {'type': 'minecraft:uniform', 'min': 4, 'max': 8},
            'entries': [
                {'type': 'minecraft:item', 'name': 'minecraft:cookie', 'weight': 6,
                 'functions': [{'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': 1, 'max': 8}}]},
                {'type': 'minecraft:item', 'name': 'minecraft:wheat', 'weight': 6,
                 'functions': [{'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': 1, 'max': 8}}]},
                {'type': 'minecraft:item', 'name': 'minecraft:glow_berries', 'weight': 6,
                 'functions': [{'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': 1, 'max': 8}}]},
                {'type': 'minecraft:item', 'name': 'minecraft:cake', 'weight': 3,
                 'functions': [{'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': 1, 'max': 1}}]},
                {'type': 'minecraft:item', 'name': 'bocchi:rehearsal_bell', 'weight': 2},
                {'type': 'minecraft:item', 'name': 'bocchi:mango_box_hat', 'weight': 2},
                {'type': 'minecraft:item', 'name': 'bocchi:starry_shirt', 'weight': 3},
                {'type': 'minecraft:item', 'name': 'bocchi:shuka_blouse', 'weight': 2},
                {'type': 'minecraft:item', 'name': 'bocchi:shuka_skirt', 'weight': 2},
                {'type': 'minecraft:item', 'name': 'bocchi:shimokitazawa_vest', 'weight': 2},
                {'type': 'minecraft:item', 'name': 'bocchi:shimokitazawa_skirt', 'weight': 2},
                {'type': 'minecraft:item', 'name': 'bocchi:maid_headband', 'weight': 2},
                {'type': 'minecraft:item', 'name': 'bocchi:maid_dress', 'weight': 2},
                {'type': 'minecraft:item', 'name': 'bocchi:guitar_pick', 'weight': 5},
                {'type': 'minecraft:item', 'name': 'bocchi:starry_ticket', 'weight': 4},
                {'type': 'minecraft:item', 'name': 'bocchi:shimokitazawa_soda', 'weight': 4},
                {'type': 'minecraft:item', 'name': 'bocchi:kikuri_drink', 'weight': 3},
                {'type': 'minecraft:item', 'name': 'bocchi:bocchi_panties', 'weight': 1},
                {'type': 'minecraft:item', 'name': 'bocchi:nijika_panties', 'weight': 1},
                {'type': 'minecraft:item', 'name': 'bocchi:ryo_panties', 'weight': 1},
                {'type': 'minecraft:item', 'name': 'bocchi:kita_panties', 'weight': 1}
            ]
        }
    ]
})

# --- 5. Локализации ---

item_names = {
    'shuka_blouse': ('Матроска старшей школы Сюка', 'Матроска старэйшай школы Сюка', 'Shuka High Sailor Blouse'),
    'shuka_skirt': ('Плиссированная юбка Сюка', 'Плісіраваная спадніца Сюка', 'Shuka High Pleated Skirt'),
    'shuka_shoes': ('Школьные туфли Сюка', 'Школьныя туфлі Сюка', 'Shuka High Loafers'),
    'shimokitazawa_vest': ('Жилет школы Симокитадзава', 'Камізэлька школы Сімакітадзава', 'Shimokitazawa High Vest'),
    'shimokitazawa_skirt': ('Юбка школы Симокитадзава', 'Спадніца школы Сімакітадзава', 'Shimokitazawa High Skirt'),
    'starry_shirt': ('Футболка персонала STARRY', 'Футболка персаналу STARRY', 'STARRY Staff T-Shirt'),
    'maid_headband': ('Ободок горничной', 'Абадок пакаёўкі', 'Maid Ruffle Headband'),
    'maid_dress': ('Платье горничной', 'Сукенка пакаёўкі', 'Maid Cafe Dress'),
    'maid_skirt': ('Юбка горничной', 'Спадніца пакаёўкі', 'Maid Cafe Skirt'),
    'bocchi_panties': ('Трусики Хитори', 'Трусікі Хіторы', 'Hitori’s Panties'),
    'nijika_panties': ('Трусики Нидзики', 'Трусікі Нідзікі', 'Nijika’s Panties'),
    'ryo_panties': ('Трусики Рё', 'Трусікі Ро', 'Ryo’s Panties'),
    'kita_panties': ('Трусики Киты', 'Трусікі Кіты', 'Kita’s Panties'),
    'seika_panties': ('Трусики Сейки', 'Трусікі Сейкі', 'Seika’s Panties'),
    'kikuri_panties': ('Трусики Кикури', 'Трусікі Кікуры', 'Kikuri’s Panties'),
    'pa_san_panties': ('Трусики ПА-сан', 'Трусікі ПА-сан', 'PA-san’s Panties'),
    'guitar_pick': ('Медиатор Kessoku Band', 'Медыятар Kessoku Band', 'Kessoku Band Guitar Pick'),
    'kikuri_drink': ('Напиток Кикури в тетрапаке', 'Напой Кікуры ў тэтрапаку', 'Kikuri’s Juice Box'),
    'empty_pack': ('Смятый тетрапак', 'Змяты тэтрапак', 'Crushed Drink Box'),
    'shimokitazawa_soda': ('Газировка Симокитадзавы', 'Газіроўка Сімакітадзавы', 'Shimokitazawa Soda'),
    'starry_ticket': ('Билет на концерт STARRY', 'Квіток на канцэрт STARRY', 'STARRY Concert Ticket')
}

messages = {
    'message.bocchi.panties.head': (
        'Вы надели трусики %s на голову! Смелый стиль...',
        'Вы надзелі трусікі %s на галаву! Смелы стыль...',
        'You put %s’s panties on your head! A bold look...'
    ),
    'message.bocchi.panties.wear': (
        'Вы надели трусики %s. Чувствуется вдохновение!',
        'Вы надзелі трусікі %s. Адчуваецца натхненне!',
        'You put on %s’s panties. You feel inspired!'
    ),
    'message.bocchi.ticket.inspect': (
        'Памятный билет на выступление Kessoku Band в STARRY!',
        'Памятны квіток на выступленне Kessoku Band у STARRY!',
        'A keepsake ticket from Kessoku Band’s gig at STARRY!'
    ),
    'message.bocchi.panties.react.bocchi': (
        'Хитори паникует: «А-а-ва-ва-ва! О-откуда они у вас?!»',
        'Хіторы панікуе: «А-а-ва-ва-ва! А-адкуль яны ў вас?!»',
        'Hitori panics: "Wh-wha-aa! Wh-where did you get those?!"'
    ),
    'message.bocchi.panties.react.nijika': (
        'Нидзика краснеет: «Эй! Это не официальный мерч, верни!»',
        'Нідзіка чырванее: «Гэй! Гэта не афіцыйны мерч, вярні!»',
        'Nijika blushes: "Hey! That is definitely not official band merch, give them back!"'
    ),
    'message.bocchi.panties.react.ryo': (
        'Рё спокойно смотрит: «Пять тысяч иен — и я сохраню секрет.»',
        'Ро спакойна глядзіць: «Пяць тысяч ен — і я захаваю сакрэт.»',
        'Ryo deadpans: "5,000 yen and I will keep your secret."'
    ),
    'message.bocchi.panties.react.kita': (
        'Кита ослепительно смущается: «Кита-а-а-н! Спрячьте немедленно!»',
        'Кіта асляпляльна бянтэжыцца: «Кіта-а-а-н! Схавайце неадкладна!»',
        'Kita blushes intensely: "Kitaaan! Put those away this instant!"'
    ),
    'message.bocchi.panties.react.seika': (
        'Сейка хмурится: «Ты что вытворяешь в моём лайвхаусе?!»',
        'Сейка хмурыцца: «Што ты робіш у маім лайвхаўсе?!»',
        'Seika scowls: "What do you think you are doing in my livehouse?!"'
    ),
    'message.bocchi.panties.react.kikuri': (
        'Кикури смеётся: «И-ха! Настоящий панк-рок, держи сок!»',
        'Кікуры смяецца: «І-ха! Сапраўдны панк-рок, трымай сок!»',
        'Kikuri cackles: "Yahoo! Pure punk rock, have a drink!"'
    ),
    'message.bocchi.panties.react.pa_san': (
        'ПА-сан невозмутима: «По правилам безопасности клуба это запрещено.»',
        'ПА-сан спакойная: «Па правілах бяспекі клуба гэта забаронена.»',
        'PA-san stays calm: "Livehouse regulations strictly forbid that in the venue."'
    ),
    'message.bocchi.gift.bocchi': (
        'Хитори робко делится сувениром в знак дружбы!',
        'Хіторы нясмела дзеліцца сувенірам у знак сяброўства!',
        'Hitori shyly hands you a token of friendship!'
    ),
    'message.bocchi.gift.nijika': (
        'Нидзика тепло улыбается и дарит подарок!',
        'Нідзіка цёпла ўсміхаецца і дорыць падарунак!',
        'Nijika smiles warmly and presents a gift!'
    ),
    'message.bocchi.gift.ryo': (
        'Рё делится полезной вещью.',
        'Ро дзеліцца карыснай рэччу.',
        'Ryo hands you a useful item.'
    ),
    'message.bocchi.gift.kita': (
        'Кита радостно сияет и вручает презент!',
        'Кіта радасна ззяе і ўручае прэзент!',
        'Kita beams with joy and hands you a present!'
    ),
    'message.bocchi.gift.seika': (
        'Сейка ворчит, но вручает клубный сувенир.',
        'Сейка бурчыць, але ўручае клубны сувенір.',
        'Seika grumbles but passes you a club souvenir.'
    ),
    'message.bocchi.gift.kikuri': (
        'Кикури весело делится с вами рок-лутом!',
        'Кікуры весела дзеліцца з вамі рок-лутам!',
        'Kikuri cheerily hands you some rock loot!'
    ),
    'message.bocchi.gift.pa_san': (
        'ПА-сан передаёт сувенир из аппаратной.',
        'ПА-сан перадае сувенір з апаратнай.',
        'PA-san hands you a souvenir from the sound desk.'
    )
}

for locale, idx in [('ru_ru', 0), ('be_by', 1), ('en_us', 2)]:
    path = AS / f'lang/{locale}.json'
    lang_data = json.loads(path.read_text(encoding='utf-8'))
    for item_id, names in item_names.items():
        lang_data[f'item.bocchi.{item_id}'] = names[idx]
    for msg_key, msgs in messages.items():
        lang_data[msg_key] = msgs[idx]
    path.write_text(json.dumps(lang_data, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

# Обновляем тег креативного контента и приветственное открытие рецептов
recipes_all = sorted((DATA / 'recipe').glob('*.json'))
js(DATA / 'tags/item/creative_content.json', {'values': [json.loads(p.read_text(encoding='utf-8'))['result']['id'] for p in recipes_all]})
js(DATA / 'advancement/recipes/welcome.json', {'criteria': {'join': {'trigger': 'minecraft:tick'}}, 'rewards': {'recipes': ['bocchi:' + p.stem for p in recipes_all]}})

print('Generated costumes, panties and loot assets successfully.')
