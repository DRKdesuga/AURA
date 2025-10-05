import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { SessionSummary } from '../../../core/models/session-summary.model';

@Component({
  selector: 'app-session-card',
  standalone: true,
  imports: [CommonModule, DatePipe],
  templateUrl: './session-card.html',
  styleUrl: './session-card.scss'
})
export class SessionCard {
  @Input() data!: SessionSummary;
  @Output() select = new EventEmitter<void>();
}
