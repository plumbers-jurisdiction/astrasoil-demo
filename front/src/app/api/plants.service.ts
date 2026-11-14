import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map, timeout } from 'rxjs';
import { API_BASE_URL } from './api.config';

@Injectable({ providedIn: 'root' })
export class PlantsService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  getPlants(): Observable<string[]> {
    return this.http.get<unknown>(`${this.baseUrl}/v1/plants`).pipe(
      timeout(60000),
      map((data) => {
        if (
          !Array.isArray(data) ||
          !data.every((plant) => typeof plant === 'string' && plant.trim())
        ) {
          throw new Error('Invalid plants response.');
        }
        return [...new Set(data as string[])];
      }),
    );
  }
}
