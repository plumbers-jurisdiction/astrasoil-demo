import { of } from 'rxjs';
import { RotationStrategiesService } from './api/rotation-strategies.service';
import { RotationRequest, RotationPlan } from './rotations/rotation.model';
import { Component, input, output } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { AppComponent } from './app.component';
import { FieldSectionComponent } from './field/field-section.component';
import { RotationSectionComponent } from './rotations/rotation-section.component';
@Component({
  selector: 'app-field-section',
  standalone: true,
  templateUrl: './testing/empty-stub.html',
})
class FieldStub {
  generating = input(false);
  generate = output<RotationRequest>();
}
@Component({
  selector: 'app-rotation-section',
  standalone: true,
  templateUrl: './testing/empty-stub.html',
})
class RotationStub {
  rotations = input<RotationPlan[]>([]);
  loading = input(false);
  error = input('');
  attempted = input(false);
}
describe('AppComponent', () => {
  it('composes the planning sections with header and footer', () => {
    TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [
        { provide: RotationStrategiesService, useValue: { generate: jest.fn(() => of([])) } },
      ],
    }).overrideComponent(AppComponent, {
      remove: { imports: [FieldSectionComponent, RotationSectionComponent] },
      add: { imports: [FieldStub, RotationStub] },
    });
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    expect(fixture.componentInstance.rotations).toEqual([]);
    const service = TestBed.inject(RotationStrategiesService);
    const plans: RotationPlan[] = [
      {
        title: 'API result',
        years: [{ year: 2027, plant: 'Corn' }],
        justifications: [],
        risks: [],
        reviewPoints: [],
      },
    ];
    jest.spyOn(service, 'generate').mockReturnValue(of(plans));
    fixture.componentInstance.generateRotations({
      latitude: 42,
      longitude: -93.6,
      history: [{ year: 2025, plants: ['Corn'] }],
    });
    fixture.detectChanges();
    expect(service.generate).toHaveBeenCalledWith({
      latitude: 42,
      longitude: -93.6,
      history: [{ year: 2025, plants: ['Corn'] }],
    });
    expect(fixture.componentInstance.rotations).toEqual(plans);
    for (const selector of [
      'app-field-section',
      'app-rotation-section',
      '.site-header',
      '.site-footer',
    ])
      expect(fixture.nativeElement.querySelector(selector)).not.toBeNull();
  });
});
