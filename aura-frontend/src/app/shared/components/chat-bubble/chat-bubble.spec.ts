import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ChatBubble } from './chat-bubble';
import { Message } from '../../../core/models/message.model';

const userMessage: Message = {
  id: 1,
  author: 'USER',
  content: 'Hello',
  timestamp: 'now'
};

const assistantMessage: Message = {
  id: 2,
  author: 'ASSISTANT',
  content: 'Hi',
  timestamp: 'now'
};

describe('ChatBubble', () => {
  let fixture: ComponentFixture<ChatBubble>;
  let component: ChatBubble;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ChatBubble]
    }).compileComponents();

    fixture = TestBed.createComponent(ChatBubble);
    component = fixture.componentInstance;
  });

  it('marks host as user and renders user avatar', () => {
    // Arrange
    component.message = userMessage;

    // Act
    fixture.detectChanges();

    // Assert
    expect(fixture.nativeElement.classList.contains('user')).toBeTrue();
    expect(fixture.nativeElement.querySelector('app-user-avatar')).toBeTruthy();
  });

  it('marks host as assistant and renders orb icon', () => {
    // Arrange
    component.message = assistantMessage;

    // Act
    fixture.detectChanges();

    // Assert
    expect(fixture.nativeElement.classList.contains('assistant')).toBeTrue();
    expect(fixture.nativeElement.querySelector('app-orb-icon')).toBeTruthy();
  });

  it('renders attachment cards above the message bubble', () => {
    // Arrange
    component.message = {
      ...userMessage,
      attachments: [{ name: 'guide.pdf', size: 1200, type: 'application/pdf' }]
    };

    // Act
    fixture.detectChanges();

    // Assert
    const stack = fixture.nativeElement.querySelector('.message-stack') as HTMLElement;
    const attachment = fixture.nativeElement.querySelector('.attachment-card') as HTMLElement;
    const bubble = fixture.nativeElement.querySelector('.bubble') as HTMLElement;
    expect(attachment).toBeTruthy();
    expect(attachment.textContent).toContain('guide.pdf');
    expect(stack.firstElementChild).toBe(fixture.nativeElement.querySelector('.attachments'));
    expect(stack.lastElementChild).toBe(bubble);
  });

  it('does not render attachments when none exist', () => {
    // Arrange
    component.message = userMessage;

    // Act
    fixture.detectChanges();

    // Assert
    expect(fixture.nativeElement.querySelector('.attachments')).toBeNull();
  });
});
