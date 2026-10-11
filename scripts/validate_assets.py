"""Checks references and 1.21.1 JSON constraints in the authored resource/data pack."""

import json
from pathlib import Path
import sys
import unittest

ROOT = Path(__file__).resolve().parents[1]
RESOURCES = ROOT / 'src/main/resources'
ASSETS = RESOURCES / 'assets/bocchi'
DATA = RESOURCES / 'data/bocchi'

count = 0
for p in RESOURCES.rglob('*.json'):
    obj = json.loads(p.read_text(encoding='utf-8'))
    count += 1

    if '/models/' in str(p):
        parent = obj.get('parent', '')
        if parent.startswith('bocchi:'):
            assert (ASSETS / 'models' / (parent.split(':')[1] + '.json')).exists(), p

        for k, tex in obj.get('textures', {}).items():
            if tex.startswith('bocchi:'):
                sprite = tex.split(':', 1)[1]
                assert (ASSETS / 'textures' / (sprite + '.png')).exists(), (p, k)
                # Minecraft 1.21.1 blocks atlas scans block/ and item/ directories.
                # A PNG outside those folders can exist but still render as missing.
                assert sprite.startswith(('block/', 'item/')), (p, k, 'texture outside block/item atlas', sprite)

        for e in obj.get('elements', []):
            assert all(-16 <= v <= 32 for v in e['from'] + e['to']), p
            assert all(a < b for a, b in zip(e['from'], e['to'])), p
            if 'rotation' in e:
                assert e['rotation']['angle'] in [-45, -22.5, 0, 22.5, 45], p
            for face in e['faces'].values():
                assert face['texture'][1:] in obj['textures'], p

    if '/recipe/' in str(p):
        assert 'id' in obj['result'] and 'item' not in obj['result'], p
        assert obj['result']['id'].startswith('bocchi:'), p

for p in (ASSETS / 'blockstates').glob('*.json'):
    assert (DATA / 'loot_table/blocks' / p.name).exists(), p
    assert (ASSETS / 'models/item' / p.name).exists(), p
    assert (DATA / 'recipe' / p.name).exists(), p

# Different results must not compete for an identical crafting layout.
seen = {}
for p in (DATA / 'recipe').glob('*.json'):
    obj = json.loads(p.read_text(encoding='utf-8'))
    if obj['type'] == 'minecraft:crafting_shaped':
        rows = [
            [json.dumps(obj['key'][c], sort_keys=True) if c != ' ' else '' for c in row]
            for row in obj['pattern']
        ]
        while rows and not any(rows[0]):
            rows.pop(0)
        while rows and not any(rows[-1]):
            rows.pop()
        while rows and all(not row[0] for row in rows):
            rows = [row[1:] for row in rows]
        while rows and all(not row[-1] for row in rows):
            rows = [row[:-1] for row in rows]
        normal = tuple(tuple(row) for row in rows)
        mirror = tuple(tuple(reversed(row)) for row in rows)
        signature = ('shaped', min(normal, mirror))
    else:
        signature = ('shapeless', tuple(sorted(json.dumps(i, sort_keys=True) for i in obj['ingredients'])))

    assert signature not in seen, ('conflicting crafting recipes', p, seen.get(signature))
    seen[signature] = p

ru = json.loads((ASSETS / 'lang/ru_ru.json').read_text(encoding='utf-8'))
en = json.loads((ASSETS / 'lang/en_us.json').read_text(encoding='utf-8'))
be = json.loads((ASSETS / 'lang/be_by.json').read_text(encoding='utf-8'))
assert ru.keys() == en.keys()
assert be.keys() == ru.keys()

assert len(list((ASSETS / 'blockstates').glob('*.json'))) == 85
assert len(list((DATA / 'recipe').glob('*.json'))) == 134
print(f'PASS: {count} JSON files; texture/model references and atlas coverage, geometry, 85 block drops and 134 recipes.')

from structure_nbt import read

for path in (DATA / 'structure/statues').glob('*.nbt'):
    nbt = read(path)
    assert nbt['size'] == [11, 18, 11], path
    positions = set()
    for block in nbt['blocks']:
        pos = tuple(block['pos'])
        assert pos not in positions
        positions.add(pos)
        assert all(0 <= v < limit for v, limit in zip(pos, nbt['size'])), path
        assert 0 <= block['state'] < len(nbt['palette']), path
    assert len(positions) > 500, path

achievements = list((DATA / 'advancement').glob('*.json'))
assert len(achievements) == 10
for path in achievements:
    obj = json.loads(path.read_text(encoding='utf-8'))
    assert obj['display']['show_toast'] is True
    for component in ('title', 'description'):
        key = obj['display'][component]['translate']
        assert key in ru and key in en and key in be

print('PASS: 7 statue NBT templates, 10 achievements, RU/BE/EN and normal toasts.')

sys.path.insert(0, str(ROOT / 'tests'))
from test_stages import StageResources

result = unittest.TextTestRunner(verbosity=1).run(
    unittest.defaultTestLoader.loadTestsFromTestCase(StageResources)
)
assert result.wasSuccessful(), 'Stage validation failed'
