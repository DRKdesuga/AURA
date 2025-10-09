import { Component, ViewChild, signal } from '@angular/core';
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
  @ViewChild(SessionsSidebar) sidebar?: SessionsSidebar;

  sidebarHidden = signal(false);
  sessionId = signal<number | null>(null);
  messages = signal<Message[]>([]);
  typing = signal(false);
  private queue: string[] = [];

  constructor(private readonly chat: ChatService) {}

  toggleSidebar() { this.sidebarHidden.update(v => !v); }

  onSelectSession(id: number) {
    this.sessionId.set(id);
    this.typing.set(false);
    this.queue = [];
    this.chat.getMessages(id).subscribe({ next: list => this.messages.set(list) });
  }

  onNewDraft() {
    this.sessionId.set(null);
    this.messages.set([]);
    this.typing.set(false);
    this.queue = [];
  }

  onSendMessage(text: string) {
    const ts = new Date().toISOString();
    const tempId = Date.now();
    this.messages.update(arr => arr.concat([{ id: tempId, author: 'USER', content: text, timestamp: ts }]));

    if (this.typing()) {
      this.queue.push(text);
      return;
    }

    const wasNew = this.sessionId() == null;
    this.typing.set(true);

    this.chat.chat({ sessionId: this.sessionId(), message: text }).subscribe({
      next: res => {
        this.sessionId.set(res.sessionId);
        this.messages.update(arr => arr.concat([
          { id: res.assistantMessageId, author: 'ASSISTANT', content: res.assistantReply, timestamp: res.timestamp }
        ]));
        if (wasNew) this.sidebar?.refresh();
        this.processQueue();
      },
      error: () => { this.typing.set(false); }
    });
  }

  private processQueue() {
    const next = this.queue.shift();
    if (!next) { this.typing.set(false); return; }
    this.typing.set(true);
    this.chat.chat({ sessionId: this.sessionId(), message: next }).subscribe({
      next: res => {
        this.messages.update(arr => arr.concat([
          { id: res.assistantMessageId, author: 'ASSISTANT', content: res.assistantReply, timestamp: res.timestamp }
        ]));
        this.processQueue();
      },
      error: () => { this.typing.set(false); }
    });
  }
}
