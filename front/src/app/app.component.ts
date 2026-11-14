import { Component } from '@angular/core';
import { FieldSectionComponent } from './field/field-section.component';
import { RotationSectionComponent } from './rotations/rotation-section.component';
@Component({
  selector: 'app-root',
  standalone: true,
  imports: [FieldSectionComponent, RotationSectionComponent],
  templateUrl: './app.component.html',
})
export class AppComponent {
  menuOpen = false;
  toggleMenu(): void {
    this.menuOpen = !this.menuOpen;
  }
}
