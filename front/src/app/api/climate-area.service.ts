import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map, timeout, throwError } from 'rxjs';
import { API_BASE_URL } from './api.config';
import { ClimateArea } from './climate-area.model';

@Injectable({ providedIn: 'root' })
export class ClimateAreaService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  getArea(latitude: number, longitude: number): Observable<ClimateArea> {
    if (
      !Number.isFinite(latitude) ||
      !Number.isFinite(longitude) ||
      Math.abs(latitude) > 90 ||
      Math.abs(longitude) > 180
    ) {
      return throwError(() => new RangeError('Coordinates are outside the allowed range.'));
    }
    return this.http
      .get<ClimateArea>(`${this.baseUrl}/v1/climate/area`, {
        params: { latitude, longitude },
      })
      .pipe(
        timeout(60000),
        map((area) => {
          if (
            !area ||
            ![area.southLatitude, area.northLatitude, area.westLongitude, area.eastLongitude].every(
              Number.isFinite,
            ) ||
            area.southLatitude < -90 ||
            area.northLatitude > 90 ||
            area.westLongitude < -180 ||
            area.eastLongitude > 180 ||
            area.southLatitude >= area.northLatitude ||
            area.westLongitude >= area.eastLongitude
          ) {
            throw new Error('Invalid climate area response.');
          }
          return area;
        }),
      );
  }
}
