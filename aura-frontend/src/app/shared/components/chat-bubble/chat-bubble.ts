import { Component, Input } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { Message } from '../../../core/models/message.model';
import { OrbIcon } from '../orb-icon/orb-icon';

@Component({
  selector: 'app-chat-bubble',
  standalone: true,
  imports: [CommonModule, DatePipe, OrbIcon],
  templateUrl: './chat-bubble.html',
  styleUrl: './chat-bubble.scss'
})
export class ChatBubble {
  @Input() message!: Message;
}
