import { Component, Input, HostBinding, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Message } from '../../../core/models/message.model';
import { OrbIcon } from '../orb-icon/orb-icon';
import { MarkdownViewer } from '../markdown-viewer/markdown-viewer';
import { UserAvatar } from '../user-avatar/user-avatar';

@Component({
  selector: 'app-chat-bubble',
  standalone: true,
  imports: [CommonModule, OrbIcon, MarkdownViewer, UserAvatar],
  templateUrl: './chat-bubble.html',
  styleUrl: './chat-bubble.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ChatBubble {
  @Input({ required: true }) message!: Message;

  @HostBinding('class.user') get isUser() { return this.message?.author === 'USER'; }
  @HostBinding('class.assistant') get isAssistant() { return this.message?.author === 'ASSISTANT'; }

  formatBytes(bytes: number): string {
    if (!Number.isFinite(bytes)) return '0 B';
    const units = ['B', 'KB', 'MB', 'GB'];
    let size = bytes;
    let unitIndex = 0;
    while (size >= 1024 && unitIndex < units.length - 1) {
      size /= 1024;
      unitIndex += 1;
    }
    const precision = size >= 10 || unitIndex === 0 ? 0 : 1;
    return `${size.toFixed(precision)} ${units[unitIndex]}`;
  }
}
