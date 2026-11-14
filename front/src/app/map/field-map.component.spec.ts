import { of, throwError } from 'rxjs';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import * as L from 'leaflet';
import { FieldMapComponent } from './field-map.component';
import { ClimateAreaService } from '../api/climate-area.service';

jest.mock('leaflet', () => {
  const layer = () => ({
    addTo: jest.fn().mockReturnThis(),
    remove: jest.fn(),
    off: jest.fn(),
    on: jest.fn().mockReturnThis(),
    redraw: jest.fn(),
    bindTooltip: jest.fn(),
    getBounds: jest.fn().mockReturnValue('bounds'),
  });
  return {
    map: jest.fn(() => ({
      setView: jest.fn().mockReturnThis(),
      on: jest.fn(),
      invalidateSize: jest.fn(),
      fitBounds: jest.fn(),
      remove: jest.fn(),
    })),
    tileLayer: jest.fn(layer),
    rectangle: jest.fn(layer),
    circleMarker: jest.fn(layer),
  };
});

describe('FieldMapComponent', () => {
  let fixture: ComponentFixture<FieldMapComponent>;
  let component: FieldMapComponent;
  let fetchMock: jest.Mock;
  let disconnect: jest.Mock;
  let resizeCallback: ResizeObserverCallback;
  let geo: jest.Mock;
  const feature = (properties: object, coordinates = [-47, -23]) => ({
    geometry: { coordinates },
    properties,
  });
  const response = (features: unknown[]) => ({ ok: true, json: async () => ({ features }) });
  const event = () => new Event('submit', { cancelable: true });
  const lastMap = () => jest.mocked(L.map).mock.results.at(-1)!.value;
  const lastTiles = () => jest.mocked(L.tileLayer).mock.results.at(-1)!.value;
  const flush = async () => {
    for (let i = 0; i < 8; i++) await Promise.resolve();
  };
  beforeEach(() => {
    jest.useFakeTimers();
    disconnect = jest.fn();
    global.ResizeObserver = jest.fn().mockImplementation((cb) => {
      resizeCallback = cb;
      return { observe: jest.fn(), disconnect };
    });
    fetchMock = jest.fn().mockResolvedValue(response([]));
    global.fetch = fetchMock;
    geo = jest.fn();
    Object.defineProperty(navigator, 'geolocation', {
      configurable: true,
      value: { getCurrentPosition: geo },
    });
    Object.defineProperty(window, 'isSecureContext', { configurable: true, value: true });
    TestBed.configureTestingModule({
      imports: [FieldMapComponent],
      providers: [
        {
          provide: ClimateAreaService,
          useValue: {
            getArea: jest.fn(() =>
              of({
                southLatitude: -24,
                northLatitude: -23.5,
                westLongitude: -46.875,
                eastLongitude: -46.25,
              }),
            ),
          },
        },
      ],
    });
    fixture = TestBed.createComponent(FieldMapComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });
  afterEach(() => {
    fixture.destroy();
    jest.clearAllTimers();
    jest.useRealTimers();
  });
  it('initializes EOX, invalidates size and preserves viewport when switching maps', () => {
    const map = lastMap();
    expect(map.setView).toHaveBeenCalledTimes(1);
    expect(L.tileLayer).toHaveBeenLastCalledWith(
      expect.stringContaining('s2cloudless'),
      expect.objectContaining({ tms: false }),
    );
    resizeCallback([], {} as ResizeObserver);
    expect(map.invalidateSize).toHaveBeenCalledWith({ pan: false });
    const previous = lastTiles();
    component.toggleBase();
    expect(previous.remove).toHaveBeenCalled();
    expect(previous.off).toHaveBeenCalled();
    expect(L.tileLayer).toHaveBeenLastCalledWith(
      'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
      expect.objectContaining({ attribution: expect.stringContaining('OpenStreetMap') }),
    );
    component.toggleBase();
    expect(component.osm()).toBe(false);
    expect(map.setView).toHaveBeenCalledTimes(1);
  });
  it('clears stale geometry and reports backend failure without drawing mock bounds', () => {
    jest
      .spyOn(TestBed.inject(ClimateAreaService), 'getArea')
      .mockReturnValue(throwError(() => new Error('offline')));
    component.choosePlace({ lat: -23, lon: -47, label: 'Selected place' });
    expect(component.areaLoading()).toBe(false);
    expect(component.areaError()).toContain('Could not');
    expect(L.rectangle).not.toHaveBeenCalled();
  });
  it('reports tile errors and retries only the active layer', () => {
    const tile = lastTiles();
    const handler = tile.on.mock.calls.find((c: unknown[]) => c[0] === 'tileerror')![1];
    handler();
    expect(component.failed()).toBe(true);
    component.retry();
    expect(component.failed()).toBe(false);
    expect(tile.redraw).toHaveBeenCalled();
  });
  it('searches only submitted valid queries and formats place labels without duplicated city', async () => {
    await component.searchAddress(event(), 'ab');
    expect(fetchMock).not.toHaveBeenCalled();
    fetchMock.mockResolvedValue(
      response([
        feature({ name: 'Campinas', city: 'Campinas', state: 'São Paulo' }),
        feature({ name: 'Fazenda A', city: 'Campinas', state: 'São Paulo' }),
        feature({}, [NaN, 0]),
      ]),
    );
    await component.searchAddress(event(), ' Campinas ');
    expect(String(fetchMock.mock.calls[0]![0])).toContain('q=Campinas');
    expect(component.results().map((p) => p.label)).toEqual([
      'Campinas – São Paulo',
      'Fazenda A · Campinas – São Paulo',
    ]);
    expect(component.searching()).toBe(false);
  });
  it('handles absent features, fallback labels and malformed coordinates', async () => {
    fetchMock.mockResolvedValue(
      response([
        feature({ country: 'Brasil' }),
        feature({}),
        feature({}, [0, 86]),
        feature({}, [181, 0]),
      ]),
    );
    await component.searchAddress(event(), 'Brasil');
    expect(component.results().map((p) => p.label)).toEqual(['Brasil', '-23, -47']);
    fetchMock.mockResolvedValue({ ok: true, json: async () => ({}) });
    await component.searchAddress(event(), 'missing');
    expect(component.results()).toEqual([]);
  });
  it('handles network and HTTP failures and releases the busy state', async () => {
    fetchMock.mockRejectedValueOnce(new Error('offline')).mockResolvedValueOnce({ ok: false });
    for (let i = 0; i < 2; i++) {
      await component.searchAddress(event(), 'Campinas');
      expect(component.locationMessage()).toContain('Could not');
      expect(component.searching()).toBe(false);
    }
  });
  it('blocks concurrent searches and aborts a stalled request on timeout', async () => {
    fetchMock.mockImplementation(
      (_url, options) =>
        new Promise((_resolve, reject) =>
          options.signal.addEventListener('abort', () => reject(new Error('aborted'))),
        ),
    );
    const pending = component.searchAddress(event(), 'Campinas');
    await component.searchAddress(event(), 'Osasco');
    expect(fetchMock).toHaveBeenCalledTimes(1);
    jest.advanceTimersByTime(12000);
    await pending;
    expect(component.searching()).toBe(false);
  });
  it('draws API-shaped gray bounds and emits selected search name without reverse request', () => {
    const emit = jest.fn();
    component.placeSelected.subscribe(emit);
    const service = TestBed.inject(ClimateAreaService);
    const spy = jest.spyOn(service, 'getArea');
    component.choosePlace({ lat: -23.55, lon: -46.63, label: 'Fazenda A · São Paulo' });
    expect(spy).toHaveBeenCalledWith(-23.55, expect.closeTo(-46.63, 8));
    expect(L.rectangle).toHaveBeenLastCalledWith(
      [
        [-24, -46.875],
        [-23.5, -46.25],
      ],
      expect.objectContaining({ fillColor: '#80888d', fillOpacity: 0.25, interactive: false }),
    );
    expect(lastMap().fitBounds).toHaveBeenCalledWith('bounds', { padding: [32, 32] });
    expect(emit).toHaveBeenCalledWith('Fazenda A · São Paulo');
    expect(fetchMock).not.toHaveBeenCalled();
  });
  const click = (lat = -23.55, lon = -46.63) => {
    const handler = lastMap().on.mock.calls.find((c: unknown[]) => c[0] === 'click')![1];
    handler({ latlng: { lat, lng: lon, wrap: () => ({ lng: lon }) } });
  };
  it('reverse-geocodes clicks after debounce and does not present nearby farm as exact selection', async () => {
    const emit = jest.fn();
    component.placeSelected.subscribe(emit);
    fetchMock.mockResolvedValue(
      response([feature({ name: 'Nearby farm', city: 'Campinas', state: 'São Paulo' })]),
    );
    click();
    expect(lastMap().fitBounds).not.toHaveBeenCalled();
    jest.advanceTimersByTime(600);
    await flush();
    expect(emit).toHaveBeenLastCalledWith('Near Campinas – São Paulo');
    expect(String(fetchMock.mock.calls[0]![0])).toContain('/reverse?');
  });
  it('uses coordinate fallback when reverse lookup fails or returns no city', async () => {
    const emit = jest.fn();
    component.placeSelected.subscribe(emit);
    click();
    jest.advanceTimersByTime(600);
    await flush();
    expect(emit).toHaveBeenLastCalledWith(expect.stringContaining('Selected location · -23.5500'));
    fetchMock.mockRejectedValue(new Error('offline'));
    click();
    jest.advanceTimersByTime(600);
    await flush();
    expect(emit).toHaveBeenLastCalledWith(expect.stringContaining('Selected location'));
  });
  it('ignores stale reverse results after a new search selection', async () => {
    let resolve!: (value: unknown) => void;
    fetchMock.mockImplementation(() => new Promise((r) => (resolve = r)));
    const emit = jest.fn();
    component.placeSelected.subscribe(emit);
    click();
    jest.advanceTimersByTime(600);
    component.choosePlace({ lat: -22, lon: -47, label: 'New place' });
    resolve(response([feature({ city: 'Old place' })]));
    await flush();
    expect(emit).toHaveBeenLastCalledWith('New place');
  });
  it('centers successful geolocation and prevents duplicate requests while locating', () => {
    component.useLocation();
    component.useLocation();
    expect(geo).toHaveBeenCalledTimes(1);
    geo.mock.calls[0]![0]({ coords: { latitude: -23, longitude: -47, accuracy: 12 } });
    expect(component.locating()).toBe(false);
    expect(component.locationMessage()).toContain('12 m');
    expect(lastMap().fitBounds).toHaveBeenCalled();
  });
  it.each([
    [1, 'blocked'],
    [2, 'unavailable'],
    [3, '30 seconds'],
    [99, 'Could not'],
  ])('handles geolocation error %s', (code, text) => {
    component.useLocation();
    geo.mock.calls[0]![1]({ code });
    expect(component.locating()).toBe(false);
    expect(component.locationMessage()).toContain(text);
  });
  it('rejects insecure contexts and unsupported geolocation', () => {
    Object.defineProperty(window, 'isSecureContext', { value: false });
    component.useLocation();
    expect(component.locationMessage()).toContain('HTTPS');
    Object.defineProperty(window, 'isSecureContext', { value: true });
    Object.defineProperty(navigator, 'geolocation', { value: undefined });
    component.useLocation();
    expect(component.locationMessage()).toContain('does not support');
    expect(geo).not.toHaveBeenCalled();
  });
  it('cleans map resources and ignores delayed geolocation callbacks after destruction', () => {
    component.useLocation();
    fixture.destroy();
    geo.mock.calls[0]![0]({ coords: { latitude: 0, longitude: 0, accuracy: 1 } });
    expect(disconnect).toHaveBeenCalled();
    expect(lastMap().remove).toHaveBeenCalledTimes(1);
    expect(L.rectangle).not.toHaveBeenCalled();
  });
  it('aborts pending search and ignores responses after destruction', async () => {
    let resolve!: (value: unknown) => void;
    fetchMock.mockImplementation(() => new Promise((r) => (resolve = r)));
    const pending = component.searchAddress(event(), 'Campinas');
    const signal = fetchMock.mock.calls[0]![1].signal;
    fixture.destroy();
    expect(signal.aborted).toBe(true);
    resolve(response([feature({ name: 'Late' })]));
    await pending;
    expect(component.results()).toEqual([]);
  });
});
