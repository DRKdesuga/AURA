import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-text-input',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './text-input.html',
  styleUrl: './text-input.scss'
})
export class TextInput {
  @Input() placeholder = '';
  @Input() value = '';
  @Input() type = 'text';
  @Output() valueChange = new EventEmitter<string>();
  @Output() enter = new EventEmitter<void>();

  onInput(v: string) { this.value = v; this.valueChange.emit(v); }
  onEnter() { this.enter.emit(); }
}
