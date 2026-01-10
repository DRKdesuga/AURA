import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ChatThread } from './chat-thread';
import { Message } from '../../../core/models/message.model';

const messages: Message[] = [
  { id: 1, author: 'USER', content: 'Hello', timestamp: 'now' },
  { id: 2, author: 'ASSISTANT', content: 'Hi', timestamp: 'now' }
];

describe('ChatThread', () => {
  let fixture: ComponentFixture<ChatThread>;
  let component: ChatThread;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ChatThread]
    }).compileComponents();

    fixture = TestBed.createComponent(ChatThread);
    component = fixture.componentInstance;
  });

  it('shows empty state when there are no messages', () => {
    // Arrange
    component.messages = [];

    // Act
    fixture.detectChanges();

    // Assert
    expect(fixture.nativeElement.querySelector('.empty')).toBeTruthy();
  });

  it('renders message bubbles when messages exist', () => {
    // Arrange
    component.messages = messages;

    // Act
    fixture.detectChanges();

    // Assert
    const bubbles = fixture.nativeElement.querySelectorAll('app-chat-bubble');
    expect(bubbles.length).toBe(2);
  });

  it('shows typing indicator when typing is true', () => {
    // Arrange
    component.messages = messages;
    component.typing = true;

    // Act
    fixture.detectChanges();

    // Assert
    expect(fixture.nativeElement.querySelector('.typing')).toBeTruthy();
  });
});
