import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subscription } from 'rxjs';
import { RotationStrategiesService } from './api/rotation-strategies.service';
import { RotationPlan, RotationRequest } from './rotations/rotation.model';
import { Component, inject, DestroyRef } from '@angular/core';
import { FieldSectionComponent } from './field/field-section.component';
import { RotationSectionComponent } from './rotations/rotation-section.component';
@Component({
  selector: 'app-root',
  standalone: true,
  imports: [FieldSectionComponent, RotationSectionComponent],
  templateUrl: './app.component.html',
})
export class AppComponent {
  rotations: RotationPlan[] = [];
  loading = false;
  rotationError = '';
  attempted = false;
  private readonly service = inject(RotationStrategiesService);
  private readonly destroyRef = inject(DestroyRef);
  private request?: Subscription;
  generateRotations(payload: RotationRequest): void {
    this.request?.unsubscribe();
    this.loading = true;
    this.rotationError = '';
    this.rotations = [];
    this.attempted = true;
    this.request = this.service
      .generate(payload)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (plans) => {
          this.rotations = plans;
          this.loading = false;
        },
        error: () => {
          this.loading = false;
          this.rotationError = 'Could not generate rotation strategies. Please try again.';
        },
      });
  }
  menuOpen = false;
  toggleMenu(): void {
    this.menuOpen = !this.menuOpen;
  }
}
