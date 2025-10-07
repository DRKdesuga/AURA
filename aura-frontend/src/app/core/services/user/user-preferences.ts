import { Injectable, signal } from '@angular/core';


@Injectable({ providedIn: 'root' })
export class UserPreferencesService {
  private readonly AVATAR_KEY = 'aura.avatar';
  private readonly AURA_KEY = 'aura.auraColor';

  readonly avatarUrl = signal<string>(localStorage.getItem(this.AVATAR_KEY) || 'assets/user/default-avatar.png');
  readonly auraColor = signal<string>(localStorage.getItem(this.AURA_KEY) || '#6f9dff');

  setAvatar(url: string): void {
    this.avatarUrl.set(url);
    localStorage.setItem(this.AVATAR_KEY, url);
  }

  setAuraColor(hex: string): void {
    this.auraColor.set(hex);
    localStorage.setItem(this.AURA_KEY, hex);
  }
}
