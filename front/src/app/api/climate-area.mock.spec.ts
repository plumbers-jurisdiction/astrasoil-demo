import { mockClimateArea } from './climate-area.mock';
describe('climate grid mock', () => {
  it.each([
    [-23.55, -46.63],
    [0, 0],
    [90, 180],
    [-90, -180],
  ])('contains coordinate %s,%s in a 0.5 × 0.625 degree cell', (lat, lon) => {
    const a = mockClimateArea(lat, lon);
    expect(a.southLatitude).toBeLessThanOrEqual(lat);
    expect(a.northLatitude).toBeGreaterThanOrEqual(lat);
    expect(a.westLongitude).toBeLessThanOrEqual(lon);
    expect(a.eastLongitude).toBeGreaterThanOrEqual(lon);
    expect(a.northLatitude - a.southLatitude).toBe(0.5);
    expect(a.eastLongitude - a.westLongitude).toBe(0.625);
  });
  it.each([
    [91, 0],
    [0, 181],
    [NaN, 0],
    [0, Infinity],
  ])('rejects invalid coordinate %s,%s', (lat, lon) =>
    expect(() => mockClimateArea(lat, lon)).toThrow(RangeError),
  );
});
