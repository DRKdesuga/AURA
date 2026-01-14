import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { By } from '@angular/platform-browser';
import { of } from 'rxjs';
import { AuraShell } from './aura-shell';
import { ChatService } from '../../../core/services/chat/chat';
import { Message } from '../../../core/models/message.model';
import { SessionsService } from '../../../core/services/sessions/sessions';
import { UserPreferencesService } from '../../../core/services/user/user-preferences';
import { AuthService } from '../../../core/auth/auth.service';
import { ChatInput } from '../../chat/chat-input/chat-input';

const messages: Message[] = [
  { id: 1, author: 'USER', content: 'Hello', timestamp: 'now' }
];

describe('AuraShell', () => {
  let fixture: ComponentFixture<AuraShell>;
  let component: AuraShell;
  let chat: jasmine.SpyObj<ChatService>;

  beforeEach(async () => {
    chat = jasmine.createSpyObj<ChatService>('ChatService', ['getMessages', 'chat', 'chatWithFile']);
    const sessions = jasmine.createSpyObj<SessionsService>('SessionsService', ['list']);
    sessions.list.and.returnValue(of({ items: [], total: 0, page: 0, size: 0 }));

    const prefs = { avatarUrl: signal('assets/user/default-avatar.png'), auraColor: signal('#6f9dff') };
    const auth = jasmine.createSpyObj<AuthService>('AuthService', ['logout']);
    auth.logout.and.returnValue(of(void 0));

    await TestBed.configureTestingModule({
      imports: [AuraShell],
      providers: [
        { provide: ChatService, useValue: chat },
        { provide: SessionsService, useValue: sessions },
        { provide: UserPreferencesService, useValue: prefs },
        { provide: AuthService, useValue: auth }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AuraShell);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('loads messages when a session is selected', () => {
    // Arrange
    chat.getMessages.and.returnValue(of(messages));

    // Act
    component.onSelectSession(5);

    // Assert
    expect(chat.getMessages).toHaveBeenCalledWith(5);
    expect(component.sessionId()).toBe(5);
    expect(component.messages().length).toBe(1);
  });

  it('sends a new message and appends assistant reply', () => {
    // Arrange
    const response = {
      sessionId: 10,
      userMessageId: 10,
      assistantMessageId: 99,
      assistantReply: 'Hi',
      timestamp: 'now',
      newSession: true
    };
    chat.chat.and.returnValue(of(response));
    component.sidebar = { refresh: jasmine.createSpy('refresh') } as any;

    // Act
    component.onSendMessage({ message: 'Hello', file: null });

    // Assert
    expect(chat.chat).toHaveBeenCalledWith({ sessionId: null, message: 'Hello' });
    expect(component.messages().length).toBe(2);
    expect(component.messages()[1].author).toBe('ASSISTANT');
    expect(component.sidebar?.refresh).toHaveBeenCalled();
    expect(component.typing()).toBeFalse();
  });

  it('queues message when already typing', () => {
    // Arrange
    component.typing.set(true);

    // Act
    component.onSendMessage({ message: 'Queued', file: null });

    // Assert
    expect(chat.chat).not.toHaveBeenCalled();
    expect(component.messages().length).toBe(1);
  });

  it('sends a message with a file using chatWithFile and clears the draft', () => {
    // Arrange
    const response = {
      sessionId: 4,
      userMessageId: 10,
      assistantMessageId: 11,
      assistantReply: 'Uploaded',
      timestamp: 'now',
      newSession: false
    };
    chat.chatWithFile.and.returnValue(of(response));

    const input = fixture.debugElement.query(By.directive(ChatInput)).componentInstance as ChatInput;
    spyOn(input, 'clearDraft').and.callThrough();

    const file = new File(['%PDF-1.4'], 'guide.pdf', { type: 'application/pdf' });
    input.attachments.set([file]);

    // Act
    component.onSendMessage({ message: 'See this', file });

    // Assert
    expect(chat.chatWithFile).toHaveBeenCalledWith({ sessionId: null, message: 'See this', file });
    expect(input.clearDraft).toHaveBeenCalled();
    expect(input.attachments().length).toBe(0);
    const userMessage = component.messages().find(item => item.author === 'USER');
    expect(userMessage?.attachments?.[0].name).toBe('guide.pdf');
  });

  it('accepts a dropped PDF and shows the attachment chip', () => {
    // Arrange
    const file = new File(['%PDF-1.4'], 'dropped.pdf', { type: 'application/pdf' });
    const dataTransfer = new DataTransfer();
    dataTransfer.items.add(file);

    const event = new Event('drop') as DragEvent;
    Object.defineProperty(event, 'dataTransfer', { value: dataTransfer });

    const main = fixture.nativeElement.querySelector('main.main') as HTMLElement;

    // Act
    main.dispatchEvent(event);
    fixture.detectChanges();

    // Assert
    const chip = fixture.nativeElement.querySelector('.attachment-chip') as HTMLElement;
    expect(chip).toBeTruthy();
    expect(chip.textContent).toContain('dropped.pdf');
  });

  it('rejects a dropped non-PDF file with an error message', () => {
    // Arrange
    const file = new File(['Hello'], 'note.txt', { type: 'text/plain' });
    const dataTransfer = new DataTransfer();
    dataTransfer.items.add(file);

    const event = new Event('drop') as DragEvent;
    Object.defineProperty(event, 'dataTransfer', { value: dataTransfer });

    const main = fixture.nativeElement.querySelector('main.main') as HTMLElement;

    // Act
    main.dispatchEvent(event);
    fixture.detectChanges();

    // Assert
    expect(fixture.nativeElement.querySelector('.attachment-chip')).toBeNull();
    const error = fixture.nativeElement.querySelector('.attachment-error') as HTMLElement;
    expect(error).toBeTruthy();
    expect(error.textContent).toContain('Only PDF');
  });
});
