import {
  Component, Input, Output, EventEmitter,
  signal, computed, ViewChild, ElementRef, AfterViewInit, HostListener
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-chat-input',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './chat-input.html',
  styleUrl: './chat-input.scss',
})
export class ChatInput implements AfterViewInit {
  @Input() disabled = false;
  @Output() send = new EventEmitter<string>();
  @Output() newChat = new EventEmitter<void>();

  @ViewChild('ta') ta!: ElementRef<HTMLTextAreaElement>;

  valueSig = signal<string>('');
  ready = computed(() => this.valueSig().trim().length > 0 && !this.disabled);

  ngAfterViewInit() { this.resize(); }

  @HostListener('window:resize')
  onResize() { this.resize(); }

  onType(v: string) {
    this.valueSig.set(v);
    this.resize();
  }

  onInput() {
    this.resize();
  }

  onKeyDown(e: KeyboardEvent) {
    if (e.key === 'Enter' && !e.shiftKey && !e.isComposing) {
      e.preventDefault();
      this.onSubmit();
    }
  }

  onEnterKey(e: KeyboardEvent) {
    if (!e.shiftKey && !e.isComposing) {
      e.preventDefault();
      this.onSubmit();
    }
  }

  onSubmit() {
    if (this.disabled) return;
    const v = this.valueSig().trim();
    if (!v) return;

    this.send.emit(v);
    this.valueSig.set('');

    const el = this.ta?.nativeElement;
    if (!el) return;

    el.value = '';
    el.classList.remove('capped');
    el.style.height = 'auto';

    requestAnimationFrame(() => {
      requestAnimationFrame(() => this.resize());
    });
  }


  private resize() {
    const el = this.ta?.nativeElement;
    if (!el) return;

    el.style.height = 'auto';

    const cap = Math.min(360, Math.max(200, Math.floor(window.innerHeight * 0.4)));
    const h = Math.min(cap, el.scrollHeight);

    el.style.height = `${h}px`;

    const capped = el.scrollHeight > cap;
    el.classList.toggle('capped', capped);
    if (capped) el.scrollTop = el.scrollHeight;
  }
}
