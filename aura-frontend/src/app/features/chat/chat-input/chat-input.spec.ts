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
    let emitted: { message: string; file: File | null } | undefined;
    component.send.subscribe(value => (emitted = value));

    component.valueSig.set('  hello  ');
    fixture.detectChanges();

    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;

    // Act
    form.dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    // Assert
    expect(emitted).toEqual({ message: 'hello', file: null });
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

  it('shows the attachment button and triggers the file picker', () => {
    // Arrange
    const button = fixture.nativeElement.querySelector('button.attach') as HTMLButtonElement;
    const input = fixture.nativeElement.querySelector('input.file-input') as HTMLInputElement;
    spyOn(input, 'click');

    // Act
    button.click();

    // Assert
    expect(input.click).toHaveBeenCalled();
  });

  it('adds a PDF attachment and displays a chip', () => {
    // Arrange
    const file = new File(['%PDF-1.4'], 'guide.pdf', { type: 'application/pdf' });
    const input = fixture.nativeElement.querySelector('input.file-input') as HTMLInputElement;

    const dataTransfer = new DataTransfer();
    dataTransfer.items.add(file);
    Object.defineProperty(input, 'files', { value: dataTransfer.files });

    // Act
    input.dispatchEvent(new Event('change'));
    fixture.detectChanges();

    // Assert
    const chip = fixture.nativeElement.querySelector('.attachment-chip') as HTMLElement;
    expect(chip).toBeTruthy();
    expect(chip.textContent).toContain('guide.pdf');
  });

  it('removes an attachment when the remove button is clicked', () => {
    // Arrange
    const file = new File(['%PDF-1.4'], 'guide.pdf', { type: 'application/pdf' });
    const input = fixture.nativeElement.querySelector('input.file-input') as HTMLInputElement;

    const dataTransfer = new DataTransfer();
    dataTransfer.items.add(file);
    Object.defineProperty(input, 'files', { value: dataTransfer.files });
    input.dispatchEvent(new Event('change'));
    fixture.detectChanges();

    const remove = fixture.nativeElement.querySelector('.attachment-remove') as HTMLButtonElement;

    // Act
    remove.click();
    fixture.detectChanges();

    // Assert
    expect(fixture.nativeElement.querySelector('.attachment-chip')).toBeNull();
  });

  it('rejects non-PDF files and shows an error message', () => {
    // Arrange
    const file = new File(['Hello'], 'note.txt', { type: 'text/plain' });
    const input = fixture.nativeElement.querySelector('input.file-input') as HTMLInputElement;

    const dataTransfer = new DataTransfer();
    dataTransfer.items.add(file);
    Object.defineProperty(input, 'files', { value: dataTransfer.files });

    // Act
    input.dispatchEvent(new Event('change'));
    fixture.detectChanges();

    // Assert
    expect(fixture.nativeElement.querySelector('.attachment-chip')).toBeNull();
    const error = fixture.nativeElement.querySelector('.attachment-error') as HTMLElement;
    expect(error).toBeTruthy();
    expect(error.textContent).toContain('Only PDF');
  });
});
