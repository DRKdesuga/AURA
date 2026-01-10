import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TextInput } from './text-input';

describe('TextInput', () => {
  let fixture: ComponentFixture<TextInput>;
  let component: TextInput;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TextInput]
    }).compileComponents();

    fixture = TestBed.createComponent(TextInput);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('emits valueChange on input', () => {
    // Arrange
    let emitted: string | undefined;
    component.valueChange.subscribe(value => (emitted = value));

    const input = fixture.nativeElement.querySelector('input') as HTMLInputElement;

    // Act
    input.value = 'hello';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    // Assert
    expect(emitted).toBe('hello');
  });

  it('emits enter when pressing Enter', () => {
    // Arrange
    let emitted = false;
    component.enter.subscribe(() => (emitted = true));

    const input = fixture.nativeElement.querySelector('input') as HTMLInputElement;

    // Act
    input.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter' }));
    fixture.detectChanges();

    // Assert
    expect(emitted).toBeTrue();
  });
});
