import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map, timeout } from 'rxjs';
import { API_BASE_URL } from './api.config';
import { RotationPlan, RotationRequest } from '../rotations/rotation.model';
@Injectable({ providedIn: 'root' })
export class RotationStrategiesService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);
  generate(request: RotationRequest): Observable<RotationPlan[]> {
    return this.http.post<unknown>(`${this.baseUrl}/v1/rotation-strategies`, request).pipe(
      timeout(120000),
      map((data) => {
        if (!Array.isArray(data)) throw new Error('Invalid rotation response.');
        const strings = (v: unknown): v is string[] =>
          Array.isArray(v) && v.every((s) => typeof s === 'string');
        return data.map((plan) => {
          if (
            !plan ||
            typeof plan.title !== 'string' ||
            !Array.isArray(plan.years) ||
            !strings(plan.justifications) ||
            !strings(plan.risks) ||
            !strings(plan.reviewPoints)
          )
            throw new Error('Invalid rotation plan.');
          return {
            ...plan,
            years: plan.years.map((season: Record<string, unknown>) => {
              if (
                !season ||
                !Number.isInteger(season['year']) ||
                typeof season['plant'] !== 'string' ||
                !season['plant'].trim()
              )
                throw new Error('Invalid rotation year.');
              const cover = season['coverCrop'] as Record<string, unknown> | null | undefined;
              return {
                ...season,
                coverCrop:
                  cover && typeof cover['plant'] === 'string' && cover['plant'].trim()
                    ? {
                        plant: cover['plant'].trim(),
                        reason: typeof cover['reason'] === 'string' ? cover['reason'] : '',
                      }
                    : null,
              };
            }),
          } as RotationPlan;
        });
      }),
    );
  }
}
