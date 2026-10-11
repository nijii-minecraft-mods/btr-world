# Executed by generate_assets.py with its model helpers.
from PIL import Image, ImageDraw

# Текстура коробки манго
im = Image.new('RGB', (64, 64), '#ba8861')
d = ImageDraw.Draw(im)
for y in range(0, 64, 4):
    d.line((0, y, 63, y), fill='#ac7c54')
d.rectangle((0, 0, 63, 63), outline='#714d33', width=2)
d.rectangle((25, 0, 37, 63), fill='#d7bc8a')
d.rectangle((5, 22, 58, 43), fill='#e6d1a8')
d.text((12, 28), 'MANGO', fill='#54693b')
d.ellipse((42, 7, 54, 20), fill='#efa735')
d.line((48, 7, 52, 3), fill='#537740', width=2)
im.save(AS / 'textures/block/model/mango.png')
textures['mango'] = 'bocchi:block/model/mango'

# 1. Коробка из-под манго на голову
e = [
    element([0, 0, 0], [16, 16, 16], 'cardboard'),
    element([-.15, 0, -.15], [16.15, 16, 0], 'mango'),
    element([0, 15.8, 0], [16, 16.2, 3], 'carddark'),
    element([0, 15.8, 13], [16, 16.2, 16], 'carddark')
]
o = model(e)
o['display']['head'] = {'rotation': [0, 180, 0], 'translation': [0, 0, 0], 'scale': [1, 1, 1]}
js(AS / 'models/item/mango_box_hat.json', o)

# 2. Дори-ахоге Нидзики
e = [
    element([7, 15, 7], [9, 19, 9], 'yellow'),
    element([8, 18, 7], [12, 20, 9], 'yellow', rotation={'origin': [8, 18, 8], 'axis': 'z', 'angle': 22.5}),
    element([10, 16, 7], [12, 19, 9], 'yellow')
]
o = model(e)
o['display']['head'] = {'rotation': [0, 0, 0], 'translation': [0, 0, 0], 'scale': [1, 1, 1]}
js(AS / 'models/item/nijika_ahoge.json', o)

# 3. Заколка-кубики Хитори (синий и жёлтый кубики сбоку головы)
e = [
    element([14.5, 11.5, 3.5], [17.0, 14.0, 6.0], 'blue'),
    element([14.5, 8.8, 3.5], [17.0, 11.3, 6.0], 'yellow'),
    element([14.2, 8.5, 4.4], [14.8, 14.3, 5.1], 'outline')
]
o = model(e)
o['display']['head'] = {'rotation': [0, 0, 0], 'translation': [0, 0, 0], 'scale': [1, 1, 1]}
o['display']['gui'] = {'rotation': [15, -45, 0], 'translation': [-7, -3, 0], 'scale': [1.2, 1.2, 1.2]}
js(AS / 'models/item/bocchi_hairclip.json', o)

# 4. Большой красный бантик Нидзики
e = [
    element([14.8, 12.0, 6.5], [16.6, 13.8, 8.5], 'red'),
    element([14.7, 12.8, 4.2], [16.4, 15.2, 6.8], 'red', rotation={'origin': [15.5, 13.0, 6.5], 'axis': 'z', 'angle': 22.5}),
    element([14.7, 12.8, 8.2], [16.4, 15.2, 10.8], 'red', rotation={'origin': [15.5, 13.0, 8.5], 'axis': 'z', 'angle': -22.5}),
    element([15.0, 9.2, 5.8], [16.3, 12.2, 7.2], 'red'),
    element([15.0, 8.4, 7.8], [16.3, 12.0, 9.2], 'red')
]
o = model(e)
o['display']['head'] = {'rotation': [0, 0, 0], 'translation': [0, 0, 0], 'scale': [1, 1, 1]}
o['display']['gui'] = {'rotation': [15, -45, 0], 'translation': [-7, -3, 0], 'scale': [1.0, 1.0, 1.0]}
js(AS / 'models/item/nijika_ribbon.json', o)

# 5. Стильная заколка Рё (крестик на чёлке)
e = [
    element([14.5, 11.2, 2.8], [15.8, 14.8, 3.4], 'white', rotation={'origin': [15.1, 13.0, 3.1], 'axis': 'x', 'angle': 22.5}),
    element([14.5, 11.2, 2.8], [15.8, 14.8, 3.4], 'white', rotation={'origin': [15.1, 13.0, 3.1], 'axis': 'x', 'angle': -22.5}),
    element([14.8, 12.4, 2.7], [15.6, 13.6, 3.5], 'metal')
]
o = model(e)
o['display']['head'] = {'rotation': [0, 0, 0], 'translation': [0, 0, 0], 'scale': [1, 1, 1]}
o['display']['gui'] = {'rotation': [15, -45, 0], 'translation': [-7, -3, 0], 'scale': [1.3, 1.3, 1.3]}
js(AS / 'models/item/ryo_hairclip.json', o)

# 6. Сверкающая заколка-звёздочка Киты
e = [
    element([14.6, 11.0, 3.1], [15.8, 15.0, 3.9], 'yellow'),
    element([14.6, 12.4, 1.7], [15.8, 13.6, 5.3], 'yellow'),
    element([14.8, 12.3, 2.8], [16.0, 13.7, 4.2], 'white'),
    element([14.7, 12.0, 2.4], [15.7, 14.0, 4.6], 'yellow', rotation={'origin': [15.2, 13.0, 3.5], 'axis': 'x', 'angle': 45.0})
]
o = model(e)
o['display']['head'] = {'rotation': [0, 0, 0], 'translation': [0, 0, 0], 'scale': [1, 1, 1]}
o['display']['gui'] = {'rotation': [15, -45, 0], 'translation': [-7, -3, 0], 'scale': [1.3, 1.3, 1.3]}
js(AS / 'models/item/kita_hairclip.json', o)

# 7. Шапка «Цутиноко-Боччи» (розовая плюшевая змейка на голове)
e = [
    element([4.0, 15.5, 3.0], [12.0, 19.5, 13.0], 'pink'),
    element([4.5, 15.5, 1.5], [11.5, 19.0, 3.5], 'pink'),
    element([6.0, 15.5, 13.0], [10.0, 18.0, 15.5], 'pink'),
    element([5.0, 15.2, 3.0], [11.0, 16.0, 12.0], 'bocchi_light'),
    element([11.2, 17.5, 2.2], [12.8, 19.1, 3.8], 'blue'),
    element([11.2, 16.0, 2.2], [12.8, 17.5, 3.8], 'yellow'),
    element([5.5, 17.0, 1.3], [7.0, 18.0, 1.6], 'black'),
    element([9.0, 17.0, 1.3], [10.5, 18.0, 1.6], 'black'),
    element([7.5, 16.2, 1.3], [8.5, 16.8, 1.6], 'black'),
    element([4.8, 16.1, 1.35], [6.0, 16.9, 1.55], 'red'),
    element([10.0, 16.1, 1.35], [11.2, 16.9, 1.55], 'red')
]
o = model(e)
o['display']['head'] = {'rotation': [0, 0, 0], 'translation': [0, 0, 0], 'scale': [1, 1, 1]}
o['display']['gui'] = {'rotation': [25, -135, 0], 'translation': [0, -3, 0], 'scale': [0.75, 0.75, 0.75]}
js(AS / 'models/item/tsuchinoko_hat.json', o)

# Дорожный знак STOP
im = Image.new('RGB', (64, 64), '#bc3545')
d = ImageDraw.Draw(im)
d.line([(17, 2), (46, 2), (61, 17), (61, 46), (46, 61), (17, 61), (2, 46), (2, 17), (17, 2)], fill='#fff5e7', width=3)
glyphs = {
    'S': ['11111', '10000', '10000', '11111', '00001', '00001', '11111'],
    'T': ['11111', '00100', '00100', '00100', '00100', '00100', '00100'],
    'O': ['01110', '10001', '10001', '10001', '10001', '10001', '01110'],
    'P': ['11110', '10001', '10001', '11110', '10000', '10000', '10000']
}
for i, c in enumerate('STOP'):
    for y, row in enumerate(glyphs[c]):
        for x, v in enumerate(row):
            if v == '1':
                d.rectangle((9 + i * 12 + x * 2, 25 + y * 2, 10 + i * 12 + x * 2, 26 + y * 2), fill='#fff5e7')
im.save(AS / 'textures/block/model/stop.png')
textures['stop'] = 'bocchi:block/model/stop'

e = [element([7.3, -8, 7.3], [8.7, 14, 8.7], 'metal')]
for y0, y1, x0, x1 in [(10, 12, 4, 12), (12, 14, 2, 14), (14, 22, 0, 16), (22, 24, 2, 14), (24, 26, 4, 12)]:
    part = element([x0, y0, 7], [x1, y1, 9], 'red')
    part['faces']['north'] = {'texture': '#stop', 'uv': [16 - x1, 26 - y1, 16 - x0, 26 - y0]}
    part['faces']['south'] = {'texture': '#stop', 'uv': [x0, 26 - y1, x1, 26 - y0]}
    e.append(part)
o = model(e)
o['display']['thirdperson_righthand'] = {'rotation': [0, 0, 0], 'translation': [0, 2, 0], 'scale': [0.9, 0.9, 0.9]}
js(AS / 'models/item/stop_sign.json', o)

labels = {
    'mango_box_hat': ('Коробка из-под манго', 'Mango Box Hat'),
    'nijika_ahoge': ('Дори-ахоге Нидзики', 'Nijika’s Dorito Ahoge'),
    'bocchi_hairclip': ('Заколка-кубики Хитори', 'Hitori’s Cube Hairclips'),
    'nijika_ribbon': ('Бантик Нидзики', 'Nijika’s Ribbon'),
    'ryo_hairclip': ('Заколка Рё', 'Ryo’s Hairclip'),
    'kita_hairclip': ('Заколка-звёздочка Киты', 'Kita’s Star Hairpin'),
    'tsuchinoko_hat': ('Шапка «Цутиноко-Боччи»', 'Tsuchinoko Bocchi Hat'),
    'stop_sign': ('Дорожный знак STOP', 'STOP Sign'),
    'pink_hood': ('Розовый спортивный капюшон', 'Pink Tracksuit Hood'),
    'pink_jacket': ('Розовая спортивная куртка', 'Pink Tracksuit Jacket'),
    'pink_pants': ('Розовые спортивные брюки', 'Pink Tracksuit Pants'),
    'pink_shoes': ('Розовые кроссовки', 'Pink Trainers')
}
for item_id, (ru_text, en_text) in labels.items():
    lang('item.bocchi.' + item_id, ru_text, en_text)

for item_id, base in [
    ('pink_hood', 'leather_helmet'),
    ('pink_jacket', 'leather_chestplate'),
    ('pink_pants', 'leather_leggings'),
    ('pink_shoes', 'leather_boots')
]:
    js(AS / f'models/item/{item_id}.json', {'parent': 'minecraft:item/' + base})
    js(DATA / f'recipe/{item_id}.json', {
        'type': 'minecraft:crafting_shapeless',
        'ingredients': [{'item': 'minecraft:' + base}, {'item': 'minecraft:pink_wool'}, {'item': 'minecraft:string'}],
        'result': {'id': 'bocchi:' + item_id, 'count': 1}
    })

recipes = [
    ('mango_box_hat', ['PPP', 'PAP', 'PPP'], {'P': 'paper', 'A': 'golden_apple'}),
    ('nijika_ahoge', [' NN', ' N ', 'S  '], {'N': 'gold_nugget', 'S': 'string'}),
    ('bocchi_hairclip', [' L ', ' Y ', ' S '], {'L': 'blue_dye', 'Y': 'yellow_dye', 'S': 'iron_nugget'}),
    ('nijika_ribbon', ['R R', ' R ', 'S S'], {'R': 'red_dye', 'S': 'string'}),
    ('ryo_hairclip', ['I I', ' W ', 'I I'], {'I': 'iron_nugget', 'W': 'white_dye'}),
    ('kita_hairclip', [' G ', 'GDG', ' S '], {'G': 'gold_nugget', 'D': 'glowstone_dust', 'S': 'iron_nugget'}),
    ('tsuchinoko_hat', [' B ', 'PPP', ' Y '], {'P': 'pink_wool', 'B': 'blue_dye', 'Y': 'yellow_dye'}),
    ('stop_sign', ['IRI', 'IRI', ' S '], {'I': 'iron_ingot', 'R': 'red_dye', 'S': 'stick'})
]

for item_id, pattern, key in recipes:
    js(DATA / f'recipe/{item_id}.json', {
        'type': 'minecraft:crafting_shaped',
        'pattern': pattern,
        'key': {k: {'item': 'minecraft:' + v} for k, v in key.items()},
        'result': {'id': 'bocchi:' + item_id, 'count': 1}
    })

from standardize_names import standardize
standardize()
