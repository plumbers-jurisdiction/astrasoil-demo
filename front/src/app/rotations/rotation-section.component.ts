import { Component } from '@angular/core';
import { RotationPlan } from './rotation.model';
import { demoRotations } from './rotation.mock';
@Component({
  selector: 'app-rotation-section',
  standalone: true,
  templateUrl: './rotation-section.component.html',
  styles: [':host { display: block; }'],
})
export class RotationSectionComponent {
  readonly currentYear = Number(
    new Intl.DateTimeFormat('en', { year: 'numeric', timeZone: 'America/Sao_Paulo' }).format(
      new Date(),
    ),
  );
  rotations: RotationPlan[] = demoRotations(this.currentYear + 1).rotations;
}
