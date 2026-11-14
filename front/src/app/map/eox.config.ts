/** WMTS REST, Web Mercator, 256px; order is matrix / row / column (z/y/x). */
export const EOX_CONFIG = {
  year: 2025,
  endpoint: 'https://tiles.maps.eox.at/wmts/1.0.0',
  center: [-23.55, -46.633] as [number, number],
  zoom: 9,
  maxNativeZoom: 14,
  maxZoom: 18,
};

export function eoxLayer(config = EOX_CONFIG) {
  return {
    url: `${config.endpoint}/s2cloudless-${config.year}_3857/default/g/{z}/{y}/{x}.jpg`,
    options: {
      tileSize: 256,
      minZoom: 0,
      maxNativeZoom: config.maxNativeZoom,
      maxZoom: config.maxZoom,
      // WMTS rows start at the north; do not enable TMS inversion.
      tms: false,
      attribution: `<a href="https://cloudless.eox.at">EOxCloudless</a> by <a href="https://eox.at">EOX IT Services GmbH</a> (Contains modified Copernicus Sentinel data ${config.year}) · <a href="https://creativecommons.org/licenses/by-nc-sa/4.0/">CC BY-NC-SA 4.0</a>`,
    },
  };
}
