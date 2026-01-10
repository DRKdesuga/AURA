import { ComponentFixture, TestBed } from '@angular/core/testing';
import { OrbIcon } from './orb-icon';

describe('OrbIcon', () => {
  let fixture: ComponentFixture<OrbIcon>;
  let component: OrbIcon;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OrbIcon]
    }).compileComponents();

    fixture = TestBed.createComponent(OrbIcon);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('adds animate class when animate is true', () => {
    // Arrange
    component.animate = true;

    // Act
    fixture.detectChanges();

    // Assert
    expect(fixture.nativeElement.classList.contains('animate')).toBeTrue();
  });

  it('removes animate class when animate is false', () => {
    // Arrange
    component.animate = false;

    // Act
    fixture.detectChanges();

    // Assert
    expect(fixture.nativeElement.classList.contains('animate')).toBeFalse();
  });
});
