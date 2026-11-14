import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { ClimateAreaService } from './climate-area.service';

describe('ClimateAreaService', () => {
  it('returns the API-shaped cell surrounding a Brazilian coordinate', async () => {
    const area = await firstValueFrom(TestBed.inject(ClimateAreaService).getArea(-23.55, -46.63));
    expect(area).toEqual({
      southLatitude: -24,
      northLatitude: -23.5,
      westLongitude: -46.875,
      eastLongitude: -46.25,
    });
  });
  it('rejects non-numeric and out-of-range requests', () => {
    const service = TestBed.inject(ClimateAreaService);
    expect(() => service.getArea(NaN, 0)).toThrow(RangeError);
    expect(() => service.getArea(0, 181)).toThrow(RangeError);
  });
});
