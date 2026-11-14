import { demoRotations } from './rotation.mock';
import { CROP_IMAGES } from './crop-images';
import { existsSync } from 'node:fs';
import { join } from 'node:path';

describe('rotation API fixtures', () => {
  it('creates independent plans with unique IDs and consecutive seasons', () => {
    const response = demoRotations(2030);
    expect(new Set(response.rotations.map((r) => r.id)).size).toBe(response.rotations.length);
    for (const plan of response.rotations) {
      expect(plan.seasons.map((s) => s.year)).toEqual([2030, 2031, 2032]);
      expect(plan.justifications.length).toBeGreaterThan(0);
      expect(plan.risks.length).toBeGreaterThan(0);
      expect(plan.needsReview.length).toBeGreaterThan(0);
    }
    response.rotations[0]!.title = 'changed';
    expect(demoRotations(2030).rotations[0]!.title).not.toBe('changed');
  });
  it('ships each referenced photo locally with attribution metadata', () => {
    for (const image of Object.values(CROP_IMAGES)) {
      expect(existsSync(join(__dirname, '../../../public', image.src))).toBe(true);
      expect(image.author).toBeTruthy();
      expect(image.source).toMatch(/^https:/);
      expect(image.license).toBeTruthy();
    }
  });
});
