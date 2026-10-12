"""Generate a high-resolution visual preview board of all 7 heroine panties sprites."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
AS = ROOT / 'src/main/resources/assets/bocchi'
OUT = ROOT / 'docs/btr-panties-preview.png'

font_path = '/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'
bold_path = '/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf'

def get_font(size, bold=False):
    p = bold_path if bold else font_path
    return ImageFont.truetype(p, size)

W, H = 2140, 520
canvas = Image.new('RGB', (W, H), '#f8f4ee')
d = ImageDraw.Draw(canvas)

# Title and header
d.text((40, 24), 'BTR WORLD · Уникальные 2D-спрайты трусиков героинь', font=get_font(28, True), fill='#342a34')
d.text((40, 64), 'Пиксель-арт 16 × 16 с боковыми завязками, кружевом и личными символами героинь · Маска на голову (ПКМ) / экипировка ног (Shift+ПКМ)', font=get_font(16), fill='#766e80')

cards_data = [
    ('bocchi', 'Хитори', 'Hitori Gotoh', '#f59bc2', 'Кубики Хитори + полоски', 'Скорость II + Невидимость'),
    ('nijika', 'Нидзика', 'Nijika Ijichi', '#f5ce42', 'Красный бант + жёлтый шёлк', 'Спешка II + Регенерация'),
    ('ryo', 'Рё', 'Ryo Yamada', '#436899', 'Морские полоски + крестик', 'Удача III + Герой деревни'),
    ('kita', 'Кита', 'Ikuyo Kita', '#e03a4b', 'Алый цвет + звезда Кита-Ауры', 'Свечение + Ночное зрение'),
    ('seika', 'Сейка', 'Seika Ijichi', '#26232b', 'Чёрный шёлк + золото', 'Стойкость II'),
    ('kikuri', 'Кикури', 'Kikuri Hiroi', '#8c286b', 'Панк-рок пурпур + изумруд', 'Сила II + Рок-кураж'),
    ('pa_san', 'ПА-сан', 'PA-san', '#302e3b', 'Готический пепел + кольцо', 'Замедление врагов + Перо')
]

for idx, (kind, ru_name, en_name, theme_col, feature, buff) in enumerate(cards_data):
    x = 40 + idx * 296
    y = 110
    w = 280
    h = 380

    # Card background
    d.rounded_rectangle((x, y, x + w, y + h), radius=16, fill='#ffffff', outline='#e8e2dc', width=2)
    # Color badge bar
    d.rounded_rectangle((x + 14, y + 14, x + 24, y + 42), radius=4, fill=theme_col)
    # Name
    d.text((x + 32, y + 12), ru_name, font=get_font(20, True), fill='#2b2633')
    d.text((x + 32, y + 36), en_name, font=get_font(13), fill='#8a8294')

    # Load and scale up sprite (nearest neighbor 8x)
    sprite_path = AS / f'textures/item/{kind}_panties.png'
    sprite = Image.open(sprite_path).convert('RGBA')
    scaled = sprite.resize((128, 128), Image.Resampling.NEAREST)

    # Soft preview pedestal
    d.ellipse((x + 60, y + 195, x + 220, y + 225), fill='#f5eff4')
    canvas.paste(scaled, (x + 76, y + 75), scaled)

    # Feature and buff labels
    d.rounded_rectangle((x + 16, y + 245, x + w - 16, y + 295), radius=8, fill='#faf6f9')
    d.text((x + 24, y + 252), 'Особенность:', font=get_font(11, True), fill='#948c9c')
    d.text((x + 24, y + 269), feature, font=get_font(12, True), fill='#373142')

    d.rounded_rectangle((x + 16, y + 308, x + w - 16, y + 358), radius=8, fill='#f4fbf7')
    d.text((x + 24, y + 315), 'Эффект при надевании:', font=get_font(11, True), fill='#5b9173')
    d.text((x + 24, y + 332), buff, font=get_font(12, True), fill='#22593b')

OUT.parent.mkdir(parents=True, exist_ok=True)
canvas.save(OUT)
print(f'Saved preview to {OUT}')
