import { Component, Input, HostBinding } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Message } from '../../../core/models/message.model';
import { OrbIcon } from '../orb-icon/orb-icon';
import { MarkdownViewer } from '../markdown-viewer/markdown-viewer';

@Component({
  selector: 'app-chat-bubble',
  standalone: true,
  imports: [CommonModule, OrbIcon, MarkdownViewer],
  templateUrl: './chat-bubble.html',
  styleUrl: './chat-bubble.scss'
})
export class ChatBubble {
  @Input({ required: true }) message!: Message;

  @HostBinding('class.user')
  get isUser() { return this.message?.author === 'USER'; }

  @HostBinding('class.assistant')
  get isAssistant() { return this.message?.author === 'ASSISTANT'; }
}
