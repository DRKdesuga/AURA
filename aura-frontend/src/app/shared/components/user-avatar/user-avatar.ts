import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { UserPreferencesService } from '../../../core/services/user/user-preferences';

@Component({
  selector: 'app-user-avatar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './user-avatar.html',
  styleUrl: './user-avatar.scss'
})
export class UserAvatar {
  @Input() size = 36;
  @Input() clickable = true;
  @Output() open = new EventEmitter<void>();
  constructor(public readonly prefs: UserPreferencesService) {}
}
