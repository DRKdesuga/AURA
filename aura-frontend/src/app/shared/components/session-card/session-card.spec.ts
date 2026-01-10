import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SessionCard } from './session-card';
import { SessionSummary } from '../../../core/models/session-summary.model';

describe('SessionCard', () => {
  let fixture: ComponentFixture<SessionCard>;
  let component: SessionCard;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SessionCard]
    }).compileComponents();

    fixture = TestBed.createComponent(SessionCard);
    component = fixture.componentInstance;
  });

  it('renders fallback text when title and preview are missing', () => {
    // Arrange
    const data: SessionSummary = {
      sessionId: 1,
      title: null,
      preview: null,
      lastMessageAt: null,
      messageCount: 0
    };
    component.data = data;

    // Act
    fixture.detectChanges();

    // Assert
    const title = fixture.nativeElement.querySelector('.title') as HTMLElement;
    const preview = fixture.nativeElement.querySelector('.preview') as HTMLElement;
    expect(title.textContent).toContain('New chat');
    expect(preview.textContent).toContain('No messages yet');
  });

  it('emits select on click', () => {
    // Arrange
    const data: SessionSummary = {
      sessionId: 2,
      title: 'Hello',
      preview: 'First message',
      lastMessageAt: new Date().toISOString(),
      messageCount: 1
    };
    component.data = data;

    let emitted = false;
    component.select.subscribe(() => (emitted = true));

    // Act
    fixture.detectChanges();
    const card = fixture.nativeElement.querySelector('.card') as HTMLElement;
    card.click();

    // Assert
    expect(emitted).toBeTrue();
  });
});
