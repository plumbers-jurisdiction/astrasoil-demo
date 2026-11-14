import { PlantsService } from './api/plants.service';
import { Component, inject } from '@angular/core';

import { FieldMapComponent } from './map/field-map.component';

import { RotationPlan } from './rotations/rotation.model';
import { demoRotations } from './rotations/rotation.mock';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [FieldMapComponent],
  templateUrl: './app.component.html',
})
export class AppComponent {
  readonly currentYear = Number(
    new Intl.DateTimeFormat('en', { year: 'numeric', timeZone: 'America/Sao_Paulo' }).format(
      new Date(),
    ),
  );
  selectedPlace = 'Selecione um local no mapa';
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
  menuOpen = false;
  // Replace this assignment with response.rotations when the API is connected.
  rotations: RotationPlan[] = demoRotations(this.currentYear + 1).rotations;

  toggleMenu(): void {
    this.menuOpen = !this.menuOpen;
  }
  closeMenu(): void {
    this.menuOpen = false;
  }
  scrollToStart(): void {
    document.getElementById('comecar')?.scrollIntoView({ behavior: 'smooth' });
  }
}
