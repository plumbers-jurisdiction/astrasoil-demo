/** GET /v1/climate/area?latitude=…&longitude=… */
export interface ClimateArea {
  southLatitude: number;
  westLongitude: number;
  northLatitude: number;
  eastLongitude: number;
}
/** Illustrative 0.5° × 0.625° cell. Grid origin is a mock, not verified NASA POWER geometry. */
export function mockClimateArea(latitude: number, longitude: number): ClimateArea {
  if (
    !Number.isFinite(latitude) ||
    !Number.isFinite(longitude) ||
    Math.abs(latitude) > 90 ||
    Math.abs(longitude) > 180
  ) {
    throw new RangeError('Coordenadas fora do intervalo permitido.');
  }
  const southLatitude = Math.min(89.5, Math.floor((latitude + 90) / 0.5) * 0.5 - 90);
  const westLongitude = Math.min(179.375, Math.floor((longitude + 180) / 0.625) * 0.625 - 180);
  return {
    southLatitude,
    westLongitude,
    northLatitude: southLatitude + 0.5,
    eastLongitude: westLongitude + 0.625,
  };
}
