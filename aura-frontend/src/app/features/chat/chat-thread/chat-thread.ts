import { Component, Input, AfterViewInit, ElementRef, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Message } from '../../../core/models/message.model';
import { ChatBubble } from '../../../shared/components/chat-bubble/chat-bubble';
import { OrbIcon } from '../../../shared/components/orb-icon/orb-icon';

@Component({
  selector: 'app-chat-thread',
  standalone: true,
  imports: [CommonModule, ChatBubble, OrbIcon],
  templateUrl: './chat-thread.html',
  styleUrl: './chat-thread.scss'
})
export class ChatThread implements AfterViewInit, OnChanges {
  @Input() messages: Message[] = [];
  @Input() typing = false;

  constructor(private readonly host: ElementRef<HTMLElement>) {}

  ngAfterViewInit(): void { this.scrollToBottom(); }
  ngOnChanges(changes: SimpleChanges): void {
    if (changes['messages'] || changes['typing']) this.scrollToBottom();
  }

  private scrollToBottom() {
    queueMicrotask(() => {
      const el = this.host.nativeElement.querySelector('.thread');
      if (el) el.scrollTop = el.scrollHeight;
    });
  }
}
