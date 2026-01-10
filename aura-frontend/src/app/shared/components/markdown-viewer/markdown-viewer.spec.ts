import { ComponentFixture, TestBed, fakeAsync, flushMicrotasks, tick } from '@angular/core/testing';
import { MarkdownViewer } from './markdown-viewer';

describe('MarkdownViewer', () => {
  let fixture: ComponentFixture<MarkdownViewer>;
  let component: MarkdownViewer;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MarkdownViewer]
    }).compileComponents();

    fixture = TestBed.createComponent(MarkdownViewer);
    component = fixture.componentInstance;
  });

  it('sanitizes unsafe html', fakeAsync(() => {
    // Arrange
    component.content = '<img src="x" onerror="alert(1)">';

    // Act
    fixture.detectChanges();
    flushMicrotasks();

    // Assert
    const root = fixture.nativeElement.querySelector('.md') as HTMLElement;
    expect(root.innerHTML.includes('onerror')).toBeFalse();
  }));

  it('adds copy button and copies code blocks', fakeAsync(() => {
    // Arrange
    const clipboard = { writeText: jasmine.createSpy('writeText').and.returnValue(Promise.resolve()) };
    Object.defineProperty(navigator, 'clipboard', { value: clipboard, configurable: true });

    component.content = '```ts\nconst x = 1;\n```';

    // Act
    fixture.detectChanges();
    flushMicrotasks();

    const button = fixture.nativeElement.querySelector('.copy-btn') as HTMLButtonElement;
    button.click();
    flushMicrotasks();
    tick(0);

    // Assert
    expect(clipboard.writeText).toHaveBeenCalledWith('const x = 1;');
    expect(button.classList.contains('ok')).toBeTrue();
  }));
});
