import { Component, Input } from '@angular/core';
import { RotationPlan } from './rotation.model';
import { CROP_IMAGES } from './crop-images';
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
  imageFor(plant: string) {
    const images: Record<string, typeof CROP_IMAGES.soja> = {
      Soybean: CROP_IMAGES.soja,
      Corn: CROP_IMAGES.milho,
      'Dry bean': CROP_IMAGES.feijao,
      Sorghum: CROP_IMAGES.sorgo,
    };
    return images[plant];
  }
  coverFor(cover: { plant?: string; reason?: string } | null | undefined) {
    return cover?.plant?.trim() ? cover : null;
  }
}
