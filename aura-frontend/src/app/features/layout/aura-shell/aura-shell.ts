import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SessionsSidebar } from '../../sidebar/sessions-sidebar/sessions-sidebar';
import { ChatHeader } from '../../chat/chat-header/chat-header';
import { ChatThread } from '../../chat/chat-thread/chat-thread';
import { ChatInput } from '../../chat/chat-input/chat-input';
import { Message } from '../../../core/models/message.model';
import { ChatService } from '../../../core/services/chat/chat';

@Component({
  selector: 'app-aura-shell',
  standalone: true,
  imports: [CommonModule, SessionsSidebar, ChatHeader, ChatThread, ChatInput],
  templateUrl: './aura-shell.html',
  styleUrl: './aura-shell.scss'
})
export class AuraShell {
  sessionId = signal<number | null>(null);
  messages = signal<Message[]>([]);
  typing = signal(false);

  private queue: string[] = [];

  constructor(private readonly chat: ChatService) {}

  onSelectSession(id: number) {
    this.sessionId.set(id);
    this.typing.set(false);
    this.queue = [];
    this.chat.getMessages(id).subscribe({
      next: list => this.messages.set(list)
    });
  }

  onNewSession(id: number) {
    this.sessionId.set(id);
    this.messages.set([]);
    this.typing.set(false);
    this.queue = [];
  }

  onSendMessage(text: string) {
    // 1) Append user bubble immediately
    const ts = new Date().toISOString();
    const tempId = Date.now();
    this.messages.update(arr => arr.concat([{ id: tempId, author: 'USER', content: text, timestamp: ts }]));

    // 2) If already generating, queue the text (assistant typing shown)
    if (this.typing()) {
      this.queue.push(text);
      return;
    }

    // 3) Start request with current (possibly null) sessionId
    this.typing.set(true);
    this.chat.chat({ sessionId: this.sessionId(), message: text }).subscribe({
      next: res => {
        if (this.sessionId() == null) this.sessionId.set(res.sessionId);
        this.messages.update(arr => arr.concat([
          { id: res.assistantMessageId, author: 'ASSISTANT', content: res.assistantReply, timestamp: res.timestamp }
        ]));
        this.processQueue();
      },
      error: () => {
        this.typing.set(false);
      }
    });
  }

  private processQueue() {
    const next = this.queue.shift();
    if (!next) { this.typing.set(false); return; }

    // keep typing on
    this.typing.set(true);
    this.chat.chat({ sessionId: this.sessionId(), message: next }).subscribe({
      next: res => {
        // append assistant answer (user bubble already appended when queued)
        this.messages.update(arr => arr.concat([
          { id: res.assistantMessageId, author: 'ASSISTANT', content: res.assistantReply, timestamp: res.timestamp }
        ]));
        this.processQueue();
      },
      error: () => { this.typing.set(false); }
    });
  }
}
