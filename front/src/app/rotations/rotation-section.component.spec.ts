import { TestBed } from '@angular/core/testing';
import { RotationSectionComponent } from './rotation-section.component';
describe('RotationSectionComponent', () => {
  it('renders cover name/reason between years and centers connectors in their own row', () => {
    const f = TestBed.createComponent(RotationSectionComponent);
    f.componentInstance.rotations = [
      {
        title: 'Healthier soil',
        years: [
          {
            year: 2027,
            plant: 'Soybean',
            coverCrop: { plant: 'Hairy vetch', reason: 'Leaves nitrogen.' },
          },
          { year: 2028, plant: 'Corn', coverCrop: null },
        ],
        justifications: ['Diversity'],
        risks: [],
        reviewPoints: ['Review sowing'],
      },
    ];
    f.detectChanges();
    expect(f.nativeElement.querySelectorAll('.strategy-year')).toHaveLength(2);
    expect(f.nativeElement.querySelectorAll('.cover-circle')).toHaveLength(1);
    expect(f.nativeElement.querySelectorAll('.cover-arrow')).toHaveLength(2);
    expect(f.nativeElement.querySelector('.strategy-cover').textContent).toContain(
      'Leaves nitrogen.',
    );
  });
  it.each([null, undefined, {}, { plant: '', reason: '' }, { plant: '  ', reason: 'ignored' }])(
    'hides empty cover and arrows: %s',
    (cover) => {
      const f = TestBed.createComponent(RotationSectionComponent);
      f.componentInstance.rotations = [
        {
          title: 'No cover',
          years: [{ year: 2027, plant: 'Wheat', coverCrop: cover as never }],
          justifications: [],
          risks: [],
          reviewPoints: [],
        },
      ];
      f.detectChanges();
      expect(f.nativeElement.querySelector('.strategy-cover')).toBeNull();
      expect(f.nativeElement.querySelector('.cover-arrow')).toBeNull();
      expect(f.nativeElement.querySelector('.crop-photo').getAttribute('src')).toContain(
        '/wheat.png',
      );
    },
  );
  it('builds Blob URLs using lower-case hyphenated plant names', () => {
    const f = TestBed.createComponent(RotationSectionComponent);
    expect(f.componentInstance.imageFor('Alfalfa')).toBe(
      'https://zxphrpelolxfvxl8.public.blob.vercel-storage.com/alfalfa.png',
    );
    expect(f.componentInstance.imageFor('  Hairy   vetch  ')).toContain('/hairy-vetch.png');
    expect(f.componentInstance.imageFor('Dry bean')).toContain('/dry-bean.png');
  });
  it('renders Blob images for both main and cover crops and falls back to initials on failure', () => {
    const f = TestBed.createComponent(RotationSectionComponent);
    f.componentInstance.rotations = [
      {
        title: 'Plan',
        years: [
          { year: 2027, plant: 'Corn', coverCrop: { plant: 'Hairy vetch', reason: 'Cover' } },
        ],
        justifications: [],
        risks: [],
        reviewPoints: [],
      },
    ];
    f.detectChanges();
    const main = f.nativeElement.querySelector('.crop-photo'),
      cover = f.nativeElement.querySelector('.cover-photo');
    expect(main.getAttribute('src')).toContain('/corn.png');
    expect(cover.getAttribute('src')).toContain('/hairy-vetch.png');
    main.dispatchEvent(new Event('error'));
    cover.dispatchEvent(new Event('error'));
    f.detectChanges();
    expect(f.nativeElement.querySelector('.crop-photo')).toBeNull();
    expect(f.nativeElement.querySelector('.cover-photo')).toBeNull();
    expect(f.nativeElement.querySelector('.plant-initials').textContent).toBe('CO');
    expect(f.nativeElement.querySelector('.cover-circle').textContent.trim()).toBe('HA');
  });
  it('renders loading, error and empty states without demo plans', () => {
    const f = TestBed.createComponent(RotationSectionComponent);
    f.detectChanges();
    expect(f.nativeElement.querySelectorAll('.rotation-card')).toHaveLength(0);
    f.componentInstance.loading = true;
    f.detectChanges();
    expect(f.nativeElement.textContent).toContain('Generating');
    f.componentInstance.loading = false;
    f.componentInstance.error = 'Unavailable';
    f.detectChanges();
    expect(f.nativeElement.querySelector('[role=alert]').textContent).toBe('Unavailable');
  });
});
