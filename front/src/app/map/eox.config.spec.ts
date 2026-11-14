import { EOX_CONFIG, eoxLayer } from './eox.config';

describe('EOX tile configuration', () => {
  it('uses HTTPS WMTS row/column order, not inverted TMS', () => {
    const layer = eoxLayer();
    expect(layer.url.replace('{z}', '9').replace('{x}', '189').replace('{y}', '290')).toBe(
      'https://tiles.maps.eox.at/wmts/1.0.0/s2cloudless-2025_3857/default/g/9/290/189.jpg',
    );
    expect(layer.options.tms).toBe(false);
    expect(layer.options.tileSize).toBe(256);
  });
  it('keeps vintage synchronized between image URL and credit', () => {
    const layer = eoxLayer({ ...EOX_CONFIG, year: 2024 });
    expect(layer.url).toContain('s2cloudless-2024_3857');
    expect(layer.options.attribution).toContain('Copernicus Sentinel data 2024');
    expect(layer.options.attribution).toContain('EOX IT Services GmbH');
    expect(layer.options.attribution).toContain('by-nc-sa/4.0');
  });
  it('overzooms native imagery and starts within Web Mercator limits', () => {
    expect(eoxLayer().options.maxNativeZoom).toBe(14);
    expect(eoxLayer().options.maxZoom).toBeGreaterThan(14);
    expect(Math.abs(EOX_CONFIG.center[0])).toBeLessThan(85.051129);
    expect(Math.abs(EOX_CONFIG.center[1])).toBeLessThanOrEqual(180);
  });
});
