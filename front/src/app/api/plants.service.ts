import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';

/** GET /v1/plants → Array<string>. Keep API values unchanged in form data. */
export const PLANTS_MOCK: string[] = [
  'Corn',
  'Soybean',
  'Wheat',
  'Alfalfa',
  'Cotton',
  'Sorghum',
  'Rice',
  'Barley',
  'Cereal rye',
  'Canola',
  'Oats',
  'Peanut',
  'Dry bean',
  'Sugarbeet',
  'Dry pea',
  'Lentil',
  'Sunflower',
  'Potato',
  'Chickpea',
  'Proso millet',
  'Hairy vetch',
  'Red clover',
  'Bare soil',
];
@Injectable({ providedIn: 'root' })
export class PlantsService {
  getPlants(): Observable<string[]> {
    // Future: return this.http.get<string[]>(`${apiBaseUrl}/v1/plants`);
    return of([...PLANTS_MOCK]);
  }
}
