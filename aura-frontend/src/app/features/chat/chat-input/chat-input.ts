import { Component, EventEmitter, Input, Output, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-chat-input',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './chat-input.html',
  styleUrl: './chat-input.scss'
})
export class ChatInput {
  @Input() disabled = false; // on ne l'utilise plus pour bloquer pendant la génération
  @Output() send = new EventEmitter<string>();

  valueSig = signal('');
  ready = computed(() => this.valueSig().trim().length > 0 && !this.disabled);

  onSubmit() {
    const v = this.valueSig().trim();
    if (!v || this.disabled) return;
    this.send.emit(v);
    this.valueSig.set('');
  }
}
