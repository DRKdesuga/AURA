import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { of } from 'rxjs';
import { AuraShell } from './aura-shell';
import { ChatService } from '../../../core/services/chat/chat';
import { Message } from '../../../core/models/message.model';
import { SessionsService } from '../../../core/services/sessions/sessions';
import { UserPreferencesService } from '../../../core/services/user/user-preferences';
import { AuthService } from '../../../core/auth/auth.service';

const messages: Message[] = [
  { id: 1, author: 'USER', content: 'Hello', timestamp: 'now' }
];

describe('AuraShell', () => {
  let fixture: ComponentFixture<AuraShell>;
  let component: AuraShell;
  let chat: jasmine.SpyObj<ChatService>;

  beforeEach(async () => {
    chat = jasmine.createSpyObj<ChatService>('ChatService', ['getMessages', 'chat']);
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
    component.onSendMessage('Hello');

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
    component.onSendMessage('Queued');

    // Assert
    expect(chat.chat).not.toHaveBeenCalled();
    expect(component.messages().length).toBe(1);
  });
});
