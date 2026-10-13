"""Generate textures, 2D item sprites, 3D models, recipes, loot tables and localizations for costumes, panties and club loot."""
from pathlib import Path
from PIL import Image, ImageDraw
import json

ROOT = Path(__file__).resolve().parents[1] / 'src/main/resources'
AS = ROOT / 'assets/bocchi'
DATA = ROOT / 'data/bocchi'

for d in ['textures/block/model', 'textures/item', 'models/item', 'lang']:
    (AS / d).mkdir(parents=True, exist_ok=True)
for d in ['recipe', 'loot_table/chests']:
    (DATA / d).mkdir(parents=True, exist_ok=True)

def js(path, obj):
    path.write_text(json.dumps(obj, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

def hex_to_rgba(hex_str, alpha=255):
    hex_str = hex_str.lstrip('#')
    r, g, b = tuple(int(hex_str[i:i+2], 16) for i in (0, 2, 4))
    return (r, g, b, alpha)

def element(a, b, tex, north=None, rotation=None):
    faces = {s: {'uv': [0, 0, 16, 16], 'texture': '#' + tex} for s in ['north', 'south', 'east', 'west', 'up', 'down']}
    if north:
        faces['north']['texture'] = '#' + north
    res = {'from': a, 'to': b, 'faces': faces}
    if rotation:
        res['rotation'] = rotation
    return res

# --- 1. Текстуры для 3D моделей блоков/предметов ---

# Тетрапак они-напитка Кикури (32x32)
im = Image.new('RGB', (32, 32), '#387346')
d = ImageDraw.Draw(im)
d.rectangle((2, 2, 29, 29), outline='#eedb9d', width=1)
d.rectangle((5, 5, 26, 12), fill='#8a2638')
d.text((7, 5), '鬼殺', fill='#fff5e7')
d.rectangle((5, 15, 26, 26), fill='#f5ebd5')
d.text((7, 16), 'SICK', fill='#2d5a37')
im.save(AS / 'textures/block/model/kikuri_pack.png')

# Банка газировки Симокитадзавы (16x16)
im = Image.new('RGB', (16, 16), '#53a5c7')
d = ImageDraw.Draw(im)
d.rectangle((0, 0, 15, 2), fill='#b8c5cc')
d.rectangle((0, 13, 15, 15), fill='#b8c5cc')
d.line([(3, 5), (12, 10)], fill='#fff5e7', width=1)
d.point([(5, 4), (10, 6), (7, 9), (11, 11)], fill='#cbf0ff')
im.save(AS / 'textures/block/model/soda.png')

# Кружева ободка горничной
im = Image.new('RGB', (16, 16), '#fbfbfb')
d = ImageDraw.Draw(im)
for x in range(0, 16, 4):
    d.arc((x, 0, x + 3, 6), 0, 180, fill='#27222c', width=1)
    d.rectangle((x + 1, 7, x + 2, 15), fill='#e3dbe8')
im.save(AS / 'textures/block/model/maid_lace.png')

# --- 2. Пиксельные 2D-спрайты трусиков (16x16 RGBA) ---

panty_specs = {
    'bocchi': {
        'outline': '#7a314b',
        'base': '#f59bc2',
        'stripe': '#ffffff',
        'lace': '#fde2ee',
        'ribbon': '#74cce8', # blue
        'accent2': '#f1d35e' # yellow
    },
    'nijika': {
        'outline': '#7a5a1a',
        'base': '#f5ce42',
        'stripe': '#ffffff',
        'lace': '#fff3c4',
        'ribbon': '#e83a48', # red bow
        'accent2': '#e83a48'
    },
    'ryo': {
        'outline': '#1c3150',
        'base': '#436899',
        'stripe': '#e3efff',
        'lace': '#b0cfee',
        'ribbon': '#ffffff', # white cross
        'accent2': '#ffffff'
    },
    'kita': {
        'outline': '#6e1823',
        'base': '#e03a4b',
        'stripe': '#ffffff',
        'lace': '#ffd9df',
        'ribbon': '#fcd33f', # star
        'accent2': '#fcd33f'
    },
    'seika': {
        'outline': '#141217',
        'base': '#26232b',
        'stripe': '#3d3845',
        'lace': '#474252',
        'ribbon': '#e3b840', # gold
        'accent2': '#e3b840'
    },
    'kikuri': {
        'outline': '#3d0f2f',
        'base': '#8c286b',
        'stripe': '#38a58e', # emerald
        'lace': '#b54e95',
        'ribbon': '#38a58e',
        'accent2': '#fcd33f'
    },
    'pa_san': {
        'outline': '#16151c',
        'base': '#302e3b',
        'stripe': '#484557',
        'lace': '#555166',
        'ribbon': '#9cb2bd', # silver
        'accent2': '#9cb2bd'
    }
}

for key, s in panty_specs.items():
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    pix = im.load()
    out = hex_to_rgba(s['outline'])
    base = hex_to_rgba(s['base'])
    stripe = hex_to_rgba(s['stripe'])
    lace = hex_to_rgba(s['lace'])
    rib = hex_to_rgba(s['ribbon'])
    acc = hex_to_rgba(s['accent2'])

    # Боковые завязки / бантики на бёдрах
    for pt in [(1, 3), (1, 5), (14, 3), (14, 5)]:
        pix[pt] = rib
    pix[2, 4] = rib; pix[13, 4] = rib

    # Кружевная кайма по верху пояса (y=3)
    for x in range(3, 13, 2):
        pix[x, 3] = lace

    # Пояс (y=4)
    for x in range(3, 13):
        pix[x, 4] = lace

    # Ткань с полосками
    for x in range(3, 13):
        pix[x, 5] = stripe
    for x in range(4, 12):
        pix[x, 6] = base
    for x in range(4, 12):
        pix[x, 7] = stripe
    for x in range(5, 11):
        pix[x, 8] = base
    for x in range(5, 11):
        pix[x, 9] = stripe
    for x in range(6, 10):
        pix[x, 10] = base
    for x in range(6, 10):
        pix[x, 11] = lace

    # Внешние контуры
    for x in range(3, 13):
        if pix[x, 3] == (0, 0, 0, 0):
            pix[x, 3] = out
    for pt in [(2, 3), (2, 5), (13, 3), (13, 5)]:
        if pix[pt] == (0, 0, 0, 0): pix[pt] = out
    for pt in [(3, 6), (12, 6), (4, 7), (11, 7), (4, 8), (11, 8), (5, 9), (10, 9), (5, 10), (10, 10), (5, 11), (10, 11)]:
        pix[pt] = out
    for x in range(6, 10):
        pix[x, 12] = out

    # Фирменные украшения героинь
    if key == 'bocchi':
        pix[3, 3] = rib # синий кубик
        pix[3, 4] = acc # жёлтый кубик
    elif key == 'nijika':
        pix[7, 4] = rib; pix[8, 4] = rib
        pix[7, 3] = rib; pix[8, 3] = rib
    elif key == 'ryo':
        pix[7, 6] = rib; pix[8, 6] = rib
        pix[7, 7] = rib; pix[8, 7] = rib
    elif key == 'kita':
        pix[7, 6] = rib; pix[8, 6] = rib
        pix[7, 7] = hex_to_rgba('#fff5cc'); pix[8, 7] = hex_to_rgba('#fff5cc')
    elif key == 'seika':
        pix[7, 4] = rib; pix[8, 4] = rib
    elif key == 'kikuri':
        pix[6, 6] = rib; pix[9, 6] = rib
    elif key == 'pa_san':
        pix[7, 4] = rib; pix[8, 4] = rib
        pix[7, 5] = rib

    im.save(AS / f'textures/item/{key}_panties.png')

# 2D спрайт гитарного медиатора (16x16)
im_pick = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
pix = im_pick.load()
pick_out = hex_to_rgba('#8c1d30')
pick_base = hex_to_rgba('#e63956')
pick_star = hex_to_rgba('#ffe28a')
for y in range(3, 13):
    span = (13 - y) // 2 + 1
    for x in range(8 - span, 8 + span):
        pix[x, y] = pick_base
pick_outlines = [
    (5, 2), (6, 2), (7, 2), (8, 2), (9, 2), (10, 2),
    (4, 3), (11, 3), (3, 4), (12, 4), (4, 5), (11, 5),
    (4, 6), (11, 6), (5, 7), (10, 7), (5, 8), (10, 8),
    (6, 9), (9, 9), (6, 10), (9, 10), (7, 11), (8, 11),
    (7, 12), (8, 12), (7, 13), (8, 13)
]
for x, y in pick_outlines:
    if 0 <= x < 16 and 0 <= y < 16:
        pix[x, y] = pick_out
for x, y in [(7, 5), (8, 5), (6, 6), (7, 6), (8, 6), (9, 6), (7, 7), (8, 7)]:
    pix[x, y] = pick_star
im_pick.save(AS / 'textures/item/guitar_pick.png')

# 2D спрайт билета STARRY (16x16)
im_tick = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
pix = im_tick.load()
tick_out = hex_to_rgba('#8a671e')
tick_base = hex_to_rgba('#ffd275')
tick_star = hex_to_rgba('#ffffff')
tick_perf = hex_to_rgba('#bfa14b')
for y in range(4, 12):
    for x in range(1, 15):
        if x in (1, 14) or y in (4, 11):
            pix[x, y] = tick_out
        elif x == 11:
            pix[x, y] = tick_perf if y % 2 == 0 else (0, 0, 0, 0)
        else:
            pix[x, y] = tick_base
for x, y in [(4, 7), (5, 7), (6, 7), (5, 6), (5, 8)]:
    pix[x, y] = tick_star
im_tick.save(AS / 'textures/item/starry_ticket.png')

# --- 3. Модели предметов (JSON) ---

# 2D предметы через minecraft:item/generated
for name in panty_specs:
    js(AS / f'models/item/{name}_panties.json', {
        'parent': 'minecraft:item/generated',
        'textures': {'layer0': f'bocchi:item/{name}_panties'}
    })

js(AS / 'models/item/guitar_pick.json', {
    'parent': 'minecraft:item/generated',
    'textures': {'layer0': 'bocchi:item/guitar_pick'}
})

js(AS / 'models/item/starry_ticket.json', {
    'parent': 'minecraft:item/generated',
    'textures': {'layer0': 'bocchi:item/starry_ticket'}
})

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

tex_3d = {
    'kikuri_pack': 'bocchi:block/model/kikuri_pack',
    'soda': 'bocchi:block/model/soda',
    'maid_lace': 'bocchi:block/model/maid_lace',
    'white': 'bocchi:block/model/white',
    'black': 'bocchi:block/model/black',
    'metal': 'bocchi:block/model/metal',
    'cardboard': 'bocchi:block/model/cardboard'
}

def make_model(elements, display=None):
    obj = {'textures': tex_3d, 'elements': elements}
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
    element([9.0, 12.0, 7.5], [10.0, 15.5, 8.5], 'white')
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

for old_file in (AS / 'textures/block/model').glob('panties_*.png'):
    old_file.unlink(missing_ok=True)
(AS / 'textures/block/model/pick.png').unlink(missing_ok=True)
(AS / 'textures/block/model/starry_ticket.png').unlink(missing_ok=True)

# --- 4. Рецепты крафта ---

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

# --- 5. Лут в сундуках статуй и бэкстейджа ---

statue_treats = {
    'bocchi': 'cookie',
    'nijika': 'cake',
    'ryo': 'wheat',
    'kita': 'glow_berries',
    'seika': 'sweet_berries',
    'kikuri': 'honey_bottle',
    'pa_san': 'dried_kelp'
}

for kind in panty_specs:
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

# --- 6. Локализации ---

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

recipes_all = sorted((DATA / 'recipe').glob('*.json'))
js(DATA / 'tags/item/creative_content.json', {'values': [json.loads(p.read_text(encoding='utf-8'))['result']['id'] for p in recipes_all]})
js(DATA / 'advancement/recipes/welcome.json', {'criteria': {'join': {'trigger': 'minecraft:tick'}}, 'rewards': {'recipes': ['bocchi:' + p.stem for p in recipes_all]}})

print('Generated costumes, panties and loot assets with 2D item sprites successfully.')
