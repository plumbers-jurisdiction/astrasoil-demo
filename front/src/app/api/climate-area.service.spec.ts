import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ClimateAreaService } from './climate-area.service';
import { API_BASE_URL } from './api.config';
describe('ClimateAreaService HTTP contract', () => {
  let http: HttpTestingController;
  let service: ClimateAreaService;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: API_BASE_URL, useValue: 'https://backend.test' },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    service = TestBed.inject(ClimateAreaService);
  });
  afterEach(() => http.verify());
  it('uses latitude/longitude query and returns server edges unchanged', () => {
    const area = {
      southLatitude: -23.75,
      westLongitude: -47.1875,
      northLatitude: -23.25,
      eastLongitude: -46.5625,
    };
    service.getArea(-23.55, -46.63).subscribe((a) => expect(a).toEqual(area));
    const req = http.expectOne((r) => r.url === 'https://backend.test/v1/climate/area');
    expect(req.request.params.get('latitude')).toBe('-23.55');
    expect(req.request.params.get('longitude')).toBe('-46.63');
    req.flush(area);
  });
  it('rejects invalid input without sending a request', () => {
    const error = jest.fn();
    service.getArea(NaN, 181).subscribe({ error });
    expect(error).toHaveBeenCalled();
    http.expectNone(() => true);
  });
  it('rejects inverted bounds and propagates HTTP errors', () => {
    const error = jest.fn();
    service.getArea(0, 0).subscribe({ error });
    http
      .expectOne((r) => r.url.endsWith('/area'))
      .flush({ southLatitude: 1, northLatitude: 0, westLongitude: 0, eastLongitude: 1 });
    expect(error).toHaveBeenCalledTimes(1);
    service.getArea(0, 0).subscribe({ error });
    http.expectOne((r) => r.url.endsWith('/area')).flush({}, { status: 500, statusText: 'Error' });
    expect(error).toHaveBeenCalledTimes(2);
  });
});
