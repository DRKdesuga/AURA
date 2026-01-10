import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ChatInput } from './chat-input';

function flushAnimationFrames() {
  spyOn(window, 'requestAnimationFrame').and.callFake((cb: FrameRequestCallback) => {
    cb(0);
    return 0;
  });
}

describe('ChatInput', () => {
  let fixture: ComponentFixture<ChatInput>;
  let component: ChatInput;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ChatInput]
    }).compileComponents();

    fixture = TestBed.createComponent(ChatInput);
    component = fixture.componentInstance;
    flushAnimationFrames();
    fixture.detectChanges();
  });

  it('renders with the send button disabled when empty', () => {
    // Arrange
    const button = fixture.nativeElement.querySelector('button.send') as HTMLButtonElement;

    // Act
    fixture.detectChanges();

    // Assert
    expect(button.disabled).toBeTrue();
  });

  it('emits trimmed message on submit and clears input', () => {
    // Arrange
    let emitted: string | undefined;
    component.send.subscribe(value => (emitted = value));

    component.valueSig.set('  hello  ');
    fixture.detectChanges();

    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;

    // Act
    form.dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    // Assert
    expect(emitted).toBe('hello');
    expect(component.valueSig()).toBe('');
  });

  it('does not emit when disabled', () => {
    // Arrange
    let emitted = false;
    component.send.subscribe(() => (emitted = true));

    component.disabled = true;
    component.valueSig.set('hello');
    fixture.detectChanges();

    // Act
    component.onSubmit();
    fixture.detectChanges();

    // Assert
    expect(emitted).toBeFalse();
  });
});
