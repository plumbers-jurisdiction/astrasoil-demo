import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { ClimateArea, mockClimateArea } from './climate-area.mock';
@Injectable({ providedIn: 'root' })
export class ClimateAreaService {
  getArea(latitude: number, longitude: number): Observable<ClimateArea> {
    // Future: GET /v1/climate/area with latitude and longitude query parameters.
    return of(mockClimateArea(latitude, longitude));
  }
}
