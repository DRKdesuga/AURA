import { Component, HostBinding, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-orb-icon',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './orb-icon.html',
  styleUrl: './orb-icon.scss'
})
export class OrbIcon {
  @Input() size = 36;
  @Input() animate = true;

  @HostBinding('class.animate')
  get isAnimated() { return this.animate; }
}
