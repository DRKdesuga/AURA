import {
  Component, Input, Output, EventEmitter,
  signal, computed, ViewChild, ElementRef, AfterViewInit, HostListener
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

export interface ChatInputPayload {
  message: string;
  file: File | null;
}

interface AttachmentPreview {
  file: File;
  name: string;
  sizeLabel: string;
}

@Component({
  selector: 'app-chat-input',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './chat-input.html',
  styleUrl: './chat-input.scss',
})
export class ChatInput implements AfterViewInit {
  private readonly disabledSig = signal(false);
  private readonly sendingSig = signal(false);

  @Input()
  set disabled(value: boolean) { this.disabledSig.set(!!value); }
  get disabled() { return this.disabledSig(); }

  @Input()
  set sending(value: boolean) { this.sendingSig.set(!!value); }
  get sending() { return this.sendingSig(); }

  @Output() send = new EventEmitter<ChatInputPayload>();
  @Output() newChat = new EventEmitter<void>();

  @ViewChild('ta') ta!: ElementRef<HTMLTextAreaElement>;
  @ViewChild('fileInput') fileInput?: ElementRef<HTMLInputElement>;

  valueSig = signal<string>('');
  attachments = signal<File[]>([]);
  error = signal<string | null>(null);

  ready = computed(() => (
    this.valueSig().trim().length > 0 && !this.disabledSig() && !this.sendingSig()
  ));

  attachmentPreviews = computed<AttachmentPreview[]>(() => (
    this.attachments().map(file => ({
      file,
      name: file.name,
      sizeLabel: this.formatBytes(file.size)
    }))
  ));

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
    if (this.disabledSig() || this.sendingSig()) return;
    const v = this.valueSig().trim();
    if (!v) return;

    const file = this.attachments().length ? this.attachments()[this.attachments().length - 1] : null;
    this.send.emit({ message: v, file });

    if (!file) this.clearInput();
  }

  openFilePicker() {
    if (this.sendingSig() || this.disabledSig()) return;
    this.fileInput?.nativeElement.click();
  }

  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement | null;
    if (this.sendingSig() || this.disabledSig()) {
      if (input) input.value = '';
      return;
    }
    const files = input?.files ? Array.from(input.files) : [];
    if (files.length) this.applyFiles(files);
    if (input) input.value = '';
  }

  handleDroppedFiles(files: FileList | File[]) {
    if (this.sendingSig() || this.disabledSig()) return;
    const list = Array.isArray(files) ? files : Array.from(files);
    if (list.length) this.applyFiles(list);
  }

  removeAttachment(index: number) {
    const current = this.attachments();
    if (!current.length) return;
    this.attachments.set(current.filter((_, i) => i !== index));
    if (this.attachments().length === 0) this.error.set(null);
  }

  clearAttachments() {
    this.attachments.set([]);
    this.error.set(null);
  }

  clearDraft() {
    this.clearInput();
    this.clearAttachments();
  }

  setError(message: string) {
    this.error.set(message);
  }

  private applyFiles(files: File[]) {
    const validFiles = files.filter(file => this.isPdf(file));
    const invalidFiles = files.filter(file => !this.isPdf(file));
    const error = this.pickErrorMessage(validFiles, invalidFiles);
    if (error) this.error.set(error);
    if (!validFiles.length) return;

    const file = validFiles[validFiles.length - 1];
    this.attachments.set([file]);
    if (!error) this.error.set(null);
  }

  private pickErrorMessage(validFiles: File[], invalidFiles: File[]): string | null {
    if (invalidFiles.length) return 'Only PDF files are supported right now.';
    if (validFiles.length > 1) return 'Only one file can be attached at a time.';
    return null;
  }

  private isPdf(file: File): boolean {
    const name = file.name.toLowerCase();
    return file.type === 'application/pdf' || name.endsWith('.pdf');
  }

  private formatBytes(bytes: number): string {
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

  private clearInput() {
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
