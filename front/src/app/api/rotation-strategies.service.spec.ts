import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { RotationStrategiesService } from './rotation-strategies.service';
import { API_BASE_URL } from './api.config';
describe('RotationStrategiesService', () => {
  beforeEach(() =>
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: API_BASE_URL, useValue: 'https://backend.test' },
      ],
    }),
  );
  afterEach(() => TestBed.inject(HttpTestingController).verify());
  it('posts selected coordinates and ordered history and normalizes absent covers', () => {
    const payload = {
      latitude: 42,
      longitude: -93.6,
      history: [{ year: 2026, plants: ['Wheat', 'Soybean'] }],
    };
    let result: unknown;
    TestBed.inject(RotationStrategiesService)
      .generate(payload)
      .subscribe((v) => (result = v));
    const req = TestBed.inject(HttpTestingController).expectOne(
      'https://backend.test/v1/rotation-strategies',
    );
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);
    req.flush([
      {
        title: 'Healthier soil',
        years: [
          { year: 2027, plant: 'Soybean', coverCrop: {} },
          { year: 2028, plant: 'Corn', coverCrop: { plant: 'Hairy vetch', reason: 'Nitrogen' } },
        ],
        justifications: [],
        risks: [],
        reviewPoints: [],
      },
    ]);
    expect(result).toEqual([
      expect.objectContaining({
        years: [
          { year: 2027, plant: 'Soybean', coverCrop: null },
          { year: 2028, plant: 'Corn', coverCrop: { plant: 'Hairy vetch', reason: 'Nitrogen' } },
        ],
      }),
    ]);
  });
  it('propagates backend errors and rejects malformed responses', () => {
    const service = TestBed.inject(RotationStrategiesService),
      http = TestBed.inject(HttpTestingController),
      error = jest.fn();
    const payload = { latitude: 0, longitude: 0, history: [] };
    service.generate(payload).subscribe({ error });
    http
      .expectOne('https://backend.test/v1/rotation-strategies')
      .flush({}, { status: 404, statusText: 'Not found' });
    expect(error).toHaveBeenCalledTimes(1);
    service.generate(payload).subscribe({ error });
    http.expectOne('https://backend.test/v1/rotation-strategies').flush({ rotations: [] });
    expect(error).toHaveBeenCalledTimes(2);
  });
});
