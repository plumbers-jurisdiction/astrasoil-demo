import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { PlantsService, PLANTS_MOCK } from './plants.service';

describe('PlantsService', () => {
  it('returns all 22 API plant names plus bare soil, without duplicates', async () => {
    const plants = await firstValueFrom(TestBed.inject(PlantsService).getPlants());
    expect(plants).toEqual(PLANTS_MOCK);
    expect(plants).toHaveLength(23);
    expect(new Set(plants).size).toBe(23);
    expect(plants).toEqual(expect.arrayContaining(['Corn', 'Soybean', 'Red clover', 'Bare soil']));
  });
  it('returns independent arrays so consumers cannot mutate the catalog', async () => {
    const service = TestBed.inject(PlantsService);
    (await firstValueFrom(service.getPlants())).pop();
    expect(await firstValueFrom(service.getPlants())).toHaveLength(23);
  });
});
