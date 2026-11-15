import { PlantsService } from '../api/plants.service';
import { of, throwError } from 'rxjs';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Component, input, output } from '@angular/core';
import { FieldSectionComponent } from './field-section.component';
import { FieldMapComponent } from '../map/field-map.component';

@Component({
  selector: 'app-field-map',
  standalone: true,
  templateUrl: '../testing/projected-content-stub.html',
})
class MapStub {
  selectedPlace = input('');
  placeSelected = output<string>();
  pointSelected = output<{ latitude: number; longitude: number }>();
}

describe('FieldSectionComponent', () => {
  let fixture: ComponentFixture<FieldSectionComponent>;
  let component: FieldSectionComponent;
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [FieldSectionComponent],
      providers: [
        { provide: PlantsService, useValue: { getPlants: jest.fn(() => of(['Corn', 'Soybean'])) } },
      ],
    }).overrideComponent(FieldSectionComponent, {
      remove: { imports: [FieldMapComponent] },
      add: { imports: [MapStub] },
    });
    fixture = TestBed.createComponent(FieldSectionComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });
  it('requires a selected point and emits the exact rotation request without sharing mutable history', () => {
    const emit = jest.fn();
    component.generate.subscribe(emit);
    component.generateRotations(new Event('submit'));
    expect(emit).not.toHaveBeenCalled();
    expect(component.formError).toContain('Select a location');
    component.point = { latitude: 42, longitude: -93.6 };
    component.harvests = [
      { year: 2026, plants: ['Wheat', 'Soybean'] },
      { year: 2025, plants: ['Corn', 'Cereal rye'] },
    ];
    component.generateRotations(new Event('submit'));
    expect(emit).toHaveBeenCalledWith({
      latitude: 42,
      longitude: -93.6,
      history: [
        { year: 2026, plants: ['Wheat', 'Soybean'] },
        { year: 2025, plants: ['Corn', 'Cereal rye'] },
      ],
    });
    component.harvests[0]!.plants.push('Corn');
    expect(emit.mock.calls[0][0].history[0].plants).toHaveLength(2);
  });
  it('keeps Bare soil only when returned by the backend', () => {
    jest
      .spyOn(TestBed.inject(PlantsService), 'getPlants')
      .mockReturnValue(of(['Corn', 'Bare soil']));
    component.loadPlants();
    expect(component.plantOptions).toEqual(['Corn', 'Bare soil']);
  });
  it('loads catalog and starts with previous year', () => {
    expect(component.plantOptions).toEqual(['Corn', 'Soybean']);
    expect(component.harvests).toEqual([{ year: component.currentYear - 1, plants: [''] }]);
  });
  it('shows API failures and retries without silently substituting a crop catalog', () => {
    const service = TestBed.inject(PlantsService);
    jest
      .spyOn(service, 'getPlants')
      .mockReturnValueOnce(throwError(() => new Error('offline')))
      .mockReturnValueOnce(of(['Wheat']));
    component.loadPlants();
    expect(component.plantsError()).toContain('Could not');
    component.loadPlants();
    expect(component.plantsError()).toBe('');
    expect(component.plantOptions).toEqual(['Wheat']);
  });
  it('limits history to eight years and restores gaps without duplicates or data loss', () => {
    for (let i = 0; i < 10; i++) component.addYear();
    expect(component.harvests).toHaveLength(8);
    component.harvests[0]!.plants = ['Corn'];
    const removed = component.currentYear - 3;
    component.removeYear(removed);
    component.addYear();
    expect(component.harvests.map((r) => r.year)).toEqual(
      Array.from({ length: 8 }, (_, i) => component.currentYear - 1 - i),
    );
    expect(component.harvests[0]!.plants).toEqual(['Corn']);
  });
  it('keeps at least one year and disables its deletion in the template', () => {
    component.removeYear(component.harvests[0]!.year);
    expect(component.harvests).toHaveLength(1);
    expect(fixture.nativeElement.querySelector('.remove-year').disabled).toBe(true);
  });
  it('limits each year to four plants and re-enables adding after removal', () => {
    const row = component.harvests[0]!;
    for (let i = 0; i < 6; i++) {
      row.plants[row.plants.length - 1] = 'Corn';
      component.addPlant(row);
    }
    fixture.detectChanges();
    expect(row.plants).toHaveLength(4);
    expect(fixture.nativeElement.querySelector('.add-plant').disabled).toBe(true);
    component.removePlant(row, 2);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.add-plant').disabled).toBe(false);
    component.addPlant(row);
    expect(row.plants).toHaveLength(4);
  });
  it('allows submitting no crop history and does not require empty selects', () => {
    component.point = { latitude: 42, longitude: -93.6 };
    const emit = jest.fn();
    component.generate.subscribe(emit);
    expect(fixture.nativeElement.querySelector('select').required).toBe(false);
    expect(fixture.nativeElement.querySelector('.generate-rotations').disabled).toBe(false);
    component.generateRotations(new Event('submit'));
    expect(emit).toHaveBeenCalledWith({ ...component.point, history: [] });
  });
  it('requires the current plant before adding another, and blocks incomplete history', () => {
    const row = component.harvests[0]!;
    component.addPlant(row);
    expect(row.plants).toEqual(['']);
    row.plants[0] = 'Corn';
    component.addPlant(row);
    component.addPlant(row);
    expect(row.plants).toEqual(['Corn', '']);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.add-plant').disabled).toBe(true);
    expect(fixture.nativeElement.querySelector('select').required).toBe(true);
    component.point = { latitude: 42, longitude: -93.6 };
    const emit = jest.fn();
    component.generate.subscribe(emit);
    component.generateRotations(new Event('submit'));
    expect(emit).not.toHaveBeenCalled();
    expect(component.formError).toContain('Select a crop');
    row.plants[1] = 'Soybean';
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.add-plant').disabled).toBe(false);
    component.generateRotations(new Event('submit'));
    expect(emit).toHaveBeenCalled();
  });
  it('clears the only crop while keeping its select visible', () => {
    component.harvests[0]!.plants = ['Corn'];
    fixture.detectChanges();
    fixture.nativeElement.querySelector('.remove-plant').click();
    fixture.detectChanges();
    expect(component.harvests[0]!.plants).toEqual(['']);
    expect(component.hasHistory).toBe(false);
    expect(fixture.nativeElement.querySelectorAll('select')).toHaveLength(1);
    expect(fixture.nativeElement.querySelector('select').value).toBe('');
    expect(fixture.nativeElement.querySelector('.add-plant').disabled).toBe(true);
    expect(fixture.nativeElement.querySelector('.remove-plant')).not.toBeNull();
  });
  it('records select changes and clears stale submission state after edits', () => {
    const row = component.harvests[0]!;
    const select = fixture.nativeElement.querySelector('select') as HTMLSelectElement;
    select.value = 'Corn';
    select.dispatchEvent(new Event('change'));
    expect(row.plants[0]).toBe('Corn');
    const event = new Event('submit', { cancelable: true });
    component.point = { latitude: 42, longitude: -93.6 };
    component.generateRotations(event);
    expect(event.defaultPrevented).toBe(true);
    expect(component.historySubmitted).toBe(true);
    component.addYear();
    expect(component.historySubmitted).toBe(false);
  });
  it('propagates the selected place to the desktop pill', () => {
    component.selectedPlace = 'Campinas – São Paulo';
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.desktop-location-pill').textContent).toContain(
      'Campinas',
    );
  });
});
