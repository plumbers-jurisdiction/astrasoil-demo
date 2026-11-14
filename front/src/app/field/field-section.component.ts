import { RotationRequest } from '../rotations/rotation.model';
import { Component, inject, DestroyRef, signal, output, input } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
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
  readonly generate = output<RotationRequest>();
  readonly generating = input(false);
  point: { latitude: number; longitude: number } | null = null;
  formError = '';
  selectedPlace = 'Select a location on the map';
  readonly maxPlantsPerYear = 4;
  plantOptions: string[] = [];
  readonly plantsLoading = signal(false);
  readonly plantsError = signal('');
  private readonly plantsService = inject(PlantsService);
  private readonly destroyRef = inject(DestroyRef);
  constructor() {
    this.loadPlants();
  }
  loadPlants(): void {
    if (this.plantsLoading()) return;
    this.plantsLoading.set(true);
    this.plantsError.set('');
    this.plantsService
      .getPlants()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (plants) => {
          // Bare soil is a field condition, not a crop supplied by the API.
          this.plantOptions = [...new Set([...plants, 'Bare soil'])];
          this.plantsLoading.set(false);
        },
        error: () => {
          this.plantsLoading.set(false);
          this.plantsError.set('Could not load the crop list. Please try again.');
        },
      });
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
    this.formError = '';
    if (this.generating()) return;
    if (!this.point) {
      this.formError = 'Select a location on the map before generating rotations.';
      return;
    }
    if (
      this.plantsLoading() ||
      this.plantsError() ||
      this.harvests.some((row) => row.plants.some((p) => !p.trim()))
    ) {
      this.formError = 'Select a crop for every field.';
      return;
    }
    this.generate.emit({
      ...this.point,
      history: this.harvests.map((row) => ({ year: row.year, plants: [...row.plants] })),
    });
    this.historySubmitted = true;
  }
}
