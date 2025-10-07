import { Component, EventEmitter, Input, Output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { UserPreferencesService } from '../../../core/services/user/user-preferences';

@Component({
  selector: 'app-profile-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './profile-modal.html',
  styleUrl: './profile-modal.scss'
})
export class ProfileModal {
  @Input() open = false;
  @Output() close = new EventEmitter<void>();

  readonly presets = [
    'assets/user/default-avatar.png'
  ];

  color = signal<string>('#6f9dff');
  selected = signal<string>('');

  constructor(public readonly prefs: UserPreferencesService) {
    this.color.set(this.prefs.auraColor());
    this.selected.set(this.prefs.avatarUrl());
  }

  onSave(): void {
    if (this.selected()) this.prefs.setAvatar(this.selected());
    if (this.color()) this.prefs.setAuraColor(this.color());
    this.close.emit();
  }

  onFile(ev: Event) {
    const input = ev.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = () => this.selected.set(String(reader.result)); // data: URL
    reader.readAsDataURL(file);
  }

  protected readonly document = document;
}
