import {
  AfterViewInit,
  Component,
  ElementRef,
  NgZone,
  OnDestroy,
  ViewChild,
  signal,
  output,
  input,
} from '@angular/core';
import * as L from 'leaflet';
import { ClimateAreaService } from '../api/climate-area.service';
import { Subscription } from 'rxjs';
import { EOX_CONFIG, eoxLayer } from './eox.config';

@Component({
  selector: 'app-field-map',
  standalone: true,
  templateUrl: './field-map.component.html',
})
export class FieldMapComponent implements AfterViewInit, OnDestroy {
  @ViewChild('canvas', { static: true }) private canvas!: ElementRef<HTMLDivElement>;
  readonly selectedPlace = input('Select a location on the map');
  readonly placeSelected = output<string>();
  private reverseController?: AbortController;
  private reverseTimer?: number;
  private selectionVersion = 0;
  readonly year = EOX_CONFIG.year;
  readonly failed = signal(false);
  readonly osm = signal(false);
  readonly searching = signal(false);
  readonly locating = signal(false);
  readonly locationMessage = signal('');
  readonly results = signal<{ lat: number; lon: number; label: string }[]>([]);
  private pin?: L.CircleMarker;
  private selectedArea?: L.Rectangle;
  private areaRequest?: Subscription;
  private searchController?: AbortController;
  private destroyed = false;
  private map?: L.Map;
  private tiles?: L.TileLayer;
  private resize?: ResizeObserver;
  constructor(
    private zone: NgZone,
    private climateArea: ClimateAreaService,
  ) {}

  ngAfterViewInit(): void {
    this.zone.runOutsideAngular(() => {
      this.map = L.map(this.canvas.nativeElement, { scrollWheelZoom: false }).setView(
        EOX_CONFIG.center,
        EOX_CONFIG.zoom,
      );
      this.map.on('click', (event: L.LeafletMouseEvent) => {
        this.focusLocation(event.latlng.lat, event.latlng.lng, false);
        this.zone.run(() => {
          this.results.set([]);
          this.locationMessage.set(
            `Selected area · ${event.latlng.lat.toFixed(5)}, ${event.latlng.wrap().lng.toFixed(5)} · sample climate area.`,
          );
        });
      });
      this.setBaseLayer();
      this.resize = new ResizeObserver(() => this.map?.invalidateSize({ pan: false }));
      this.resize.observe(this.canvas.nativeElement);
    });
  }
  async searchAddress(event: Event, value: string): Promise<void> {
    event.preventDefault();
    const query = value.trim();
    if (query.length < 3 || this.searching() || this.locating()) return;
    this.searching.set(true);
    this.results.set([]);
    this.locationMessage.set('');
    this.searchController = new AbortController();
    const timeout = window.setTimeout(() => this.searchController?.abort(), 12000);
    try {
      const url = new URL('https://photon.komoot.io/api/');
      url.searchParams.set('q', query);
      url.searchParams.set('limit', '5');
      const response = await fetch(url, { signal: this.searchController.signal });
      if (!response.ok) throw new Error('search');
      const data = await response.json();
      if (this.destroyed) return;
      const places: { lat: number; lon: number; label: string }[] = [];
      for (const feature of data.features ?? []) {
        const [lon, lat] = feature.geometry?.coordinates ?? [];
        if (
          !Number.isFinite(lat) ||
          !Number.isFinite(lon) ||
          Math.abs(lat) > 85 ||
          Math.abs(lon) > 180
        )
          continue;
        const p = feature.properties ?? {};
        const label = this.formatPlace(p);
        places.push({ lat, lon, label: label || `${lat}, ${lon}` });
      }
      this.results.set(places);
      this.locationMessage.set(
        places.length
          ? 'Select a result to locate it on the map.'
          : 'No places found. Try including a city and state.',
      );
    } catch {
      if (!this.destroyed)
        this.locationMessage.set(
          'Could not search for the address. Check your connection and try again.',
        );
    } finally {
      window.clearTimeout(timeout);
      if (!this.destroyed) this.searching.set(false);
    }
  }
  choosePlace(place: { lat: number; lon: number; label: string }): void {
    this.focusLocation(place.lat, place.lon, true, place.label);
    this.results.set([]);
    this.locationMessage.set(place.label);
  }
  useLocation(): void {
    if (this.locating() || this.searching()) return;
    if (!window.isSecureContext) {
      this.locationMessage.set('Location requires HTTPS or localhost. Use address search instead.');
      return;
    }
    if (!navigator.geolocation) {
      this.locationMessage.set(
        'Your browser does not support location. Use address search instead.',
      );
      return;
    }
    this.locating.set(true);
    this.results.set([]);
    this.locationMessage.set('Waiting for your browser location…');
    navigator.geolocation.getCurrentPosition(
      (position) =>
        this.zone.run(() => {
          if (this.destroyed) return;
          this.locating.set(false);
          this.focusLocation(position.coords.latitude, position.coords.longitude);
          this.locationMessage.set(
            `Approximate current location · accuracy of ${Math.round(position.coords.accuracy)} m.`,
          );
        }),
      (error) =>
        this.zone.run(() => {
          if (this.destroyed) return;
          this.locating.set(false);
          const messages: Record<number, string> = {
            1: 'Location access is blocked. Allow location for this site in your browser and for your browser in system Location Services. Then try again or search for an address.',
            2: 'Your browser could not determine your position (position unavailable). Check system Location Services and your connection. If using the in-app browser, try Chrome or Safari, or search for an address.',
            3: 'Your browser could not locate you within 30 seconds. Check system Location Services and try again, or search for an address.',
          };
          this.locationMessage.set(
            messages[error.code] ?? 'Could not obtain your location. Use address search instead.',
          );
        }),
      { enableHighAccuracy: true, timeout: 30000, maximumAge: 60000 },
    );
  }
  private focusLocation(lat: number, lon: number, fit = true, knownLabel?: string): void {
    this.updatePlaceLabel(lat, lon, knownLabel);
    this.areaRequest?.unsubscribe();
    this.areaRequest = this.climateArea
      .getArea(lat, ((((lon + 180) % 360) + 360) % 360) - 180)
      .subscribe((area) =>
        this.zone.runOutsideAngular(() => {
          if (!this.map) return;
          this.selectedArea?.remove();
          this.selectedArea = L.rectangle(
            [
              [area.southLatitude, area.westLongitude],
              [area.northLatitude, area.eastLongitude],
            ],
            {
              color: '#626b70',
              opacity: 0.8,
              weight: 2,
              dashArray: '8 5',
              fillColor: '#80888d',
              fillOpacity: 0.25,
              interactive: false,
            },
          ).addTo(this.map);
          this.selectedArea.bindTooltip('Climate area · sample', {
            permanent: true,
            direction: 'top',
            className: 'area-tooltip',
          });
          if (fit) this.map.fitBounds(this.selectedArea.getBounds(), { padding: [32, 32] });
          this.pin?.remove();
          this.pin = L.circleMarker([lat, lon], {
            radius: 8,
            color: '#fff',
            weight: 3,
            fillColor: '#245c43',
            fillOpacity: 1,
            interactive: false,
          }).addTo(this.map);
        }),
      );
  }
  private formatPlace(properties: Record<string, unknown>): string {
    const text = (key: string): string =>
      typeof properties[key] === 'string' ? (properties[key] as string) : '';
    const city =
      text('city') || text('town') || text('village') || text('municipality') || text('county');
    const state = text('state');
    const region = [city, state].filter(Boolean).join(' – ');
    const name = text('name');
    return (
      [...new Set([name === city ? '' : name, region].filter(Boolean))].join(' · ') ||
      text('country')
    );
  }
  private updatePlaceLabel(lat: number, lon: number, knownLabel?: string): void {
    const version = ++this.selectionVersion;
    this.reverseController?.abort();
    window.clearTimeout(this.reverseTimer);
    const normalizedLon = ((((lon + 180) % 360) + 360) % 360) - 180;
    const fallback = `${lat.toFixed(4)}, ${normalizedLon.toFixed(4)}`;
    this.zone.run(() =>
      this.placeSelected.emit(knownLabel || `Identifying location · ${fallback}`),
    );
    if (knownLabel) return;
    this.reverseTimer = window.setTimeout(async () => {
      const controller = new AbortController();
      this.reverseController = controller;
      const timeout = window.setTimeout(() => controller.abort(), 10000);
      try {
        const url = new URL('https://photon.komoot.io/reverse');
        url.searchParams.set('lat', String(lat));
        url.searchParams.set('lon', String(normalizedLon));
        const response = await fetch(url, { signal: controller.signal });
        if (!response.ok) throw new Error('reverse');
        const data = await response.json();
        if (this.destroyed || version !== this.selectionVersion) return;
        // A reverse result is a nearby mapped feature; do not claim its name is the selected farm.
        const properties = data.features?.[0]?.properties ?? {};
        const city =
          properties.city || properties.town || properties.village || properties.municipality;
        const label = [city, properties.state].filter(Boolean).join(' – ');
        this.zone.run(() =>
          this.placeSelected.emit(label ? `Near ${label}` : `Selected location · ${fallback}`),
        );
      } catch {
        if (!this.destroyed && version === this.selectionVersion)
          this.zone.run(() => this.placeSelected.emit(`Selected location · ${fallback}`));
      } finally {
        window.clearTimeout(timeout);
      }
    }, 600);
  }
  toggleBase(): void {
    this.osm.update((value) => !value);
    this.zone.runOutsideAngular(() => this.setBaseLayer());
  }
  private setBaseLayer(): void {
    if (!this.map) return;
    this.tiles?.remove();
    this.tiles?.off();
    this.failed.set(false);
    const eox = eoxLayer();
    this.tiles = this.osm()
      ? L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
          maxZoom: EOX_CONFIG.maxZoom,
          attribution:
            '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
        })
      : L.tileLayer(eox.url, eox.options);
    this.tiles.on('tileerror', () => this.zone.run(() => this.failed.set(true)));
    this.tiles.addTo(this.map);
  }
  retry(): void {
    this.failed.set(false);
    this.tiles?.redraw();
  }
  ngOnDestroy(): void {
    this.destroyed = true;
    window.clearTimeout(this.reverseTimer);
    this.reverseController?.abort();
    this.areaRequest?.unsubscribe();
    this.searchController?.abort();
    this.resize?.disconnect();
    this.map?.remove();
  }
}
