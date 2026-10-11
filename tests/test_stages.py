"""Validate playable positions and complete structure resources, not just NBT syntax."""

import json
import math
from pathlib import Path
import sys
import unittest

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'scripts'))

from structure_nbt import read

DATA = ROOT / 'src/main/resources/data/bocchi'


class StageResources(unittest.TestCase):
    def test_population_and_equipment(self):
        paths = list((DATA / 'structure/stages').glob('*.nbt'))
        self.assertEqual(len(paths), 9)

        for path in paths:
            with self.subTest(template=path.stem):
                nbt = read(path)
                palette = nbt['palette']
                blocks = {tuple(b['pos']): palette[b['state']]['Name'] for b in nbt['blocks']}

                self.assertEqual(len(blocks), math.prod(nbt['size']))
                for pos in blocks:
                    self.assertTrue(all(0 <= v < limit for v, limit in zip(pos, nbt['size'])))

                self.assertEqual(len(nbt['entities']), 4)
                self.assertEqual(
                    {e['nbt']['id'].split(':')[1].split('_')[0] for e in nbt['entities']},
                    {'bocchi', 'nijika', 'ryo', 'kita'}
                )

                for e in nbt['entities']:
                    self.assertEqual(e['nbt']['BtrStagePerformer'], 1)
                    self.assertNotIn('CustomName', e['nbt'])
                    self.assertNotIn('UUID', e['nbt'])
                    self.assertEqual(e['nbt']['PersistenceRequired'], 1)
                    pos = tuple(e['blockPos'])
                    self.assertEqual(blocks[pos], 'minecraft:air')
                    self.assertNotEqual(blocks[(pos[0], pos[1] - 1, pos[2])], 'minecraft:air')

                positions = [e['pos'] for e in nbt['entities']]
                for i, a in enumerate(positions):
                    for b in positions[i + 1:]:
                        self.assertGreater(math.dist(a, b), 0.65)

                self.assertEqual(list(blocks.values()).count('bocchi:guitar_stand'), 2)
                for name in ['drum_kit', 'microphone', 'amplifier', 'speaker', 'guitar_case', 'bocchi_box', 'jimihen_plush', 'mini_slime_plush', 'fairy_lights']:
                    self.assertIn('bocchi:' + name, blocks.values())

                for actor in nbt['entities']:
                    kind = actor['nbt']['id'].split(':')[1].split('_')[0]
                    target = {'nijika': 'drum_kit', 'kita': 'microphone'}.get(kind, 'guitar_stand')
                    nearest = min(math.dist(actor['pos'], (x + 0.5, y, z + 0.5)) for (x, y, z), name in blocks.items() if name == 'bocchi:' + target)
                    self.assertLessEqual(nearest, 1.5)

                # Each wall/ceiling light/poster has a real supporting block.
                offsets = {'north': (0, 0, 1), 'south': (0, 0, -1), 'east': (-1, 0, 0), 'west': (1, 0, 0), 'down': (0, 1, 0)}
                for b in nbt['blocks']:
                    state = palette[b['state']]
                    if state['Name'].startswith('bocchi:') and 'facing' in state.get('Properties', {}):
                        pos = b['pos']
                        delta = offsets[state['Properties']['facing']]
                        support = tuple(a + c for a, c in zip(pos, delta))
                        self.assertNotEqual(blocks[support], 'minecraft:air')

                chest = next(b for b in nbt['blocks'] if 'nbt' in b)
                self.assertEqual(chest['nbt']['LootTable'], 'bocchi:chests/stage_backstage')

    def test_locate_and_pool_references(self):
        for style in ['starry', 'school', 'indie']:
            structure = json.loads((DATA / f'worldgen/structure/{style}_stage.json').read_text(encoding='utf-8'))
            pool_id = structure['start_pool'].split(':')[1]
            pool = json.loads((DATA / f'worldgen/template_pool/{pool_id}.json').read_text(encoding='utf-8'))
            self.assertEqual(len(pool['elements']), 3)

            for entry in pool['elements']:
                location = entry['element']['location'].split(':')[1]
                self.assertTrue((DATA / f'structure/{location}.nbt').is_file())

            tag = structure['biomes'].split(':')[1]
            self.assertTrue((DATA / f'tags/worldgen/biome/{tag}.json').is_file())

        structure_set = json.loads((DATA / 'worldgen/structure_set/stages.json').read_text(encoding='utf-8'))
        self.assertEqual(
            {e['structure'] for e in structure_set['structures']},
            {f'bocchi:{s}_stage' for s in ['starry', 'school', 'indie']}
        )

    def test_consistent_translations(self):
        keys = {f'entity.bocchi.{kind}_{form}' for kind in ['bocchi', 'nijika', 'ryo', 'kita'] for form in ['plush', 'chibi']}
        for locale in ['ru_ru', 'be_by', 'en_us']:
            data = json.loads((ROOT / f'src/main/resources/assets/bocchi/lang/{locale}.json').read_text(encoding='utf-8'))
            self.assertTrue(keys <= data.keys())
            for key in keys:
                self.assertIn(' · ', data[key])
                self.assertIn(data[key], data[key.replace('entity.', 'item.') + '_summoner'])


if __name__ == '__main__':
    unittest.main()
