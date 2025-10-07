import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { OrbIcon } from '../../../shared/components/orb-icon/orb-icon';
import { UserAvatar } from '../../../shared/components/user-avatar/user-avatar';
import { ProfileModal } from '../../../shared/components/profile-modal/profile-modal';

@Component({
  selector: 'app-chat-header',
  standalone: true,
  imports: [CommonModule, OrbIcon, UserAvatar, ProfileModal],
  templateUrl: './chat-header.html',
  styleUrl: './chat-header.scss'
})
export class ChatHeader {
  readonly showProfile = signal(false);
}
