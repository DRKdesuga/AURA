import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { OrbIcon } from '../../../shared/components/orb-icon/orb-icon';

@Component({
  selector: 'app-chat-header',
  standalone: true,
  imports: [CommonModule, OrbIcon],
  templateUrl: './chat-header.html',
  styleUrl: './chat-header.scss'
})
export class ChatHeader {}
