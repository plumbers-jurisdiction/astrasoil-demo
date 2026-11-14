import { Component, inject } from '@angular/core';
import { PlantsService } from '../api/plants.service';
import { FieldMapComponent } from '../map/field-map.component';
@Component({
  selector: 'app-field-section',
  standalone: true,
  imports: [FieldMapComponent],
  templateUrl: './field-section.component.html',
  styles: [':host { display: block; }'],
})
export class FieldSectionComponent {
  readonly currentYear = Number(
    new Intl.DateTimeFormat('en', { year: 'numeric', timeZone: 'America/Sao_Paulo' }).format(
      new Date(),
    ),
  );
  selectedPlace = 'Select a location on the map';
  readonly maxPlantsPerYear = 4;
  plantOptions: string[] = [];
  constructor() {
    inject(PlantsService)
      .getPlants()
      .subscribe((plants) => (this.plantOptions = plants));
  }
  harvests = [{ year: this.currentYear - 1, plants: [''] }];
  historySubmitted = false;

  addYear(): void {
    if (this.harvests.length >= 8) return;
    const year = Array.from({ length: 8 }, (_, index) => this.currentYear - 1 - index).find(
      (candidate) => !this.harvests.some((row) => row.year === candidate),
    );
    if (year === undefined) return;
    this.harvests.push({ year, plants: [''] });
    this.harvests.sort((a, b) => b.year - a.year);
    this.historySubmitted = false;
  }
  removeYear(year: number): void {
    if (this.harvests.length <= 1) return;
    this.harvests = this.harvests.filter((row) => row.year !== year);
    this.historySubmitted = false;
  }
  addPlant(row: { plants: string[] }): void {
    if (row.plants.length >= this.maxPlantsPerYear) return;
    row.plants.push('');
    this.historySubmitted = false;
  }
  setPlant(row: { plants: string[] }, index: number, event: Event): void {
    row.plants[index] = (event.target as HTMLSelectElement).value;
    this.historySubmitted = false;
  }
  removePlant(row: { plants: string[] }, index: number): void {
    row.plants.splice(index, 1);
    this.historySubmitted = false;
  }
  generateRotations(event: Event): void {
    event.preventDefault();
    this.historySubmitted = true;
  }
}
