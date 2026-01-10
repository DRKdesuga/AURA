import { ComponentFixture, TestBed } from '@angular/core/testing';
import { UiButton } from './ui-button';

describe('UiButton', () => {
  let fixture: ComponentFixture<UiButton>;
  let component: UiButton;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UiButton]
    }).compileComponents();

    fixture = TestBed.createComponent(UiButton);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('applies ghost class when variant is ghost', () => {
    // Arrange
    component.variant = 'ghost';

    // Act
    fixture.detectChanges();

    // Assert
    const button = fixture.nativeElement.querySelector('button') as HTMLButtonElement;
    expect(button.classList.contains('ghost')).toBeTrue();
  });

  it('disables the button when disabled input is true', () => {
    // Arrange
    component.disabled = true;

    // Act
    fixture.detectChanges();

    // Assert
    const button = fixture.nativeElement.querySelector('button') as HTMLButtonElement;
    expect(button.disabled).toBeTrue();
  });
});
