import { Component, ViewChild, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SessionsSidebar } from '../../sidebar/sessions-sidebar/sessions-sidebar';
import { ChatHeader } from '../../chat/chat-header/chat-header';
import { ChatThread } from '../../chat/chat-thread/chat-thread';
import { ChatInput, ChatInputPayload } from '../../chat/chat-input/chat-input';
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
  @ViewChild(ChatInput) chatInput?: ChatInput;

  sidebarHidden = signal(false);
  sessionId = signal<number | null>(null);
  messages = signal<Message[]>([]);
  typing = signal(false);
  fileSending = signal(false);
  dragActive = signal(false);
  private queue: string[] = [];
  private dragDepth = 0;

  constructor(private readonly chat: ChatService) {}

  toggleSidebar() { this.sidebarHidden.update(v => !v); }

  onSelectSession(id: number) {
    this.sessionId.set(id);
    this.typing.set(false);
    this.queue = [];
    this.chatInput?.clearAttachments();
    this.chat.getMessages(id).subscribe({ next: list => this.messages.set(list) });
  }

  onNewDraft() {
    this.sessionId.set(null);
    this.messages.set([]);
    this.typing.set(false);
    this.queue = [];
    this.chatInput?.clearAttachments();
  }

  onSendMessage(payload: ChatInputPayload) {
    const { message, file } = payload;
    if (file && this.typing()) {
      this.chatInput?.setError('Please wait for the current response before sending a file.');
      return;
    }

    const ts = new Date().toISOString();
    const tempId = Date.now();
    const attachments = file ? [{ name: file.name, size: file.size, type: file.type }] : undefined;
    this.messages.update(arr => arr.concat([{
      id: tempId,
      author: 'USER',
      content: message,
      timestamp: ts,
      attachments
    }]));

    if (file) {
      this.sendWithFile(message, file, tempId);
      return;
    }

    if (this.typing()) {
      this.queue.push(message);
      return;
    }

    const wasNew = this.sessionId() == null;
    this.typing.set(true);

    this.chat.chat({ sessionId: this.sessionId(), message }).subscribe({
      next: res => {
        this.sessionId.set(res.sessionId);
        this.messages.update(arr => arr.concat([
          { id: res.assistantMessageId, author: 'ASSISTANT', content: res.assistantReply, timestamp: res.timestamp }
        ]));
        if (wasNew) this.sidebar?.refresh();
        this.processQueue();
      },
      error: () => {
        this.typing.set(false);
        this.chatInput?.setError('Failed to send message. Please try again.');
      }
    });
  }

  onDragEnter(event: DragEvent) {
    if (!this.isFileDrag(event)) return;
    event.preventDefault();
    this.dragDepth += 1;
    this.dragActive.set(true);
  }

  onDragOver(event: DragEvent) {
    if (!this.isFileDrag(event)) return;
    event.preventDefault();
    if (event.dataTransfer) event.dataTransfer.dropEffect = 'copy';
    this.dragActive.set(true);
  }

  onDragLeave(event: DragEvent) {
    if (!this.isFileDrag(event)) return;
    event.preventDefault();
    this.dragDepth = Math.max(0, this.dragDepth - 1);
    if (this.dragDepth === 0) this.dragActive.set(false);
  }

  onDrop(event: DragEvent) {
    if (!this.isFileDrag(event)) return;
    event.preventDefault();
    this.dragDepth = 0;
    this.dragActive.set(false);
    const files = event.dataTransfer?.files;
    if (files?.length) this.chatInput?.handleDroppedFiles(files);
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
      error: () => {
        this.typing.set(false);
        this.chatInput?.setError('Failed to send message. Please try again.');
      }
    });
  }

  private sendWithFile(message: string, file: File, tempId: number) {
    const wasNew = this.sessionId() == null;
    this.fileSending.set(true);
    this.typing.set(true);

    this.chat.chatWithFile({ sessionId: this.sessionId(), message, file }).subscribe({
      next: res => {
        this.sessionId.set(res.sessionId);
        this.messages.update(arr => {
          const updated = arr.map(item => (
            item.id === tempId
              ? { ...item, id: res.userMessageId }
              : item
          ));
          return updated.concat([
            { id: res.assistantMessageId, author: 'ASSISTANT', content: res.assistantReply, timestamp: res.timestamp }
          ]);
        });
        if (wasNew) this.sidebar?.refresh();
        this.fileSending.set(false);
        this.typing.set(false);
        this.chatInput?.clearDraft();
      },
      error: () => {
        this.fileSending.set(false);
        this.typing.set(false);
        this.chatInput?.setError('Failed to send message. Please try again.');
      }
    });
  }

  private isFileDrag(event: DragEvent): boolean {
    if (this.dragDepth > 0) return true;
    const dataTransfer = event.dataTransfer;
    if (!dataTransfer) return false;
    if (dataTransfer.types && Array.from(dataTransfer.types).includes('Files')) return true;
    return dataTransfer.files?.length > 0;
  }
}
