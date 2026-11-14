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
  template: `
    <div class="map-sidebar">
      <form class="map-search" (submit)="searchAddress($event, address.value)">
        <label for="map-address">Endereço ou lugar</label>
        <div class="map-search-row">
          <input
            #address
            id="map-address"
            type="search"
            placeholder="Cidade, endereço ou nome do lugar"
            required
            minlength="3"
          /><button type="submit" [disabled]="locating() || searching()">
            {{ searching() ? 'Buscando…' : 'Buscar' }}
          </button>
        </div>
        <button
          type="button"
          class="use-location"
          [disabled]="locating() || searching()"
          (click)="useLocation()"
        >
          {{ locating() ? 'Localizando…' : '◎ Usar minha localização' }}
        </button>
        <small
          >A busca consulta o Photon/Komoot com dados ©
          <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>. A localização atual
          depende da sua permissão no navegador.</small
        >
      </form>
      @if (results().length) {
        <ul class="map-search-results">
          @for (place of results(); track $index) {
            <li>
              <button type="button" (click)="choosePlace(place)">{{ place.label }}</button>
            </li>
          }
        </ul>
      }
      <ng-content></ng-content>
    </div>
    <div class="map-panel">
      <span class="location-pill mobile-location-pill"><i></i>{{ selectedPlace() }}</span>
      <label class="map-switch">
        <span>OpenStreetMap</span>
        <input
          type="checkbox"
          role="switch"
          [checked]="osm()"
          (change)="toggleBase()"
          aria-label="Usar OpenStreetMap"
        />
        <span class="map-switch-track" aria-hidden="true"></span>
        <span class="map-switch-state">{{ osm() ? 'Ligado' : 'Desligado · EOX' }}</span>
      </label>
      <p class="map-area-note">
        Clique no mapa ou busque um local para selecionar uma área climática demonstrativa
        (aproximadamente 50 km).
      </p>
      <div class="field-map-shell">
        <div
          #canvas
          class="field-map"
          role="region"
          [attr.aria-label]="
            osm() ? 'Mapa interativo OpenStreetMap' : 'Mapa interativo Sentinel-2 cloudless'
          "
          aria-describedby="map-description"
        ></div>
        @if (failed()) {
          <div class="map-error" role="status">
            Não foi possível carregar algumas imagens do mapa. Verifique sua conexão.
            <button type="button" (click)="retry()">Tentar novamente</button>
          </div>
        }
      </div>
      <p id="map-description" class="map-description">
        @if (osm()) {
          OpenStreetMap · Mapa de ruas e lugares.
        } @else {
          Sentinel-2 cloudless {{ year }} · Mosaico anual, sem atualização em tempo real.
        }
        Localização ilustrativa; os indicadores ao lado são demonstrativos.
      </p>
    </div>
  `,
})
export class FieldMapComponent implements AfterViewInit, OnDestroy {
  @ViewChild('canvas', { static: true }) private canvas!: ElementRef<HTMLDivElement>;
  readonly selectedPlace = input('Selecione um local no mapa');
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
            `Área selecionada · ${event.latlng.lat.toFixed(5)}, ${event.latlng.wrap().lng.toFixed(5)} · área climática demonstrativa.`,
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
          ? 'Selecione um resultado para localizar no mapa.'
          : 'Nenhum local encontrado. Tente incluir cidade e estado.',
      );
    } catch {
      if (!this.destroyed)
        this.locationMessage.set(
          'Não foi possível buscar o endereço. Verifique a conexão e tente novamente.',
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
      this.locationMessage.set(
        'A localização precisa de HTTPS ou localhost. Use a busca por endereço.',
      );
      return;
    }
    if (!navigator.geolocation) {
      this.locationMessage.set('Seu navegador não oferece localização. Use a busca por endereço.');
      return;
    }
    this.locating.set(true);
    this.results.set([]);
    this.locationMessage.set('Aguardando a localização do navegador…');
    navigator.geolocation.getCurrentPosition(
      (position) =>
        this.zone.run(() => {
          if (this.destroyed) return;
          this.locating.set(false);
          this.focusLocation(position.coords.latitude, position.coords.longitude);
          this.locationMessage.set(
            `Localização atual aproximada · precisão de ${Math.round(position.coords.accuracy)} m.`,
          );
        }),
      (error) =>
        this.zone.run(() => {
          if (this.destroyed) return;
          this.locating.set(false);
          const messages: Record<number, string> = {
            1: 'Localização bloqueada. Permita o acesso à localização para este site no navegador e para o navegador nos Serviços de Localização do sistema. Depois tente novamente, ou busque um endereço.',
            2: 'O navegador não conseguiu determinar sua posição (posição indisponível). Verifique os Serviços de Localização do sistema e a conexão. Se estiver no navegador integrado, tente abrir este site no Chrome ou Safari, ou use a busca por endereço.',
            3: 'O navegador não encontrou sua posição em 30 segundos. Verifique os Serviços de Localização do sistema e tente novamente, ou use a busca por endereço.',
          };
          this.locationMessage.set(
            messages[error.code] ?? 'Falha ao obter a localização. Use a busca por endereço.',
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
          this.selectedArea.bindTooltip('Área climática · demonstração', {
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
    this.zone.run(() => this.placeSelected.emit(knownLabel || `Identificando local · ${fallback}`));
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
          this.placeSelected.emit(label ? `Próximo a ${label}` : `Local selecionado · ${fallback}`),
        );
      } catch {
        if (!this.destroyed && version === this.selectionVersion)
          this.zone.run(() => this.placeSelected.emit(`Local selecionado · ${fallback}`));
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
