import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { PlantsService } from './plants.service';
import { API_BASE_URL } from './api.config';
describe('PlantsService HTTP contract', () => {
  let http: HttpTestingController;
  let service: PlantsService;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: API_BASE_URL, useValue: 'https://backend.test' },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    service = TestBed.inject(PlantsService);
  });
  afterEach(() => http.verify());
  it('loads and deduplicates actual API strings without mock fallback', () => {
    let result: string[] = [];
    service.getPlants().subscribe((p) => (result = p));
    const req = http.expectOne('https://backend.test/v1/plants');
    expect(req.request.method).toBe('GET');
    req.flush(['Corn', 'Soybean', 'Corn']);
    expect(result).toEqual(['Corn', 'Soybean']);
  });
  it('preserves an empty API list', () => {
    service.getPlants().subscribe((p) => expect(p).toEqual([]));
    http.expectOne('https://backend.test/v1/plants').flush([]);
  });
  it('rejects malformed responses', () => {
    const error = jest.fn();
    service.getPlants().subscribe({ error });
    http.expectOne('https://backend.test/v1/plants').flush({ plants: ['Corn'] });
    expect(error).toHaveBeenCalled();
  });
  it('propagates HTTP failures', () => {
    const error = jest.fn();
    service.getPlants().subscribe({ error });
    http
      .expectOne('https://backend.test/v1/plants')
      .flush({ message: 'Unavailable' }, { status: 500, statusText: 'Error' });
    expect(error).toHaveBeenCalled();
  });
});
