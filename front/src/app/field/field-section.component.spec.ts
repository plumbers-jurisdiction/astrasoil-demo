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
}

describe('FieldSectionComponent', () => {
  let fixture: ComponentFixture<FieldSectionComponent>;
  let component: FieldSectionComponent;
  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [FieldSectionComponent] }).overrideComponent(
      FieldSectionComponent,
      {
        remove: { imports: [FieldMapComponent] },
        add: { imports: [MapStub] },
      },
    );
    fixture = TestBed.createComponent(FieldSectionComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });
  it('loads catalog and starts with previous year', () => {
    expect(component.plantOptions).toHaveLength(23);
    expect(component.harvests).toEqual([{ year: component.currentYear - 1, plants: [''] }]);
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
    for (let i = 0; i < 6; i++) component.addPlant(row);
    fixture.detectChanges();
    expect(row.plants).toHaveLength(4);
    expect(fixture.nativeElement.querySelector('.add-plant').disabled).toBe(true);
    component.removePlant(row, 2);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.add-plant').disabled).toBe(false);
    component.addPlant(row);
    expect(row.plants).toHaveLength(4);
  });
  it('records select changes and clears stale submission state after edits', () => {
    const row = component.harvests[0]!;
    const select = fixture.nativeElement.querySelector('select') as HTMLSelectElement;
    select.value = 'Corn';
    select.dispatchEvent(new Event('change'));
    expect(row.plants[0]).toBe('Corn');
    const event = new Event('submit', { cancelable: true });
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
