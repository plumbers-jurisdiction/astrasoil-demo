import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { AppComponent } from './app.component';
import { FieldSectionComponent } from './field/field-section.component';
import { RotationSectionComponent } from './rotations/rotation-section.component';
@Component({
  selector: 'app-field-section',
  standalone: true,
  templateUrl: './testing/empty-stub.html',
})
class FieldStub {}
@Component({
  selector: 'app-rotation-section',
  standalone: true,
  templateUrl: './testing/empty-stub.html',
})
class RotationStub {}
describe('AppComponent', () => {
  it('composes sections with header and footer and toggles the menu', () => {
    TestBed.configureTestingModule({ imports: [AppComponent] }).overrideComponent(AppComponent, {
      remove: { imports: [FieldSectionComponent, RotationSectionComponent] },
      add: { imports: [FieldStub, RotationStub] },
    });
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    for (const selector of [
      'app-field-section',
      'app-rotation-section',
      '.site-header',
      '.site-footer',
    ])
      expect(fixture.nativeElement.querySelector(selector)).not.toBeNull();
    fixture.nativeElement.querySelector('.mobile-menu').click();
    fixture.detectChanges();
    expect(fixture.componentInstance.menuOpen).toBe(true);
    expect(fixture.nativeElement.querySelector('.mobile-menu').getAttribute('aria-label')).toBe(
      'Close menu',
    );
  });
});
