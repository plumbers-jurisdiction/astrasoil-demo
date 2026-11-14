import { TestBed } from '@angular/core/testing';
import { RotationSectionComponent } from './rotation-section.component';
describe('RotationSectionComponent', () => {
  it('renders cards and handles empty plans', () => {
    const fixture = TestBed.createComponent(RotationSectionComponent);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelectorAll('.rotation-card')).toHaveLength(2);
    expect(fixture.nativeElement.querySelectorAll('.crop-photo')).toHaveLength(6);
    fixture.componentInstance.rotations = [];
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.rotation-empty')).not.toBeNull();
    expect(fixture.nativeElement.querySelectorAll('.rotation-card')).toHaveLength(0);
  });
});
