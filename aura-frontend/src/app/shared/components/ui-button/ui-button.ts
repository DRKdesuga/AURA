import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-ui-button',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ui-button.html',
  styleUrl: './ui-button.scss'
})
export class UiButton {
  @Input() disabled = false;
  @Input() variant: 'primary' | 'ghost' = 'primary';
}
