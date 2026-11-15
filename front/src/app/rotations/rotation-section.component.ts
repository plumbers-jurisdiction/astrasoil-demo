import { Component, Input, signal } from '@angular/core';
import { RotationPlan } from './rotation.model';
@Component({
  selector: 'app-rotation-section',
  standalone: true,
  templateUrl: './rotation-section.component.html',
  styles: [':host { display: block; }'],
})
export class RotationSectionComponent {
  @Input() rotations: RotationPlan[] = [];
  @Input() loading = false;
  @Input() error = '';
  @Input() attempted = false;
  private readonly imageBaseUrl = 'https://zxphrpelolxfvxl8.public.blob.vercel-storage.com';
  private readonly failedImages = signal(new Set<string>());
  private imageKey(plant: string): string {
    return plant.trim().toLowerCase().replace(/\s+/g, '-');
  }
  imageFor(plant: string): string | null {
    const key = this.imageKey(plant);
    return key && !this.failedImages().has(key)
      ? `${this.imageBaseUrl}/${encodeURIComponent(key)}.png`
      : null;
  }
  imageFailed(plant: string): void {
    this.failedImages.update((previous) => new Set([...previous, this.imageKey(plant)]));
  }
  coverFor(cover: { plant?: string; reason?: string } | null | undefined) {
    return cover?.plant?.trim() ? cover : null;
  }
}
